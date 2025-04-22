FROM openjdk:21-jdk-slim  # 경량화된 이미지 사용
WORKDIR /app
COPY podoemarket-0.0.1-SNAPSHOT.jar app.jar  # 명시적 파일명 지정
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
