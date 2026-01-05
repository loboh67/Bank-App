FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_OPTS=""

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8081 9091

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 CMD ["sh", "-c", "curl -fsS http://localhost:${SERVER_PORT:-8081}/actuator/health || exit 1"]

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
