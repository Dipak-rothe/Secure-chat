# SecureChat REST API Reference

All REST endpoints reside under the `/api` prefix. Standard JSON request and response envelopes are used throughout.

Base URL: `http://localhost:8080/api` (or configured server host)

---

## 1. Authentication (`/api/auth`)

### 1.1 Register User
**`POST /api/auth/register`** (Public)

Creates a new user account and returns tokens with user profile.

**Request Body:**
```json
{
  "name": "Alice Smith",
  "email": "alice@example.com",
  "password": "SuperSecretPassword123"
}
```

**Response (201 Created):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "dGhpcy1pcy1hLXJhbmRvbS10b2tlbi1zdHJpbmc",
  "user": {
    "id": 1,
    "name": "Alice Smith",
    "email": "alice@example.com",
    "lastSeen": null,
    "createdAt": "2026-10-12T10:00:00Z",
    "online": false
  }
}
```

---

### 1.2 Login
**`POST /api/auth/login`** (Public)

Authenticates credentials and returns new access + rotating refresh token.

**Request Body:**
```json
{
  "email": "alice@example.com",
  "password": "SuperSecretPassword123",
  "totpCode": null
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "bmV3LXJlZnJlc2gtdG9rZW4tc3RyaW5n",
  "user": {
    "id": 1,
    "name": "Alice Smith",
    "email": "alice@example.com",
    "lastSeen": "2026-10-12T09:59:00Z",
    "createdAt": "2026-10-12T09:00:00Z",
    "online": true
  }
}
```

---

### 1.3 Rotate Refresh Token
**`POST /api/auth/refresh`** (Public)

Refreshes an expired access token. Implements rotating refresh tokens with reuse detection.

**Request Body:**
```json
{
  "refreshToken": "bmV3LXJlZnJlc2gtdG9rZW4tc3RyaW5n"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "YW5vdGhlci1uZXctcmVmcmVzaC10b2tlbg",
  "user": {
    "id": 1,
    "name": "Alice Smith",
    "email": "alice@example.com"
  }
}
```

---

### 1.4 Logout
**`POST /api/auth/logout`** (Bearer Auth)

Revokes all active refresh tokens for the authenticated user.

**Headers:**
`Authorization: Bearer <accessToken>`

**Response (204 No Content)**

---

## 2. Users (`/api/users`)

### 2.1 Get Current Profile
**`GET /api/users/me`** (Bearer Auth)

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Alice Smith",
  "email": "alice@example.com",
  "lastSeen": "2026-10-12T10:05:00Z",
  "createdAt": "2026-10-12T09:00:00Z",
  "online": true
}
```

---

### 2.2 Search Users
**`GET /api/users/search?q={query}`** (Bearer Auth)

Searches users by name or email prefix (2 to 10 characters). Excludes self and blocked users.

**Response (200 OK):**
```json
[
  {
    "id": 2,
    "name": "Bob Jones",
    "email": "bob@example.com",
    "lastSeen": "2026-10-12T09:45:00Z",
    "online": false
  }
]
```

---

### 2.3 Upload Public Key Bundle
**`PUT /api/users/me/keys`** (Bearer Auth)

Uploads the client's public EC key and encrypted private key backup.

**Request Body:**
```json
{
  "publicKey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...",
  "keyFingerprint": "A1B2 C3D4 E5F6 0718 293A 4B5C 6D7E 8F90",
  "encryptedPrivateKeyBackup": "AES-GCM-CIPHERTEXT-BASE64...",
  "kdfSalt": "SALT-BASE64...",
  "kdfIterations": 210000
}
```

**Response (200 OK):** (Same as uploaded bundle with createdAt timestamp)

---

### 2.4 Get User's Public Key
**`GET /api/users/{id}/keys`** (Bearer Auth)

Fetches a peer's public key to perform ECDH key agreement.

**Response (200 OK):**
```json
{
  "userId": 2,
  "publicKey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...",
  "keyFingerprint": "B2C3 D4E5 F607 1829 3A4B 5C6D 7E8F 90A1",
  "createdAt": "2026-10-12T09:10:00Z"
}
```

---

## 3. Error Envelope

All API errors return consistent JSON:
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    "Email must be a well-formed email address"
  ],
  "timestamp": "2026-10-12T10:15:30.123Z"
}
```
