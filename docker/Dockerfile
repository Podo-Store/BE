# jdk 21 Image Start
FROM openjdk:21-jdk
# 인자 정리 - jar
ARG JAR_FILE=*.jar
# jar file copy
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "/app.jar"]