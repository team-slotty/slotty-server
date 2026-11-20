# ---------- 1단계: 빌드 ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ---------- 2단계: 실행 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
