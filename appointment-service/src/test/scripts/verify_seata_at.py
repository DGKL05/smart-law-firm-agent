"""Verify Seata AT using disposable schemas and temporary application ports.

Start the project's MySQL, Nacos and Seata containers and publish the Nacos
configuration before building both service jars. The verification provides
its own token introspection and lawyer snapshot fixture on localhost.
"""
import argparse
import datetime as dt
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import tempfile
from threading import Thread
import time
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs
from urllib.request import Request, urlopen
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


USER_ID = 9101
LAWYER_ID = 9102
ROOT = Path(__file__).resolve().parents[4]


def mysql(container, sql):
    # Read the password inside the container rather than printing it or passing it in host arguments.
    command = 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --batch --skip-column-names --default-character-set=utf8mb4'
    result = subprocess.run(["docker", "exec", "-i", container, "sh", "-c", command],
                            input=sql.encode("utf-8"), capture_output=True, timeout=35, check=False)
    if result.returncode:
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace"))
    return result.stdout.decode("utf-8").strip()


class UpstreamFixture(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path != f"/internal/lawyers/{LAWYER_ID}/snapshot":
            self.send_error(404)
            return
        snapshot = {"id": LAWYER_ID, "lawFirmId": 1, "name": "Seata Test",
                    "lawFirmName": "AT Test Firm", "status": 1}
        self.respond({"code": 200, "message": "success", "data": snapshot})

    def do_POST(self):
        if self.path != "/internal/oauth2/introspect":
            self.send_error(404)
            return
        fields = parse_qs(self.rfile.read(int(self.headers.get("Content-Length", "0"))).decode())
        if fields.get("token") != [self.server.fixture_token]:
            self.respond({"active": False})
            return
        self.respond({"active": True, "user_id": USER_ID, "username": "seata-at-test", "roles": ["USER"]})

    def respond(self, value):
        payload = json.dumps(value).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, *_):
        pass


