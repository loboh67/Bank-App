FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /workspace

# Pre-copy pom to leverage Docker layer caching for dependencies
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw

# Copy source last to avoid invalidating dependency layers unnecessarily
COPY src src
COPY src/main/proto src/main/proto

RUN ./mvnw -B -DskipTests package


FROM eclipse-temurin:25-jdk AS runtime
WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=builder /workspace/target/*-SNAPSHOT.jar /app/app.jar

# Install curl for the container healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 8082 9090

HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -fsS http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
