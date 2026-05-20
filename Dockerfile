# === Build stage ===
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# Gradle wrapper + 빌드 파일 먼저 복사 (레이어 캐싱)
COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./

RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스 복사 후 빌드
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# === Runtime stage ===
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
