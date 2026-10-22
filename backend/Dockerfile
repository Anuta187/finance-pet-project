# syntax=docker/dockerfile:1

# сборка
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
# сначала pom.xml, чтобы зависимости кэшировались
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
RUN mvn -B -q -DskipTests package

# запуск
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 finance && mkdir -p /data && chown finance /data
COPY --from=build /src/target/finance-backend.jar app.jar

ENV PORT=3000 \
    DB_PATH=/data/finance \
    TZ=Asia/Vladivostok \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
VOLUME ["/data"]
EXPOSE 3000
USER finance

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fsS http://localhost:${PORT}/api/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
