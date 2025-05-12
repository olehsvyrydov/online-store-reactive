FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} ./payment-service.jar
RUN chmod +x ./payment-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "./payment-service.jar"]
