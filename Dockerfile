FROM eclipse-temurin:17-jre
WORKDIR /app
ARG MODULE_NAME
COPY ${MODULE_NAME}/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
