FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN mkdir -p /app/uploads /app/temp /app/backups
COPY --from=builder /build/target/file-sharing-system-1.0.0.jar /app/app.jar

EXPOSE 8080

ENV HTTPS_ENABLED=false \
    HTTPS_PORT=8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
