FROM gradle:9.3.0-jdk21 AS builder

LABEL authors="Ariel Adonai Souza"

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle gradle

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy

LABEL authors="Ariel Adonai Souza"

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
 "-XX:+UseContainerSupport", \
 "-XX:MaxRAMPercentage=75.0", \
 "-jar", "/app/app.jar"]
