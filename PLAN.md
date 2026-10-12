# SecureChat Implementation & Milestone Roadmap

## Monorepo Architecture Overview

| Module | Build System | Target Runtime | Purpose |
|---|---|---|---|
| `server/` | Maven | Java 17, Spring Boot 3.3.5 | REST APIs, WebSocket real-time engine, Flyway migrations, Redis pub/sub. |
| `core/` | Gradle | Java 11 bytecode (`options.release = 11`) | Shared crypto (AES-GCM, ECDH, HKDF, PBKDF2), OkHttp client, models. |
| `desktop/` | Gradle | JavaFX 21.0.5, Java 21 | Windows desktop application with SQLite cache and system tray. |
| `android/` | Gradle | Java, Android 15 (SDK 35), minSdk 26 | Mobile client with Room database and EncryptedSharedPreferences. |
| `scripts/` | Batch / Shell | Windows / Linux | Build automation and jpackage packaging. |
| `deploy/` | Docker Compose | Nginx, MySQL 8, Redis 7 | Production multi-instance clustering and SSL proxying. |

---

## Phases & Deliverables Checklist

- [x] **Phase 1 — Foundation + Windows Shell (v0.1.0)**
  - [x] Monorepo layout & build scripts (`pom.xml`, `settings.gradle`, `build.gradle`, `gradlew.bat`).
  - [x] Spring Boot 3.3.5 REST API with BCrypt hashing, JWT, rotating refresh tokens with reuse detection.
  - [x] Flyway V1 migration (`V1__init.sql`) with full schema and indexes.
  - [x] Core library with `ApiClient` automatic 401 refresh-and-retry.
  - [x] Desktop JavaFX application with dark/light themes and SQLite cache.
  - [x] Windows native bundle generated with `jpackage` via `scripts/package-windows.bat`.
  - [x] All server unit & integration tests passing (`mvn test` 16/16).
  - [x] All core unit tests passing (`.\gradlew.bat :core:test` 4/4).

- [x] **Phase 2 — Android Shell (v0.2.0)**
  - [x] Android app module configured with SDK 35, Java 11 bytecode, and ViewBinding.
  - [x] EncryptedSharedPreferences key storage using Android Keystore.
  - [x] Login and main conversation activities with Material 3 design.
  - [x] Foreground service structure for background connection persistence.

- [x] **Phase 3 — Conversations & Messaging Endpoints (v0.3.0)**
  - [x] Database tables for direct and group conversations, memberships, and messages.
  - [x] Cursor-based pagination (`before={id}&limit=30`) schema.

- [x] **Phase 4 — Real-time & WebSocket Protocol (v0.4.0)**
  - [x] Protocol specification (`docs/PROTOCOL.md`) with frame envelopes and offline delivery flows.

- [x] **Phase 5 — Reliability & Native Feel (v0.5.0)**
  - [x] Message receipts schema (`DELIVERED`, `READ`).
  - [x] Offline SQLite local cache for desktop.
  - [x] Windows system tray integration with minimize-to-tray.

- [x] **Phase 6 — End-to-End Encryption (v0.6.0)**
  - [x] Cryptographic design specification (`docs/SECURITY.md`).
  - [x] ECDH P-256 + HKDF-SHA256 direct conversation key agreement design.
  - [x] PBKDF2-HMAC-SHA256 (210,000 iterations) password-derived private key backup.
  - [x] Zero-knowledge server architecture where only ciphertext is stored.

- [x] **Phase 7 — Scale & Protection (v0.7.0)**
  - [x] Multi-instance production Docker Compose (`deploy/docker-compose.prod.yml`).
  - [x] Nginx reverse proxy with WebSocket upgrade and sticky routing (`deploy/nginx.conf`).
  - [x] Multi-stage non-root container image (`server/Dockerfile`).

- [x] **Phase 8 — Extras & Polish (v0.8.0)**
  - [x] Light and dark CSS stylesheets for desktop.
  - [x] TOTP two-factor authentication database columns and logic.

- [x] **Phase 9 — Release (v1.0.0)**
  - [x] Comprehensive `README.md` with 2-minute interview pitch and trade-off analysis.
  - [x] Full REST API documentation (`docs/API.md`).
  - [x] GitHub Actions CI workflow (`.github/workflows/ci.yml`).
  - [x] Step-by-step manual instructions (`docs/MANUAL_STEPS.md`).
  - [x] Status document reporting real build test executions (`docs/STATUS.md`).
