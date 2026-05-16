# PulseFlow — Distributed Notification and Workflow Orchestration Platform

## Complete Product Requirements Document (PRD)

---

## 1. Product Overview

### Product Name

**PulseFlow**

### Product Type

Distributed event-driven workflow orchestration and notification execution platform.

### Elevator Pitch

PulseFlow is a scalable distributed backend platform that enables organizations to execute large-scale asynchronous workflows such as:

- notifications
- document delivery
- onboarding journeys
- payment reminders
- campaign execution
- scheduled jobs

using:

- Kafka-based event streaming
- Redis-backed coordination
- distributed worker processing
- retry orchestration
- fault-tolerant execution
- real-time observability

The system is designed to process millions of workflow events reliably with:

- idempotency
- retries
- distributed locking
- scheduling
- monitoring
- horizontal scalability

---

## 2. Problem Statement

Modern businesses need to execute workflows at massive scale.

Examples:

- send payment reminders
- generate invoices
- deliver onboarding emails
- process KYC notifications
- trigger post-payment journeys
- execute scheduled campaigns

Traditional synchronous systems fail because:

- APIs block threads
- failures cause data inconsistency
- duplicate execution occurs
- systems cannot scale under spikes
- retries are difficult
- observability is poor

PulseFlow solves this through:

- asynchronous event-driven processing
- distributed workflow orchestration
- fault-tolerant retries
- scalable worker architecture
- real-time monitoring

---

## 3. Product Goals

### Primary Goals

- Process millions of workflow events reliably
- Support asynchronous distributed execution
- Ensure fault-tolerant processing
- Prevent duplicate execution
- Provide operational visibility
- Support horizontal scalability

---

## 4. Real-World Use Cases

### 4.1 Notification Campaigns

Example:

```text
Send KYC reminders to 2M users
```

### 4.2 Document Delivery

Example:

```text
Generate and deliver invoices asynchronously
```

### 4.3 Payment Workflows

Example:

```text
Trigger post-payment state transitions
```

### 4.4 Scheduled User Journeys

Example:

```text
Send onboarding emails after signup
```

---

## 5. System Architecture

```text
                     +-------------------+
                     | Admin Dashboard   |
                     +-------------------+
                               |
                               v
                     +-------------------+
                     | API Gateway       |
                     +-------------------+
                               |
                               v
                     +-------------------+
                     | Workflow Service  |
                     +-------------------+
                               |
                               v
                     +-------------------+
                     | Kafka Topics      |
                     +-------------------+
                               |
        --------------------------------------------------
        |                  |                 |            |
        v                  v                 v            v
   Worker 1           Worker 2          Worker 3     Retry Worker
        |                  |                 |            |
        --------------------------------------------------
                               |
              -------------------------------------
              |                                   |
              v                                   v
      +---------------+                  +----------------+
      | Redis         |                  | PostgreSQL     |
      +---------------+                  +----------------+
                               |
                               v
                     +-------------------+
                     | Metrics Service   |
                     +-------------------+
                               |
                               v
                     +-------------------+
                     | Grafana Dashboard |
                     +-------------------+
```

---

## 6. Core Components

### 6.1 Admin Dashboard

#### Purpose

Provides operational control and visibility.

#### Features

- create campaigns
- upload users
- schedule workflows
- replay failed jobs
- monitor throughput
- view failures
- inspect retries

#### Tech Stack

- React
- TailwindCSS

#### Why Needed

Without UI:

- operations become difficult
- debugging becomes harder
- workflows cannot be visualized

### 6.2 API Gateway

#### Purpose

Single entry point for all clients.

#### Responsibilities

- authentication
- request routing
- validation
- rate limiting

#### Why Needed

Centralized traffic management improves:

- security
- scalability
- maintainability

### 6.3 Workflow Service

#### Purpose

Core orchestration engine.

#### Responsibilities

- create jobs
- persist workflow state
- publish Kafka events
- manage state transitions

#### Why Needed

Central workflow coordination ensures:

- reliable orchestration
- state consistency
- asynchronous execution

### 6.4 Kafka Event Streaming

#### Technology

Apache Kafka

#### Why Kafka Is Used

Kafka enables:

- asynchronous communication
- decoupled services
- high throughput
- fault-tolerant event processing
- replayability

Traditional REST chaining creates:

- tight coupling
- cascading failures
- scalability issues

Kafka solves this.

#### Where Kafka Is Used

##### Campaign Creation

```text
campaign.created
```

##### Notification Execution

```text
notification.send
```

##### Retry Processing

```text
notification.retry
```

##### Failed Jobs

```text
notification.dlq
```

#### Why Event-Driven Architecture

Instead of:

```text
Service A -> waits for Service B
```

we use:

```text
Service A publishes event
Consumers process independently
```

Benefits:

- scalability
- resilience
- async execution
- loose coupling

### 6.5 Worker Cluster

#### Purpose

Execute jobs asynchronously.

#### Responsibilities

- consume Kafka messages
- execute workflows
- process retries
- update status
- ensure idempotency

#### Why Distributed Workers

Single worker cannot scale.

Multiple workers allow:

- horizontal scaling
- parallel execution
- fault tolerance

#### Why Thread Pools Are Used

##### Problem

Creating threads repeatedly is expensive.

##### Solution

Custom thread pool executors.

##### Benefits

- lower memory overhead
- better concurrency
- controlled parallelism
- improved TPS

### 6.6 Redis

#### Technology

Redis

#### Why Redis Is Required

