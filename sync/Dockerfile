ARG JDK_VERSION=21

FROM maven:3.9-eclipse-temurin-${JDK_VERSION} AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Use Maven cache and retry HTTP downloads to avoid transient failures
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.http.connectionTimeout=60000 \
    -Dmaven.wagon.http.readTimeout=120000 \
    package

FROM eclipse-temurin:${JDK_VERSION}
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/sync-0.0.1-SNAPSHOT.jar /app/app.jar

ENV JAVA_OPTS=""

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
