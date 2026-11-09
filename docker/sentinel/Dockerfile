FROM eclipse-temurin:17-jre
WORKDIR /app
ARG SENTINEL_DASHBOARD_SOURCE=https://github.com/alibaba/Sentinel/releases/download/1.8.8/sentinel-dashboard-1.8.8.jar
ADD ${SENTINEL_DASHBOARD_SOURCE} /app/sentinel-dashboard.jar
RUN echo '7f3e39534ceccb5b38e3787c3b177f75443e31f57e28f7ab3525640c9925b458  /app/sentinel-dashboard.jar' | sha256sum -c -
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=8080", "-Dproject.name=sentinel-dashboard", "-jar", "/app/sentinel-dashboard.jar"]