def api(port, path, method="GET", body=None, token=None, internal=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if internal:
        headers["X-Internal-Token"] = internal
    request = Request(f"http://127.0.0.1:{port}{path}", headers=headers, method=method,
                      data=json.dumps(body).encode() if body is not None else None)
    try:
        with urlopen(request, timeout=35) as response:
            return response.status, json.load(response)
    except HTTPError as error:
        try:
            return error.code, json.load(error)
        except ValueError:
            return error.code, {}


def wait_until(condition, description, timeout=35):
    deadline = time.monotonic() + timeout
    last_error = None
    while time.monotonic() < deadline:
        try:
            if condition():
                return
        except (OSError, URLError, ValueError) as error:
            last_error = error
        time.sleep(0.4)
    raise RuntimeError(f"{description} did not complete; last connection error: {last_error}")


def ensure_free(port):
    with socket.socket() as candidate:
        candidate.bind(("127.0.0.1", port))


def service_ready(process, port, path, token=None, internal=None):
    if process.poll() is not None:
        raise RuntimeError(f"The isolated service on port {port} exited. See the verification logs.")
    return api(port, path, token=token, internal=internal)[0] == 200


def start_service(name, port, schema, args, env, log_dir, options=()):
    jar = ROOT / f"{name}-service" / "target" / f"{name}-service-0.0.1-SNAPSHOT.jar"
    if not jar.is_file():
        raise RuntimeError(f"Build {jar} first with mvn -pl appointment-service,schedule-service -am package")
    url = (f"jdbc:mysql://127.0.0.1:{args.mysql_port}/{schema}"
           "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai")
    tracing = (["--management.tracing.sampling.probability=1.0",
                f"--management.zipkin.tracing.endpoint={args.zipkin_endpoint}"] if args.zipkin_endpoint
               else ["--management.tracing.enabled=false"])
    command = ["java", "-Xmx384m", "-XX:ActiveProcessorCount=2", "-Dfile.encoding=UTF-8", "-jar", str(jar),
               f"--server.port={port}", "--server.address=127.0.0.1",
               f"--spring.datasource.url={url}", "--spring.cloud.nacos.discovery.register-enabled=false",
               "--spring.cloud.service-registry.auto-registration.enabled=false",
               "--spring.boot.admin.client.enabled=false", "--appointment.notification-delay-ms=600000",
               *tracing, *options]
    with (log_dir / f"{name}.log").open("wb") as log:
        return subprocess.Popen(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT,
                                creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def verify(args, appointment_schema, schedule_schema, token):
    def query(sql):
        return mysql(args.mysql_container, sql)

    def check_rows(appointment_id, appointment_status, reservation_status):
        appointment = query(f"SELECT status FROM {appointment_schema}.appointment WHERE id={appointment_id};")
        reservation = query(f"SELECT status FROM {schedule_schema}.schedule_reservation "
                            f"WHERE appointment_id={appointment_id};")
        return (appointment, reservation) == (appointment_status, reservation_status)

    def book(slot, expected_status=200):
        status, body = api(args.appointment_port, "/api/me/appointments/book", "POST",
                           {"lawyerId": LAWYER_ID, "appointmentTime": slot.isoformat(), "remark": "Seata AT test"},
                           token=token)
        check_status(status, body, expected_status)
        if expected_status == 200:
            return int(body["data"]["id"])
        return None

    def cancel(appointment_id, expected_status=200):
        status, body = api(args.appointment_port, f"/api/me/appointments/{appointment_id}/cancel", "PUT", token=token)
        check_status(status, body, expected_status)

    def add_fault(event_key):
        query(f"INSERT INTO {appointment_schema}.appointment_notification_outbox "
              "(event_key,user_id,title,content,next_attempt_time,sent_time) "
              f"VALUES ('{event_key}',{USER_ID},'test duplicate','test duplicate',NOW(),NOW());")

    def remove_fault(event_key):
        query(f"DELETE FROM {appointment_schema}.appointment_notification_outbox WHERE event_key='{event_key}';")

    try:
        now = dt.datetime.now(ZoneInfo("Asia/Shanghai"))
    except ZoneInfoNotFoundError:
        now = dt.datetime.now(dt.timezone(dt.timedelta(hours=8)))
    day = now.date() + dt.timedelta(days=1)
    while day.weekday() >= 5:
        day += dt.timedelta(days=1)
    first_slot = dt.datetime.combine(day, dt.time(hour=9))
    fault_slot = dt.datetime.combine(day, dt.time(hour=10))

    committed = book(first_slot)
    wait_until(lambda: check_rows(committed, "已预约", "RESERVED"), "booking commit")
    print(f"COMMIT booking {committed}: appointment=已预约, schedule=RESERVED", flush=True)

    next_id = int(query("SET SESSION information_schema_stats_expiry=0; "
                        "SELECT AUTO_INCREMENT FROM information_schema.tables "
                        f"WHERE table_schema='{appointment_schema}' AND table_name='appointment';"))
    booking_fault = f"appointment:{next_id}:booked"
    add_fault(booking_fault)
    try:
        book(fault_slot, 409)
        wait_until(lambda: check_rows(next_id, "", ""), "two-database booking rollback")
    finally:
        remove_fault(booking_fault)
    print(f"ROLLBACK booking {next_id}: neither database retained the failed booking", flush=True)
    retried = book(fault_slot)
    require(retried > next_id, "The failed booking never reached the appointment insert")
    wait_until(lambda: check_rows(retried, "已预约", "RESERVED"), "same-slot booking after rollback")
    print(f"RETRY booking {retried}: the rolled-back slot is still available", flush=True)

    cancellation_fault = f"appointment:{committed}:cancelled"
    add_fault(cancellation_fault)
    try:
        cancel(committed, 409)
        wait_until(lambda: check_rows(committed, "已预约", "RESERVED"), "two-database cancellation rollback")
    finally:
        remove_fault(cancellation_fault)
    print(f"ROLLBACK cancellation {committed}: the appointment and occupancy were both restored", flush=True)
    cancel(committed)
    wait_until(lambda: check_rows(committed, "已取消", "RELEASED"), "cancellation commit")
    renewed = book(first_slot)
    wait_until(lambda: check_rows(renewed, "已预约", "RESERVED"), "same-slot booking after cancellation")
    print(f"COMMIT cancellation {committed} and rebook {renewed}: the released slot can be booked again", flush=True)


def check_status(status, body, expected):
    require(status == expected and body.get("code") == expected,
            f"Expected HTTP/API {expected} but got {status}: {body}")
    if expected == 409:
        require(body.get("message") == "记录冲突或不符合数据约束",
                f"Booking did not reach the injected outbox duplicate: {body}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mysql-container", default="smart-law-mysql")
    parser.add_argument("--mysql-port", type=int, default=3307)
    parser.add_argument("--appointment-port", type=int, default=18083)
    parser.add_argument("--schedule-port", type=int, default=18086)
    parser.add_argument("--zipkin-endpoint")
    args = parser.parse_args()
    if args.appointment_port == args.schedule_port:
        parser.error("Choose different ports for the two temporary services")
    ensure_free(args.appointment_port)
    ensure_free(args.schedule_port)

    suffix = secrets.token_hex(5)
    schemas = {name: f"smart_law_at_verify_{name}_{suffix}" for name in ("appointment", "schedule")}
    log_dir = Path(tempfile.mkdtemp(prefix="smart-law-at-verify-"))
    env = os.environ.copy()
    fixture = ThreadingHTTPServer(("127.0.0.1", 0), UpstreamFixture)
    fixture.fixture_token = secrets.token_hex(24)
    env.update(INTERNAL_SERVICE_TOKEN=secrets.token_hex(16), OAUTH2_INTROSPECTION_CLIENT_SECRET=secrets.token_hex(24),
               OAUTH2_INTROSPECTION_URI=f"http://127.0.0.1:{fixture.server_port}/internal/oauth2/introspect")
    Thread(target=fixture.serve_forever, daemon=True).start()
    processes = []
    created = []
    keep_schemas = False
    print(f"Verification logs: {log_dir}", flush=True)

    try:
        for name, schema in schemas.items():
            mysql(args.mysql_container, f"CREATE DATABASE {schema} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
            created.append(schema)
            ddl = (ROOT / "sql" / "microservices" / name / "001_schema.sql").read_text(encoding="utf-8")
            mysql(args.mysql_container, ddl.replace(f"USE smart_law_{name};", f"USE {schema};", 1))

        lawyer_url = f"http://127.0.0.1:{fixture.server_port}"
        common = (f"--spring.cloud.openfeign.client.config.lawyer-service.url={lawyer_url}",)
        processes.append(start_service("schedule", args.schedule_port, schemas["schedule"], args, env, log_dir, common))
        appointment_options = (*common,
                               f"--spring.cloud.openfeign.client.config.schedule-service.url=http://127.0.0.1:"
                               f"{args.schedule_port}")
        processes.append(start_service("appointment", args.appointment_port, schemas["appointment"],
                                       args, env, log_dir, appointment_options))
        path = f"/internal/schedule/lawyers/{LAWYER_ID}"
        wait_until(lambda: service_ready(processes[0], args.schedule_port, path,
                                         internal=env["INTERNAL_SERVICE_TOKEN"]),
                   "isolated schedule service startup", 90)
        token = fixture.fixture_token
        wait_until(lambda: service_ready(processes[1], args.appointment_port, "/api/me/appointments", token=token),
                   "isolated appointment service startup", 90)
        verify(args, schemas["appointment"], schemas["schedule"], token)
    finally:
        try:
            if processes:
                wait_until(lambda: all(mysql(args.mysql_container, f"SELECT COUNT(*) FROM {schema}.undo_log;") == "0"
                                       for schema in created), "AT undo log cleanup", 30)
        except (RuntimeError, subprocess.TimeoutExpired):
            keep_schemas = True
            print(f"Keeping isolated schemas for unresolved transactions: {', '.join(created)}", flush=True)
            raise
        finally:
            stop_fixture(fixture, processes)
            if not keep_schemas:
                for schema in created:
                    mysql(args.mysql_container, f"DROP DATABASE {schema};")


def stop_fixture(fixture, processes):
    fixture.shutdown()
    fixture.server_close()
    for process in reversed(processes):
        if process.poll() is not None:
            continue
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)


if __name__ == "__main__":
    main()
