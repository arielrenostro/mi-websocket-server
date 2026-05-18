# mi-remote-tuning-server

Brokering server for remote vehicle calibration. Connects two types of clients in real time — **Tuner** (calibration tool) and **Customer** (vehicle/ECU) — and relays serial data between them after pairing.

## Stack

- Kotlin + Spring Boot 4
- Spring WebSocket (STOMP-free, raw handler)
- Spring MVC (REST)
- Java 21

## Architecture

The server uses a hybrid model:

- **WebSocket** manages presence and data exchange — open connection = client online, closed connection = client offline.
- **REST** manages discrete operations — listing, pairing, querying state.

```
Tuner                        Server                        Customer
  |                              |                              |
  |-- WS connect (X-Client-Name) |                              |
  |<-- REGISTERED {id, secret} --|                              |
  |                              |   WS connect (X-Client-Name) |
  |                              |<-- REGISTERED {id, secret} --|
  |                              |                              |
  |-- GET /api/customers ------->|                              |
  |<-- [{id, name, connected}] --|                              |
  |                              |                              |
  |-- POST /api/connections ---->|                              |
  |                              |---- REGISTER_TUNER WS push ->|
  |                              |<-- POST /respond (accepted) -|
  |<-- WS: PAIR_CONNECTED -------|                              |
  |                              |---- WS: PAIR_CONNECTED ----->|
  |                              |                              |
  |<====== ECHO_SERIAL_DATA ===============================>   |
```

## Running

### Local

Requires Java 21.

```bash
./gradlew bootRun
```

### Docker

```bash
docker build -t mi-remote-tuning-server .
docker run -p 8080:8080 mi-remote-tuning-server
```

The server listens on port `8080` by default. To change it, edit `application.properties`:

```properties
server.port=9090
server.address=0.0.0.0
```

## WebSocket Connection

Two endpoints available:

| Endpoint | Actor |
|---|---|
| `ws://host:8080/ws/tuner` | Tuner |
| `ws://host:8080/ws/customer` | Customer |

### First connection — new registration

```
X-Client-Name: <client name>
```

The server responds with a `REGISTERED` push containing `id` and `secret`:

```json
{
  "type": "REGISTERED",
  "timestamp": 1715000000000,
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "name": "MyTuner",
  "secret": "a3f1c29e8b4d6e0f7a2c5d8e1b4f9c3a"
}
```

Save `id` and `secret` — they are the credentials for reconnection and REST calls.

### Reconnection — resume existing session

```
X-Client-Id:     <id>
X-Client-Secret: <secret>
```

The server confirms with `REGISTERED` and, if the client was paired, follows up with `PAIR_CONNECTED` to resync state.

## REST Authentication

All REST calls require the headers:

```
X-Client-Id:     <id>
X-Client-Secret: <secret>
```

Invalid or missing credentials return `401 Unauthorized`.

## API

Full documentation of REST endpoints and WebSocket messages with JSON examples in [`API.md`](./API.md).

## Development

```bash
# Build
./gradlew build

# Tests
./gradlew test

# Single test
./gradlew test --tests "com.masterinjection.remotetuningserver.<ClassName>"
```
