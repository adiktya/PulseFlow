# Build from Insurance-Codes repository root (docker compose sets context: .. relative to pulseflow/).
FROM eclipse-temurin:17-jdk AS build
WORKDIR /repo
COPY pulseflow/backend /repo/pulseflow/backend
COPY flashcache /repo/flashcache
WORKDIR /repo/pulseflow/backend
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /repo/pulseflow/backend/build/libs/*.jar /app/app.jar
EXPOSE 8082
HEALTHCHECK --interval=10s --timeout=3s --retries=30 \
  CMD curl -fsS http://localhost:8082/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=docker"]
