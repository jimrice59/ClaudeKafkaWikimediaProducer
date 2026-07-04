# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

A Maven multi-module Spring Boot 3.3 / Java 21 project that bridges the Wikimedia real-time SSE event stream into Apache Kafka.

- **kafka-producer** — connects to `https://stream.wikimedia.org/v2/stream/recentchange` via Spring WebFlux `WebClient`, filters blank SSE frames, and publishes each JSON payload to the `wikimedia.recentchange` Kafka topic.
- **kafka-consumer** — listens to `wikimedia.recentchange` and logs each received event. No embedded web server (`spring.main.web-application-type=none`).

Base package: `com.jimrice.wikimedia`

## Running with Docker Compose (preferred)

```bash
# Build images and start Kafka + producer + consumer
docker compose up --build

# Tear down
docker compose down
```

Kafka runs in KRaft mode (no Zookeeper) via `bitnami/kafka:3.7`. The producer and consumer containers wait for Kafka's healthcheck before starting.

Kafka listeners:
- `kafka:9092` — used internally between containers
- `localhost:9094` — use this from the host for CLI tools or IDE connections

## Running Locally (without Docker)

Kafka must be running on `localhost:9092` before starting either module. The topic `wikimedia.recentchange` is auto-created on first use.

```bash
# Build all modules from the root
mvn clean package -DskipTests

# Run the producer (streams indefinitely via blockLast())
mvn -pl kafka-producer spring-boot:run

# Run the consumer (separate terminal)
mvn -pl kafka-consumer spring-boot:run
```

## Key Design Decisions

- `WikimediaChangesHandler` implements `CommandLineRunner` because the SSE stream must start after the full application context is ready (Kafka wired, beans initialized) but has no natural trigger — no HTTP endpoint, no schedule, no event. `CommandLineRunner.run()` is Spring Boot's hook for "execute once at startup." The `blockLast()` call at the end keeps the JVM alive; without it `run()` would return, Spring would see no active threads, and the app would exit.
- `Flux` is used to consume the Wikimedia SSE stream because the endpoint is a single long-lived HTTP connection that emits an unbounded sequence of events over time — exactly what `Flux` models. `WebClient.bodyToFlux(ServerSentEvent.class)` decodes each SSE frame as it arrives rather than waiting for a complete response (which never comes). A blocking client like `RestTemplate` would require a dedicated thread sitting blocked on the socket for the lifetime of the stream; `Flux` lets the reactive scheduler reuse threads between events.
- The consumer has no HTTP server; `spring-boot-starter` (not webflux/web) is used so no Netty/Tomcat starts.
- Both modules share `spring-kafka` dependency management from the parent POM.
- Dockerfiles copy POMs before source so Maven dependency resolution is a separate cached layer — rebuilds after source-only changes skip the slow `dependency:go-offline` step.
