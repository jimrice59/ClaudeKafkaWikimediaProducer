# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

A Kafka producer that consumes the Wikimedia real-time event stream (SSE) and publishes events to a Kafka topic. This is a tech-prep/learning project.

## Project Status

This project is newly initialized — no source files exist yet. Update this file once the tech stack and build tooling are chosen.

## Intended Architecture

- **Source**: Wikimedia EventStreams SSE endpoint (`https://stream.wikimedia.org/v2/stream/recentchange`)
- **Sink**: Apache Kafka topic (e.g., `wikimedia.recentchange`)
- **Role**: Bridges the Wikimedia HTTP SSE stream into Kafka for downstream consumers

## Development Commands

_To be filled in once the project language and build tool are chosen (e.g., Maven/Gradle for Java, pip for Python, npm for Node)._
