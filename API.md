# API Reference — mi-websocket-server

Servidor de intermediação WebSocket para calibração remota. Clientes são **Tuners** (ferramenta de calibração) e **Customers** (veículo/ECU).

---

## Visão geral do fluxo

```
1. Cliente abre WebSocket  →  recebe REGISTERED {id, secret}
2. Tuner lista customers   →  GET /api/customers
3. Tuner solicita pair     →  POST /api/connections
4. Customer recebe push    ←  WS: REGISTER_TUNER
5. Customer aceita         →  POST /api/connections/{requestId}/respond
6. Ambos recebem push      ←  WS: PAIR_CONNECTED
7. Troca de dados serial   ↔  WS: ECHO_SERIAL_DATA (bidirecional)
8. Qualquer lado desconecta→  outro recebe WS: PAIR_DISCONNECTED
```

---

## WebSocket

### Endpoints

| Endpoint | Actor |
|---|---|
| `ws://host/ws/tuner` | Tuner |
| `ws://host/ws/customer` | Customer |

### Conexão — novo registro

Envie o header `X-Client-Name` na abertura do WebSocket (sem `X-Client-Id`).

```
GET /ws/tuner
X-Client-Name: MeuTuner
```

Resposta imediata (push `REGISTERED`):

```json
{
  "type": "REGISTERED",
  "timestamp": 1715000000000,
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "name": "MeuTuner",
  "secret": "a3f1c29e8b4d6e0f7a2c5d8e1b4f9c3a"
}
```

> Guarde `id` e `secret` — são usados para reconexão e para autenticar as chamadas REST.

### Conexão — reconexão (retoma sessão existente)

Envie `X-Client-Id` + `X-Client-Secret` (sem `X-Client-Name`).

```
GET /ws/tuner
X-Client-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
X-Client-Secret: a3f1c29e8b4d6e0f7a2c5d8e1b4f9c3a
```

Resposta imediata (push `REGISTERED`):

```json
{
  "type": "REGISTERED",
  "timestamp": 1715000001000,
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "name": "MeuTuner",
  "secret": "a3f1c29e8b4d6e0f7a2c5d8e1b4f9c3a"
}
```

Se estiver pareado, também recebe imediatamente:

```json
{
  "type": "PAIR_CONNECTED",
  "timestamp": 1715000001001,
  "peer": {
    "id": "c9a1b2e3-44f5-6789-abcd-ef0123456789",
    "name": "Carro01"
  }
}
```

---

## Mensagens WebSocket — referência completa

Todas as mensagens são JSON com o campo `type` como discriminador.

### `REGISTERED` — server → client

Enviado ao conectar ou reconectar.

```json
{
  "type": "REGISTERED",
  "timestamp": 1715000000000,
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "name": "MeuTuner",
  "secret": "a3f1c29e8b4d6e0f7a2c5d8e1b4f9c3a"
}
```

---

### `REGISTER_TUNER` — server → customer

Customer recebe quando um Tuner solicita pareamento.

```json
{
  "type": "REGISTER_TUNER",
  "timestamp": 1715000010000,
  "tuner": {
    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "name": "MeuTuner"
  }
}
```

> O campo `timestamp` deste push é o `requestId` usado em `POST /api/connections/{requestId}/respond`.

---

### `PAIR_CONNECTED` — server → tuner e customer

Enviado para **ambos** quando o pareamento é confirmado. Também enviado ao reconectar se já estiver pareado.

```json
{
  "type": "PAIR_CONNECTED",
  "timestamp": 1715000011000,
  "peer": {
    "id": "c9a1b2e3-44f5-6789-abcd-ef0123456789",
    "name": "Carro01"
  }
}
```

---

### `PAIR_DISCONNECTED` — server → client

Enviado ao peer sobrevivente quando o outro lado desconecta o WebSocket ou é desregistrado.

```json
{
  "type": "PAIR_DISCONNECTED",
  "timestamp": 1715000020000
}
```

---

### `REGISTER_TO_CUSTOMER_RESPONSE` — server → tuner

Enviado ao Tuner quando a solicitação de pair expira (15s) ou é rejeitada pelo Customer.

```json
{
  "type": "REGISTER_TO_CUSTOMER_RESPONSE",
  "timestamp": 1715000025000,
  "responseTo": 1715000010000,
  "success": false
}
```

---

### `ECHO_SERIAL_DATA` — bidirecional

Enviado pelo Tuner ou Customer; o servidor repassa diretamente ao par.

**Envio (cliente → servidor):**

```json
{
  "type": "ECHO_SERIAL_DATA",
  "timestamp": 1715000030000,
  "data": "0200010A"
}
```

**Recebimento (servidor → peer):** mesma estrutura.

---

### `ERROR` — server → client

```json
{
  "type": "ERROR",
  "timestamp": 1715000005000,
  "responseTo": 1715000004000,
  "message": "Tuner has no paired customer with an active WebSocket"
}
```

> `responseTo` é o `timestamp` da mensagem que causou o erro; pode ser `null` para erros não correlacionados.

---

## REST

### Autenticação

Endpoints com `{id}` no path usam apenas `X-Client-Secret` (o ID já está na URL):

