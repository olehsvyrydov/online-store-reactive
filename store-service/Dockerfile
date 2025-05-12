FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} ./online-shop.jar
RUN chmod +x ./online-shop.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "./online-shop.jar"]
