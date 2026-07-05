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
- `@KafkaListener` commits offsets automatically after the listener method returns without throwing — the default `AckMode.BATCH` commits offsets for the whole batch. If the method throws, the offset is not committed and the message can be redelivered. This can be changed via `spring.kafka.listener.ack-mode`: `RECORD` commits after every single record; `MANUAL` requires the listener to call `Acknowledgment.acknowledge()` explicitly (useful when you need to delay the commit until a downstream write — e.g. to a database — succeeds); `MANUAL_IMMEDIATE` is the same but commits synchronously. `AckMode.BATCH` has no parameters of its own — batch size is controlled by `spring.kafka.consumer.max-poll-records` (default 500) and commit frequency is driven by the poll cycle, not a configurable interval.
- Dockerfiles copy POMs before source so Maven dependency resolution is a separate cached layer — rebuilds after source-only changes skip the slow `dependency:go-offline` step.
- No partition key is set when producing — `kafkaTemplate.send(TOPIC, message)` uses round-robin partitioning. This is intentional: Wikimedia recent-change events are independent so there is no ordering requirement between them, and round-robin gives the most even load distribution. A key (e.g. page ID) would only be needed if multiple consumers required all edits to the same page to land on the same partition and be processed in order. If a key were added, `domain` from the event metadata would be a natural choice — each event payload contains a `meta.domain` field (e.g. `en.wikipedia.org`, `fr.wikipedia.org`) identifying which wiki the edit came from. To use it you would deserialize the JSON into a Java record, then call `kafkaTemplate.send(TOPIC, event.meta().domain(), rawJson)`. Currently the message is forwarded as a raw JSON string with no deserialization in the producer.
- `@Component` tells Spring to detect the class during classpath scanning and register it as a bean in the application context. `@Service` and `@Repository` are specializations that carry the same registration behavior but add semantic meaning. `WikimediaChangesHandler` uses `@Component` because it's an infrastructure/integration piece that doesn't belong to the service or data-access layer; `WikimediaChangesProducer` uses `@Service` because it encapsulates the logic of sending a message.
