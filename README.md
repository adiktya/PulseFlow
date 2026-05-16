# PulseFlow

PulseFlow is a standalone sample for distributed notification and workflow orchestration. This folder is not wired into the Insurance-Codes Gradle root.

## What runs where

| Surface | URL | Notes |
| --- | --- | --- |
| Admin UI | `http://localhost:3000` | nginx static build proxies `/api/*` to the gateway |
| API gateway | `http://localhost:8080` | Spring Cloud Gateway forwards `/api/v1/**` to the workflow service |
| Workflow API (direct) | `http://localhost:8082` | Spring Boot app (useful for debugging without the gateway) |
| Kafka (host) | `localhost:9092` | PLAINTEXT_HOST listener |
| Kafka UI | `http://localhost:8090` | Topic inspection |
| Postgres (host) | `localhost:5433` | default user/db/password: `pulseflow` |
| FlashCache | `localhost:7654` | TCP JSON protocol; Prometheus text on `http://localhost:7655/metrics` |

## Prerequisites

- Docker and Docker Compose (for the full stack)
- JDK 17+ and Gradle 8.10.x (for running Spring apps on the host)

If `./gradlew` cannot download Gradle due to TLS interception, use a locally installed Gradle 8.10.2:

```bash
export PF_GRADLE="$HOME/.gradle/wrapper/dists/gradle-8.10.2-bin/"*/gradle-8.10.2/bin/gradle
```

## Full stack in Docker

From the **Insurance-Codes** repository root (the workflow image build uses `context: ..` so both `pulseflow/backend` and `flashcache` are visible to Gradle):

```bash
docker compose -f pulseflow/docker-compose.yml up -d --build
```

This starts Zookeeper, Kafka, **FlashCache**, Postgres, Kafka UI, the **workflow** Spring Boot container (`SPRING_PROFILES_ACTIVE=docker`), the **gateway**, and the **admin UI** nginx container.

- Dashboard: `http://localhost:3000`
- Gateway health: `GET http://localhost:8080/actuator/health`
- Workflow health (direct): `GET http://localhost:8082/actuator/health`

Stop:

```bash
docker compose -f pulseflow/docker-compose.yml down
```

Reset volumes:

```bash
docker compose -f pulseflow/docker-compose.yml down -v
```

## Host-only development (no Docker for Spring)

Bring up dependencies only:

```bash
docker compose -f pulseflow/docker-compose.yml up -d zookeeper kafka kafka-setup flashcache postgres kafka-ui
```

Terminal A (gateway on 8080):

```bash
cd pulseflow/gateway
./gradlew bootRun
```

Terminal B (workflow on 8082):

```bash
cd pulseflow/backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

Terminal C (UI dev server on 5173):

```bash
cd pulseflow/admin-ui
npm install
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8080` (the gateway).

## Example API flow (through gateway)

Create a campaign:

```bash
curl -s -X POST http://localhost:8080/api/v1/campaigns \
  -H 'Content-Type: application/json' \
  -d '{"name":"KYC reminders May"}' | jq .
```

List campaigns:

```bash
curl -s http://localhost:8080/api/v1/campaigns | jq .
```

Enqueue a job:

```bash
curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H 'Content-Type: application/json' \
  -d '{"workflowId":"kyc-reminder","payload":"{\"userId\":\"u1\"}"}' | jq .
```

List jobs:

```bash
curl -s http://localhost:8080/api/v1/jobs | jq .
```

Replay a failed job:

```bash
curl -s -X POST http://localhost:8080/api/v1/jobs/<jobId>/replay | jq .
```

To force failures for retry and DLQ demos, set a `simulate` field inside the JSON payload string:

- `"simulate":"fail-once"` fails the first attempt, then succeeds (exercises `notification.retry`).
- `"simulate":"fail-always"` fails every attempt (exercises `notification.dlq` once `retry_count` reaches `pulseflow.max-retries`).

Replaying clears **FlashCache** idempotency markers for that job before republishing to `notification.send`.

## Gateway configuration

- `PULSEFLOW_WORKFLOW_BASE_URL` (Spring relaxed binding for `pulseflow.workflow.base-url`) controls the upstream workflow URL. Docker Compose sets this to `http://workflow:8082`.
- `pulseflow.gateway.security.enabled=true` enables a minimal stub filter that requires an `X-Api-Key` header (placeholder for real auth).

### Pointing the gateway at a workflow running on your host

If the workflow JVM runs on the host (`./gradlew bootRun` in `pulseflow/backend`) while the gateway runs in Docker, set the upstream URL to your host loopback from inside the gateway container:

```yaml
# Example override for a gateway service in compose
environment:
  PULSEFLOW_WORKFLOW_BASE_URL: http://host.docker.internal:8082
extra_hosts:
  - "host.docker.internal:host-gateway"
```

On macOS/Windows, `host.docker.internal` is often available without `extra_hosts`. On Linux, `host-gateway` is the portable way to wire the same hostname.

## Configuration

See `pulseflow/.env.example` for Docker port overrides.

Workflow connects to FlashCache using `pulseflow.flashcache.host` / `pulseflow.flashcache.port` (defaults `localhost` / `7654`; Docker profile uses host `flashcache`).

## Documentation

- [Product requirements](docs/PRD.md)
- [Local architecture notes](docs/LOCAL_ARCHITECTURE.md)
- [UI + gateway plan (implemented)](docs/PLAN_UI_AND_GATEWAY.md)
- [FlashCache (coordination SDK + server)](../flashcache/README.md)
