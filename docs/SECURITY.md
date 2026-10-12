# SecureChat Security Specification & Cryptographic Design

## 1. Cryptographic Primitives

All cryptographic operations are implemented using standard JDK primitives (`java.security`, `javax.crypto`). No custom, proprietary, or unverified primitives are used.

- **Asymmetric Key Agreement**: Elliptic Curve Diffie-Hellman (ECDH) over the NIST P-256 curve (`secp256r1`).
- **Symmetric Encryption**: AES-256 in Galois/Counter Mode (GCM) with 128-bit authentication tags and fresh 12-byte random IVs per message.
- **Key Derivation (KDF)**:
  - **HKDF-SHA256**: RFC 5869 extract-and-expand scheme for deriving conversation keys from ECDH shared secrets.
  - **PBKDF2-HMAC-SHA256**: 210,000 iterations with 16-byte cryptographically secure random salt for password-based key derivation.
- **Fingerprinting & Hashing**: SHA-256 for public key fingerprints, refresh token hashes, and integrity checks.

---

## 2. Key Management & Life Cycle

### 2.1 Identity Key Pair
- Generated on the client device during account registration:
  - Algorithm: EC (Elliptic Curve), standard P-256 curve.
  - Public Key: Exported in X.509 SPKI format, Base64-encoded, and sent to server.
  - Private Key: PKCS#8 format. **Never leaves client unencrypted.**

### 2.2 Safety Numbers (Key Verification)
- To prevent Man-In-The-Middle (MITM) attacks, users can verify each other's keys out-of-band:
  - `Fingerprint = SHA-256(PublicKeyBytes)`
  - Formatted in the UI as 8 groups of 4 hexadecimal digits (e.g., `A1B2 C3D4 E5F6 0718 ...`).

### 2.3 Private Key Backup at Rest
- A client encrypts its private key locally:
  - Password derived using PBKDF2-HMAC-SHA256 (210,000 iterations, 16-byte random salt).
  - Derived key encrypts the PKCS#8 private key using AES-256-GCM.
  - Encrypted ciphertext, salt, and iteration count are backed up to `user_keys` table.
  - The server holds only the ciphertext and cannot decrypt it without the user's password.
  - *Trade-off*: If a user forgets their password, past encrypted messages cannot be recovered.

---

## 3. Message Encryption Specification

### 3.1 Direct (1:1) Conversations
1. Client A fetches Client B's public key from `/api/users/{B}/keys`.
2. Client A performs ECDH with Client A's private key and Client B's public key, yielding `sharedSecret`.
3. Client A executes HKDF-SHA256:
   - `info = "securechat-v1|direct|" + conversationId`
   - `salt = null` (or conversation creation timestamp)
   - Result: 32-byte AES key `K_direct`.
4. Client A encrypts message content:
   - Cipher: `AES/GCM/NoPadding`
   - Key: `K_direct`
   - IV: 12-byte cryptographically secure random bytes
   - AAD (Additional Authenticated Data): `conversationId|senderId|clientMessageId|keyVersion`
   - Tag Length: 128 bits
5. Wire Format: `ciphertext` (Base64) and `iv` (Base64).

### 3.2 Group Conversations
1. Group creator generates random 32-byte AES key `K_group_v1`.
2. For each member M, `K_group_v1` is encrypted (wrapped) with AES-GCM using an ECDH key derived between creator and M.
3. Wrapped keys are uploaded to `group_keys` table.
4. When a member is removed, the admin generates a new key `K_group_v2` for remaining members and uploads new wrapped keys (`key_version + 1`).

---

## 4. Threat Model & Limitations

### What is protected:
- **Server compromise**: An attacker with full database and server access cannot read message content or decrypt private keys.
- **Wire eavesdropping**: All transport traffic uses TLS/WSS; payload content has independent application-layer E2E encryption.
- **Token theft**: Rotating refresh tokens invalidate the entire token family upon reuse detection.

### Limitations:
- **No Forward Secrecy per message**: Ratcheting (Signal Double Ratchet) is not implemented in v1; static conversation keys are derived via ECDH.
- **Metadata leakage**: The server observes who converses with whom, message timestamps, and message sizes.
- **Compromised endpoint**: Malware on the client device with memory access can extract decrypted messages.
