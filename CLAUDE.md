# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

A Maven multi-module Spring Boot 3.3 / Java 21 project that bridges the Wikimedia real-time SSE event stream into Apache Kafka.

- **kafka-producer** — connects to `https://stream.wikimedia.org/v2/stream/recentchange` via Spring WebFlux `WebClient`, filters blank SSE frames, and publishes each JSON payload to the `wikimedia.recentchange` Kafka topic.
- **kafka-consumer** — listens to `wikimedia.recentchange` and logs each received event. No embedded web server (`spring.main.web-application-type=none`).

Base package: `com.jimrice.wikimedia`

## Prerequisites

Kafka must be running on `localhost:9092` before starting either module. The topic `wikimedia.recentchange` is auto-created on first use.

## Build & Run

```bash
# Build all modules from the root
mvn clean package -DskipTests

# Run the producer (streams indefinitely via blockLast())
mvn -pl kafka-producer spring-boot:run

# Run the consumer (separate terminal)
mvn -pl kafka-consumer spring-boot:run
```

## Key Design Decisions

- `WikimediaChangesHandler` implements `CommandLineRunner` and calls `blockLast()` on the reactive SSE flux — this keeps the producer JVM alive while the stream runs.
- The consumer has no HTTP server; `spring-boot-starter` (not webflux/web) is used so no Netty/Tomcat starts.
- Both modules share `spring-kafka` dependency management from the parent POM.