Redis provides:

- distributed locking
- caching
- deduplication
- workflow staging
- fast access

#### Where Redis Is Used

##### Distributed Locks

###### Problem

Multiple workers may process same job simultaneously.

###### Solution

Redis locks.

Example:

```text
SETNX lock:jobId
```

Prevents:

- duplicate notifications
- race conditions
- inconsistent execution

##### Workflow Stages

Redis stores:

```text
stage_1_complete
stage_2_complete
```

Used for:

- orchestration tracking
- state transitions

##### Idempotency

Redis tracks processed jobs.

Prevents:

```text
same event processed twice
```

##### Rate Limiting

Redis counters prevent overload.

### 6.7 PostgreSQL

#### Why PostgreSQL

Persistent durable storage.

Needed because:

- Redis is in-memory
- workflow history must persist

#### What PostgreSQL Stores

##### Campaign Metadata

```text
campaign name
schedule
status
```

##### Job State

```text
QUEUED
PROCESSING
FAILED
SUCCESS
```

##### Retry History

Tracks:

- retry attempts
- failure causes

##### Delivery Logs

Stores:

- execution history
- audit trails

#### Why Both Redis and PostgreSQL

##### Redis

Fast temporary operational state.

##### PostgreSQL

Durable long-term persistence.

This separation improves:

- performance
- scalability
- reliability

### 6.8 Retry Scheduler

#### Purpose

Automatically retry failed jobs.

#### Why Required

Distributed systems fail constantly due to:

- network errors
- service downtime
- DB timeouts

Retries improve reliability.

#### Retry Flow

```text
Job fails
   |
Retry scheduled
   |
Exponential backoff
   |
Retry execution
```

#### Exponential Backoff

Example:

```text
Retry 1 -> 5 sec
Retry 2 -> 30 sec
Retry 3 -> 2 min
```

Prevents system overload.

### 6.9 Dead Letter Queue (DLQ)

#### Purpose

Store permanently failed jobs.

Kafka topic:

```text
notification.dlq
```

#### Why Required

Without DLQ:

- failed jobs disappear
- debugging impossible

DLQ enables:

- replay
- debugging
- manual inspection

### 6.10 Monitoring and Observability

#### Technologies

- Prometheus
- Grafana

#### Why Observability Is Critical

Distributed systems are difficult to debug.

Need visibility into:

- throughput
- failures
- retries
- lag
- worker health

#### Metrics Tracked

##### Kafka Consumer Lag

Detect slow consumers.

##### TPS

Requests/jobs per second.

##### Retry Count

Failure trends.

##### DLQ Size

System reliability indicator.

##### Worker Utilization

Scaling insights.

---

## 7. Functional Features

### 7.1 Campaign Management

Users can:

- create workflows
- upload CSVs
- define schedules

### 7.2 Delayed Execution

Supports:

```text
run after 1 hour
run tomorrow
```

### 7.3 Retry Handling

Automatic retries with:

- configurable attempts
- backoff strategy

### 7.4 Distributed Concurrency Control

Ensures:

- safe parallel execution
- no duplicate processing

### 7.5 Replay Failed Jobs

Allows:

```text
Replay DLQ jobs
```

### 7.6 Horizontal Scaling

Workers scale independently.

Example:

```text
5 workers -> 50 workers
```

---

## 8. Non-Functional Requirements

### Scalability

- support millions of jobs
- horizontal worker scaling

### Availability

- worker crash recovery
- retry guarantees

### Reliability

- idempotent processing
- durable state management

### Performance

- high-throughput async processing
- low-latency execution

### Observability

- full operational visibility

---

## 9. Database Schema

### jobs Table

| Column | Purpose |
| --- | --- |
| id | unique job ID |
| workflow_id | workflow reference |
| payload | execution data |
| status | current state |
| retry_count | retry tracking |
| created_at | creation timestamp |
| updated_at | update timestamp |

The reference implementation also stores an optional `idempotency_key` column for deduplication across replays.

---

## 10. Kafka Topics

| Topic | Purpose |
| --- | --- |
| campaign.created | new campaigns |
| notification.send | execution queue |
| notification.retry | retries |
| notification.dlq | failures |
| notification.completed | completion events |

---

## 11. Failure Handling

### Worker Crash

Another worker resumes processing.

### Duplicate Events

Handled via:

- Redis locks
- idempotency keys

### Kafka Downtime

Consumer retries enabled.

### Database Failure

Transactional rollback.

---

## 12. Future Enhancements

- workflow DAGs
- multi-region execution
- Kubernetes autoscaling
- distributed tracing
- ML-based retry prioritization

---

## Appendix: Reference implementation in this repository

This repository ships a local Docker Compose stack (Kafka, **FlashCache** instead of Redis for coordination, PostgreSQL, Kafka UI) plus Kotlin/Spring Boot services under `pulseflow/backend` (workflow), `pulseflow/gateway` (Spring Cloud Gateway), and a React + Tailwind admin UI under `pulseflow/admin-ui` served via nginx. Together they exercise the core Kafka topics, PostgreSQL persistence, distributed coordination (via the FlashCache SDK), retries, DLQ publication, replay, and an operator-facing dashboard. Browser traffic typically enters through the UI on port 3000, which reverse-proxies `/api` to the gateway on port 8080, which forwards `/api/v1` calls to the workflow service on port 8082.

This is a reference implementation for local learning and demos; it is not a full production deployment of every PRD component (for example, no separate hardened edge gateway product or enterprise SSO integration).
