# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Remote Tuning Server for **Master Injection** — a Spring Boot 4 / Kotlin application that brokers real-time communication between two types of clients: **Tuners** (calibration tools) and **Customers** (vehicles/ECUs being tuned). The server pairs them and relays serial data between them.

## Commands

```bash
# Run locally
./gradlew bootRun

# Build fat JAR
./gradlew bootJar

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.masterinjection.remotetuningserver.application.RemoteTuningServerApplicationTests"

# Build Docker image
docker build -t mi-remote-tuning-server .
```

**Requirements:** Java 21 toolchain (Gradle wrapper handles it).

## Architecture

### Hybrid: WS manages presence, REST manages operations

WebSocket connection = presence signal (no polling needed). REST handles discrete control-plane operations, authenticated with the secret generated at WS registration time.

### WebSocket connection lifecycle

| Header | Value | Effect |
|---|---|---|
| `X-Client-Name` | any string | **New registration** — server creates entity, returns `REGISTERED {id, name, secret}` |
| `X-Client-Id` + `X-Client-Secret` | from prior `REGISTERED` push | **Reconnect** — resumes existing session; if paired, also pushes `PAIR_CONNECTED` to re-sync state |

Endpoints: `/ws/tuner` and `/ws/customer`. WS disconnect ≠ unregistration — entity remains in memory for **2 minutes** after disconnect, then is auto-removed. Explicit REST `DELETE` removes immediately. Disconnect triggers `PAIR_DISCONNECTED` to the peer if currently paired.

### REST API

All endpoints require `X-Client-Id` and `X-Client-Secret` headers (credentials from `REGISTERED` push). `GET /api/customers` validates a **tuner** secret; all others validate the matching entity type.

| Method | Path | Who calls | Description |
|---|---|---|---|
| `DELETE` | `/api/tuners/{id}` | Tuner | Explicit unregistration |
| `GET` | `/api/tuners/{id}/state` | Tuner | Own state |
| `DELETE` | `/api/customers/{id}` | Customer | Explicit unregistration |
| `GET` | `/api/customers/{id}/state` | Customer | Own state |
| `GET` | `/api/customers` | Tuner | List all customers (including `DISCONNECTED`) |
| `POST` | `/api/connections` | Tuner | Request pairing `{customerId}` → `{requestId}` |
| `POST` | `/api/connections/{requestId}/respond` | Customer | Accept/reject `{accepted}` |

### Pairing flow

```
Tuner  ──POST /api/connections──────────────────────► Server
                                                        │
Customer ◄──── WS push: REGISTER_TUNER ────────────────┘
Customer ──POST /api/connections/{id}/respond──────────► Server
                                                        │
Tuner   ◄───── WS push: PAIR_CONNECTED {peer} ─────────┤
Customer ◄──── WS push: PAIR_CONNECTED {peer} ─────────┘
```

If customer doesn't respond within 15 seconds, tuner receives `REGISTER_TO_CUSTOMER_RESPONSE { success: false }`.

### Client status (`ClientStatus`)

Each tuner and customer carries a `status: ClientStatus` field:

| Status | Meaning |
|---|---|
| `CONNECTED` | WS active, not paired |
| `PAIRED` | WS active and paired with a peer |
| `DISCONNECTED` | WS closed; entity pending 2-min auto-removal |

Transitions: new connection / reconnect → `CONNECTED`; pairing accepted → `PAIRED` (both sides); WS disconnect / unregister → peer reverts to `CONNECTED`, disconnected side → `DISCONNECTED`. Entities with `DISCONNECTED` status are auto-removed 2 minutes after disconnect; reconnecting before that window cancels the removal.

### Session state (all in-memory, `TuningSessionService`)

- `tuners` / `customers` — keyed by entity ID; includes entities in `DISCONNECTED` state (pending 2-min cleanup)
- `tunersByWsSessionId` / `customersByWsSessionId` — reverse maps for O(1) WS-session lookup (only active sessions)
- `pendingRegisters` — in-flight pairing requests, keyed by `requestId` (Unix ms timestamp)

### WS message reference

| Type | Direction | Description |
|---|---|---|
| `REGISTERED` | Server → Client | Sent on new connection and reconnect; includes `{id, name, secret}` |
| `PAIR_CONNECTED` | Server → Client | Sent to **both** tuner and customer when pairing is confirmed; includes `{peer: {id, name}}` |
| `PAIR_DISCONNECTED` | Server → Client | Sent to surviving peer when the other WS disconnects or entity is unregistered |
| `REGISTER_TUNER` | Server → Customer | Incoming pairing request from a tuner; includes `{tuner: {id, name}}` |
| `REGISTER_TO_CUSTOMER_RESPONSE` | Server → Tuner | Async result for timed-out or rejected connection requests |
| `ECHO_SERIAL_DATA` | Bidirectional | Serial data relayed transparently to the paired peer |
| `ERROR` | Server → Client | Error details with optional `responseTo` timestamp |

### Adding a new message type

1. Add the value to `MessageType`
2. Create the class extending `BaseMessage` (or `BaseResponseMessage` for responses with `responseTo`)
3. Register it in `BaseMessage`'s `@JsonSubTypes`
4. Add a `when` branch in `TuningSessionService.onTunerMessage` or `onCustomerMessage`

### Extension functions

`WebSocketSessionExtension.kt` provides `sendJsonMessage` and `sendJsonError` on `WebSocketSession`. `GlobalMapper` is a singleton `ObjectMapper` initialized at startup via `JacksonConfig`.
