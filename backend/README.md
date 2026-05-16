# PulseFlow workflow service

Kotlin + Spring Boot workflow engine (REST, Kafka, FlashCache, PostgreSQL).

## Run (host, deps via Compose)

```bash
docker compose -f ../docker-compose.yml up -d zookeeper kafka kafka-setup flashcache postgres kafka-ui
./gradlew bootRun --args='--spring.profiles.active=local'
```

Service listens on **8082** by default. OpenAPI UI: `http://localhost:8082/swagger-ui.html`.

## Run (container)

Use the root `docker-compose.yml` `workflow` service (`SPRING_PROFILES_ACTIVE=docker`).
