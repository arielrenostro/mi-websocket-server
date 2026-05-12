# mi-websocket-server

Servidor de intermediação para calibração remota de veículos. Conecta em tempo real dois tipos de clientes — **Tuner** (ferramenta de calibração) e **Customer** (veículo/ECU) — e retransmite dados seriais entre eles após o pareamento.

## Stack

- Kotlin + Spring Boot 4
- Spring WebSocket (STOMP-free, handler raw)
- Spring MVC (REST)
- Java 21

## Arquitetura

O servidor adota um modelo híbrido:

- **WebSocket** gerencia presença e troca de dados — conexão aberta = cliente online, conexão fechada = cliente offline.
- **REST** gerencia operações discretas — listar, parear, consultar estado.

```
Tuner                        Servidor                      Customer
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

## Executando

### Local

Requer Java 21.

```bash
./gradlew bootRun
```

### Docker

```bash
docker build -t mi-websocket-server .
docker run -p 8080:8080 mi-websocket-server
```

O servidor sobe na porta `8080` por padrão. Para alterar, edite `application.properties`:

```properties
server.port=9090
server.address=0.0.0.0
```

## Conexão WebSocket

Dois endpoints disponíveis:

| Endpoint | Actor |
|---|---|
| `ws://host:8080/ws/tuner` | Tuner |
| `ws://host:8080/ws/customer` | Customer |

### Primeiro acesso — novo registro

```
X-Client-Name: <nome do cliente>
```

O servidor responde com um push `REGISTERED` contendo `id` e `secret`:

```json
{
  "type": "REGISTERED",
  "timestamp": 1715000000000,
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "name": "MeuTuner",
  "secret": "a3f1c29e8b4d6e0f7a2c5d8e1b4f9c3a"
}
```

Guarde `id` e `secret` — são as credenciais para reconexão e chamadas REST.

### Reconexão — retoma sessão existente

```
X-Client-Id:     <id>
X-Client-Secret: <secret>
```

O servidor confirma com `REGISTERED` e, se o cliente estiver pareado, envia `PAIR_CONNECTED` em seguida para ressincronizar o estado.

## Autenticação REST

Todas as chamadas REST exigem os headers:

```
X-Client-Id:     <id>
X-Client-Secret: <secret>
```

Credenciais inválidas ou ausentes retornam `401 Unauthorized`.

## API

Documentação completa dos endpoints REST e mensagens WebSocket com exemplos JSON em [`API.md`](./API.md).

## Desenvolvimento

```bash
# Compilar
./gradlew build

# Testes
./gradlew test

# Teste único
./gradlew test --tests "com.masterinjection.websocket.<NomeDaClasse>"
```
