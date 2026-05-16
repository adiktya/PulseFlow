Status: **Implemented** in this repository (gateway + admin UI + compose wiring).

# Plan: Admin UI and API Gateway for PulseFlow

This extends the existing local stack ([README](../README.md), [LOCAL_ARCHITECTURE](LOCAL_ARCHITECTURE.md)) with the PRD components **Admin Dashboard (React + Tailwind)** and **API Gateway**.

## Goals

- Single browser entry point for operators (dashboard) that talks only to the gateway.
- Gateway handles routing, validation at the edge (basic request limits where practical), CORS, and a stable public API path prefix.
- Local `docker compose` can optionally run gateway + static UI; Spring workflow service may stay on the host for fast iteration or move behind gateway via env URL.

## Proposed layout

| Piece | Path | Stack |
| --- | --- | --- |
| Workflow service (existing) | `pulseflow/backend/` | Kotlin / Spring Boot (unchanged responsibility: domain + Kafka + DB + Redis) |
| API Gateway (new) | `pulseflow/gateway/` | Kotlin / Spring Boot + **Spring Cloud Gateway** (reactive), Java 17, same Gradle style as `backend/` |
| Admin UI (new) | `pulseflow/admin-ui/` | **Vite + React + TypeScript + TailwindCSS** |

## API Gateway

### Responsibilities

- Route `/api/v1/**` to the workflow service base URL (configurable, e.g. `http://localhost:8082` in dev).
- Optional route `/actuator/**` blocked or stripped in non-local profiles (local can allow for debugging).
- **CORS**: allow admin UI origin (`http://localhost:5173` dev, production origins via env).
- **Rate limiting**: optional first phase using Redis (same Redis as workflow) with a simple bucket per IP or global limiter filter; if scope is tight, document “placeholder filter” and ship rate limit in a follow-up.
- **Authentication**: PRD mentions auth at gateway; for local dev ship **disabled by default** with a property `pulseflow.gateway.security.enabled=false`, and a stub filter that can later validate JWT or API keys without blocking demos.

### Configuration

- `application.yml` / `application-local.yml`: `pulseflow.workflow.base-url`, CORS origins list, server port **8080** (gateway becomes the main entry).
- Move workflow service default port to **8082** in `backend` `application.yml` to avoid clashing with gateway on 8080 (update README and compose env).

### Docker

- Add service `pulseflow-gateway` building from `gateway/Dockerfile` (multi-stage: Gradle build + JRE 17 slim).
- Environment: `PULSEFLOW_WORKFLOW_BASE_URL=http://host.docker.internal:8082` on macOS/Windows, or `http://workflow:8082` if workflow is containerized later.

## Admin UI

### Pages (MVP)

- **Campaigns**: list (from new read API or reuse existing if added), create form (name, optional schedule), success toast.
- **Jobs**: create job (workflow id, payload JSON textarea, optional idempotency key), table of recent jobs with status/retry count, detail drawer, **Replay** for `FAILED` jobs.
- **Health**: link to gateway/workflow actuator health (optional fetch for status pill).

### Data access

- UI calls only `http://localhost:8080/api/v1/...` (gateway), never the workflow port directly.
- If list endpoints are missing, add minimal read APIs to workflow: `GET /api/v1/campaigns`, `GET /api/v1/jobs` (paginated or top N) to support the dashboard (small additive change in `backend`).

### Dev and build

- `npm` scripts: `dev` (Vite with proxy to gateway), `build`, `preview`.
- Tailwind configured per Vite + PostCSS defaults.

### Docker

- **Option A (recommended for compose)**: multi-stage Dockerfile builds static assets and **nginx** serves `/` and proxies `/api` to gateway (avoids CORS in all-docker mode).
- **Option B**: document `npm run dev` only; compose runs gateway + deps only.

## Docker Compose updates

- Add `gateway` service (depends on `kafka`/`redis`/`postgres` optional; really depends only on workflow being reachable).
- Add `admin-ui` service (nginx) or document host-only UI.
- Published ports example: `8080` gateway, `8082` workflow (if workflow containerized in future); for current “workflow on host” dev, only gateway in compose + UI container optional.

## Documentation updates

- [README](../README.md): new “Run gateway + UI” section, port matrix, env vars.
- [LOCAL_ARCHITECTURE.md](LOCAL_ARCHITECTURE.md): diagram node for Gateway + Admin UI.
- [PRD.md](PRD.md) appendix: note that local reference now includes gateway + dashboard.

## Testing

- Smoke: create campaign and job from UI, observe Kafka UI topics, verify job reaches `SUCCESS`.
- Gateway: verify CORS from Vite origin, verify 404 for unknown routes.

## Risks / follow-ups

- **Two Spring apps** duplicate some config; keep shared conventions only in docs (no shared Gradle parent unless you want a `pulseflow/settings.gradle.kts` composite later).
- **Rate limit + auth** can stay minimal in v1 to ship UI + routing quickly.

## Implementation order

1. Change workflow default port to 8082; verify local README.
2. Scaffold `gateway` module with routes + CORS + health.
3. Add read list APIs to workflow if needed.
4. Scaffold `admin-ui` with Tailwind and MVP screens.
5. Dockerfiles + compose services + doc updates.

## Decision (stack shape)

**Default:** containerize **workflow**, **gateway**, and **admin-ui** in Docker Compose so `docker compose up` reproduces the full path (gateway as single entry, UI static or nginx, workflow internal DNS name `http://workflow:8082`). Document an optional **host workflow** workflow for Kotlin debugging (`bootRun` on 8082 while gateway points at `host.docker.internal`).
