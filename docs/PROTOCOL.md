# SecureChat WebSocket & Real-Time Protocol Specification

Endpoint: `/ws`

## 1. Handshake & Authentication

The client initiates an HTTP WebSocket upgrade request to `/ws`.
Authentication is performed during the handshake using the HTTP `Authorization` header:

```http
GET /ws HTTP/1.1
Host: localhost:8080
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
Authorization: Bearer <accessToken>
```

- If `Authorization` is absent or invalid, the handshake fails with HTTP **401 Unauthorized**.
- If the token is valid, the connection upgrades to a WebSocket channel.

## 2. Frame Format

Every message frame across the WebSocket connection is a JSON object with this standard envelope:

```json
{
  "type": "MESSAGE_TYPE",
  "requestId": "optional-uuid-for-request-tracing",
  "payload": { ... }
}
```

---

## 3. Client-to-Server Messages

### 3.1 `SEND_MESSAGE`
Client sends an end-to-end encrypted message.
```json
{
  "type": "SEND_MESSAGE",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "payload": {
    "conversationId": 12,
    "clientMessageId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "type": "TEXT",
    "ciphertext": "AES-256-GCM-CIPHERTEXT-BASE64...",
    "iv": "12-BYTE-IV-BASE64...",
    "keyVersion": 1,
    "replyToId": null
  }
}
```
*Idempotency:* If the server receives the same `(senderId, clientMessageId)` pair, it does not create a duplicate row and returns the existing message ID in the `MESSAGE_ACK`.

### 3.2 `MARK_DELIVERED`
Client acknowledges delivery of one or more messages.
```json
{
  "type": "MARK_DELIVERED",
  "payload": {
    "messageIds": [101, 102, 103]
  }
}
```

### 3.3 `MARK_READ`
Client reports that messages in a conversation have been viewed by the user.
```json
{
  "type": "MARK_READ",
  "payload": {
    "conversationId": 12,
    "lastReadMessageId": 103
  }
}
```

### 3.4 `TYPING`
Client sends typing status indicator.
```json
{
  "type": "TYPING",
  "payload": {
    "conversationId": 12,
    "isTyping": true
  }
}
```

### 3.5 `PING`
Client heartbeat frame to keep the connection alive and refresh presence in Redis.
```json
{
  "type": "PING",
  "payload": {}
}
```

---

## 4. Server-to-Client Messages

### 4.1 `MESSAGE_ACK`
Confirms persistence and server assignment of message ID.
```json
{
  "type": "MESSAGE_ACK",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "payload": {
    "clientMessageId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "messageId": 104,
    "createdAt": "2026-10-12T10:20:00Z"
  }
}
```

### 4.2 `MESSAGE`
Pushed to recipient clients in real time.
```json
{
  "type": "MESSAGE",
  "payload": {
    "id": 104,
    "conversationId": 12,
    "senderId": 1,
    "clientMessageId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "type": "TEXT",
    "ciphertext": "AES-256-GCM-CIPHERTEXT-BASE64...",
    "iv": "12-BYTE-IV-BASE64...",
    "keyVersion": 1,
    "replyToId": null,
    "createdAt": "2026-10-12T10:20:00Z"
  }
}
```

### 4.3 `MESSAGE_STATUS`
Status update when a message is delivered or read.
```json
{
  "type": "MESSAGE_STATUS",
  "payload": {
    "messageId": 104,
    "userId": 2,
    "status": "READ",
    "timestamp": "2026-10-12T10:21:00Z"
  }
}
```

### 4.4 `PRESENCE`
Broadcasted when a user connects or disconnects.
```json
{
  "type": "PRESENCE",
  "payload": {
    "userId": 2,
    "online": true,
    "lastSeen": "2026-10-12T10:20:00Z"
  }
}
```

---

## 5. Offline Delivery & Backlog Sync

When a client reconnects after being offline:
1. Client connects with handshake token.
2. Client sends its `lastSeenMessageId` in the connection handshake or initial sync frame.
3. Server queries the database for all undelivered messages where `id > lastSeenMessageId` for conversations the user is a member of.
4. Messages are pushed sequentially in chronological order.
5. Client acknowledges delivery with `MARK_DELIVERED`.