```
X-Client-Secret: <secret recebido no REGISTERED>
```

Endpoints sem `{id}` no path (list, connections) usam ambos:

```
X-Client-Id:     <id recebido no REGISTERED>
X-Client-Secret: <secret recebido no REGISTERED>
```

Erros de autenticação retornam `401 Unauthorized`:

```json
{ "message": "Invalid secret for tuner f47ac10b-..." }
```

---

### Tuner

#### `GET /api/tuners/{id}/state`

Retorna o estado atual do tuner.

**Request:**
```
GET /api/tuners/f47ac10b-58cc-4372-a567-0e02b2c3d479/state
X-Client-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
X-Client-Secret: a3f1c29e8b4d6e0f7a2c5d8e1b4f9c3a
```

**Response `200`:**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "name": "MeuTuner",
  "status": "CONNECTED"
}
```

> `status` pode ser `CONNECTED` (WS ativo, sem par), `PAIRED` (WS ativo e pareado) ou `DISCONNECTED` (WS fechado, entidade aguarda remoção em 2 min).

---

#### `DELETE /api/tuners/{id}`

Desregistra o tuner explicitamente. O peer pareado recebe `PAIR_DISCONNECTED`.

**Request:**
```
DELETE /api/tuners/f47ac10b-58cc-4372-a567-0e02b2c3d479
X-Client-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
X-Client-Secret: a3f1c29e8b4d6e0f7a2c5d8e1b4f9c3a
```

**Response `204 No Content`**

---

### Customer

#### `GET /api/customers`

Lista todos os customers registrados. Requer credenciais de **Tuner**.

**Request:**
```
GET /api/customers
X-Client-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
X-Client-Secret: a3f1c29e8b4d6e0f7a2c5d8e1b4f9c3a
```

**Response `200`:**
```json
[
  {
    "id": "c9a1b2e3-44f5-6789-abcd-ef0123456789",
    "name": "Carro01",
    "status": "CONNECTED"
  },
  {
    "id": "d8e7f6a5-3b2c-1d0e-9f8a-7b6c5d4e3f2a",
    "name": "Carro02",
    "status": "PAIRED"
  },
  {
    "id": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b",
    "name": "Carro03",
    "status": "DISCONNECTED"
  }
]
```

> Inclui customers em todos os estados. Customers `DISCONNECTED` serão removidos automaticamente após 2 minutos; não possuem WebSocket ativo e não podem receber solicitações de pair.

---

#### `GET /api/customers/{id}/state`

**Request:**
```
GET /api/customers/c9a1b2e3-44f5-6789-abcd-ef0123456789/state
X-Client-Id: c9a1b2e3-44f5-6789-abcd-ef0123456789
X-Client-Secret: 9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d4e
```

**Response `200`:**
```json
{
  "id": "c9a1b2e3-44f5-6789-abcd-ef0123456789",
  "name": "Carro01",
  "status": "CONNECTED"
}
```

> `status` pode ser `CONNECTED`, `PAIRED` ou `DISCONNECTED`. Veja detalhes em [GET /api/tuners/{id}/state](#get-apitunersidstate).

---

#### `DELETE /api/customers/{id}`

**Request:**
```
DELETE /api/customers/c9a1b2e3-44f5-6789-abcd-ef0123456789
X-Client-Id: c9a1b2e3-44f5-6789-abcd-ef0123456789
X-Client-Secret: 9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d4e
```

**Response `204 No Content`**

---

### Conexão (pairing)

#### `POST /api/connections`

Tuner solicita pareamento com um Customer. O Customer recebe push `REGISTER_TUNER` via WebSocket.

**Request:**
```
POST /api/connections
X-Client-Id: f47ac10b-58cc-4372-a567-0e02b2c3d479
X-Client-Secret: a3f1c29e8b4d6e0f7a2c5d8e1b4f9c3a
Content-Type: application/json

{
  "customerId": "c9a1b2e3-44f5-6789-abcd-ef0123456789"
}
```

**Response `200`:**
```json
{
  "requestId": 1715000010000
}
```

> Timeout: se o Customer não responder em 15 segundos, o Tuner recebe `REGISTER_TO_CUSTOMER_RESPONSE { success: false }`.

---

#### `POST /api/connections/{requestId}/respond`

Customer aceita ou rejeita a solicitação. O `requestId` é o `timestamp` do push `REGISTER_TUNER` recebido.

Se `accepted: true`, ambos recebem push `PAIR_CONNECTED` imediatamente.  
Se `accepted: false`, o Tuner recebe `REGISTER_TO_CUSTOMER_RESPONSE { success: false }`.

**Request:**
```
POST /api/connections/1715000010000/respond
X-Client-Id: c9a1b2e3-44f5-6789-abcd-ef0123456789
X-Client-Secret: 9b8c7d6e5f4a3b2c1d0e9f8a7b6c5d4e
Content-Type: application/json

{
  "accepted": true
}
```

**Response `200 OK`**

---

## Respostas de erro

| Status | Quando |
|---|---|
| `400 Bad Request` | ID não encontrado, request inválido |
| `401 Unauthorized` | Secret incorreto |
| `409 Conflict` | Operação inválida no estado atual (ex: já pareado) |
