package com.example.dgkl.appointment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.dgkl.appointment.dto.AppointmentBookingRequest;
import com.example.dgkl.appointment.dto.LawyerSnapshotResponse;
import com.example.dgkl.appointment.entity.Appointment;
import com.example.dgkl.appointment.entity.NotificationOutbox;
import com.example.dgkl.appointment.mapper.AppointmentGuardMapper;
import com.example.dgkl.appointment.mapper.AppointmentMapper;
import com.example.dgkl.appointment.mapper.NotificationOutboxMapper;
import com.example.dgkl.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.spring.annotation.GlobalLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AppointmentStore {
    private static final DateTimeFormatter NOTICE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int RETRY_SECONDS = 10;

    private final AppointmentMapper appointmentMapper;
    private final AppointmentGuardMapper guardMapper;
    private final NotificationOutboxMapper outboxMapper;
    private final CaseAvailability cases;

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Appointment prepare(Long userId, LawyerSnapshotResponse lawyer, AppointmentBookingRequest request) {
        Long lawyerId = lawyer.getId();
        LocalDateTime start = request.getAppointmentTime();
        guardMapper.lock(lawyerId);
        List<Appointment> existing = appointmentMapper.selectList(Wrappers.<Appointment>lambdaQuery()
                .eq(Appointment::getLawyerId, lawyerId)
                .eq(Appointment::getAppointmentTime, start)
                .in(Appointment::getStatus, AppointmentStatus.ACTIVE));
        if (!existing.isEmpty()) {
            Appointment appointment = existing.get(0);
            if (userId.equals(appointment.getUserId()) && AppointmentStatus.BOOKED.equals(appointment.getStatus())) {
                return appointment;
            }
            throw new BusinessException(409, "该时段已有预约或正在处理");
        }
        if (!cases.overlapping(lawyerId, start, AppointmentTimes.slotEnd(start)).isEmpty()) {
            throw new BusinessException(409, "该时段被案件占用");
        }

        Appointment appointment = new Appointment();
        appointment.setUserId(userId);
        appointment.setLawyerId(lawyerId);
        appointment.setLawFirmId(lawyer.getLawFirmId());
        appointment.setLawyerName(lawyer.getName());
        appointment.setLawFirmName(lawyer.getLawFirmName());
        appointment.setAppointmentTime(start);
        appointment.setDurationMinutes(AppointmentTimes.SLOT_DURATION_MINUTES);
        appointment.setStatus(AppointmentStatus.BOOKING);
        appointment.setRemark(request.getRemark());
        // NULL excludes AT's phase-one state from the old background reconciler.
        appointment.setDeleted(0);
        appointmentMapper.insert(appointment);
        return appointment;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Appointment beginCancellation(Long id, Long lawyerId) {
        guardMapper.lock(lawyerId);
        Appointment appointment = forUpdate(id);
        if (appointment == null || !lawyerId.equals(appointment.getLawyerId())) {
            throw new BusinessException(404, "预约不存在");
        }
        if (AppointmentStatus.CANCELLED.equals(appointment.getStatus())) {
            return appointment;
        }
        if (!AppointmentStatus.BOOKED.equals(appointment.getStatus())) {
            throw new BusinessException(409, "当前状态不可取消");
        }
        appointmentMapper.update(null, Wrappers.<Appointment>lambdaUpdate()
                .eq(Appointment::getId, id)
                .set(Appointment::getStatus, AppointmentStatus.CANCELLING)
                .set(Appointment::getNextSyncTime, null));
        appointment.setStatus(AppointmentStatus.CANCELLING);
        return appointment;
    }

    public Appointment forUpdate(Long id) {
        return appointmentMapper.selectOne(Wrappers.<Appointment>lambdaQuery()
                .eq(Appointment::getId, id)
                .last("FOR UPDATE"));
    }

    @GlobalLock
    @Transactional(rollbackFor = Exception.class)
    public void resolveBooking(Appointment appointment, boolean reserved) {
        String status = reserved ? AppointmentStatus.BOOKED : AppointmentStatus.REJECTED;
        if (completeTransition(appointment.getId(), AppointmentStatus.BOOKING, status) && reserved) {
            enqueue(appointment, "booked", "预约成功", "已预约%s律师 %s 的咨询。");
        }
    }

    @GlobalLock
    @Transactional(rollbackFor = Exception.class)
    public void resolveCancellation(Appointment appointment) {
        if (completeTransition(appointment.getId(), AppointmentStatus.CANCELLING, AppointmentStatus.CANCELLED)) {
            enqueue(appointment, "cancelled", "预约取消", "已取消%s律师 %s 的咨询预约。");
        }
    }

    @GlobalLock
    @Transactional(rollbackFor = Exception.class)
    public void defer(Appointment appointment) {
        appointmentMapper.update(null, Wrappers.<Appointment>lambdaUpdate()
                .eq(Appointment::getId, appointment.getId())
                .eq(Appointment::getStatus, appointment.getStatus())
                .isNotNull(Appointment::getNextSyncTime)
                .set(Appointment::getNextSyncTime, AppointmentTimes.now().plusSeconds(RETRY_SECONDS)));
    }

    public List<Appointment> due() {
        return appointmentMapper.selectList(Wrappers.<Appointment>lambdaQuery()
                .in(Appointment::getStatus, AppointmentStatus.BOOKING, AppointmentStatus.CANCELLING)
                .le(Appointment::getNextSyncTime, AppointmentTimes.now())
                .orderByAsc(Appointment::getNextSyncTime)
                .last("LIMIT 20"));
    }

    private boolean completeTransition(Long id, String expectedStatus, String nextStatus) {
        boolean global = RootContext.inGlobalTransaction();
        boolean completed = appointmentMapper.update(null, Wrappers.<Appointment>lambdaUpdate()
                .eq(Appointment::getId, id)
                .eq(Appointment::getStatus, expectedStatus)
                .isNull(global, Appointment::getNextSyncTime)
                .isNotNull(!global, Appointment::getNextSyncTime)
                .set(Appointment::getStatus, nextStatus)
                .set(AppointmentStatus.REJECTED.equals(nextStatus), Appointment::getFailureReason, "时段不可预约")
                .set(Appointment::getNextSyncTime, null)) == 1;
        if (global && !completed) {
            throw new BusinessException(409, "预约状态已改变，请刷新后重试");
        }
        return completed;
    }

    private void enqueue(Appointment appointment, String event, String title, String content) {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setEventKey("appointment:" + appointment.getId() + ":" + event);
        outbox.setUserId(appointment.getUserId());
        outbox.setTitle(title);
        outbox.setContent(content.formatted(appointment.getLawyerName(),
                appointment.getAppointmentTime().format(NOTICE_TIME)));
        outbox.setNextAttemptTime(AppointmentTimes.now());
        outboxMapper.insert(outbox);
    }
}
