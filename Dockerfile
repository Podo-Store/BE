# syntax=docker/dockerfile:1

########################################
# 1) Build stage
########################################
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 레이어 캐싱: 빌드 스크립트 / 래퍼 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스 전체 복사 후 빌드 (테스트 제외)
COPY src ./src
RUN ./gradlew --no-daemon clean build -x test

# 실행 가능한 boot jar만 추출 (*-plain.jar 제외)
RUN cp build/libs/*-SNAPSHOT.jar app.jar

########################################
# 2) Runtime stage
########################################
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# 비루트 유저로 실행
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/app.jar app.jar

ENV JAVA_OPTS=""

# 컨테이너 기본 포트(문서화용). Cloud Run 은 런타임에 $PORT 를 주입한다.
EXPOSE 8080

# Cloud Run 이 주는 $PORT 를 server.port 로 사용하고, 없으면 8080 사용
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=docker -Dserver.port=${PORT:-8080} -jar app.jar"]
