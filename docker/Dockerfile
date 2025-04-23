FROM openjdk:21-jdk-slim
WORKDIR /app
COPY podoemarket-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
