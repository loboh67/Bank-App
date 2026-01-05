FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw \
    && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

COPY --from=builder /app/target/*.jar /app/app.jar

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 CMD curl -f http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
