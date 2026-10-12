# SecureChat — Project Verification & Build Status

**Last Updated:** 2026-10-12
**Monorepo Version:** v0.1.0 (Phase 1)

---

## 1. Environment & Tooling Verification

| Component | Status | Verified Version / Details |
|---|---|---|
| Operating System | ✅ Verified | Windows 10/11 x64 |
| JDK | ✅ Verified | Oracle JDK 21.0.12.1 (`C:\Program Files\Java\jdk-21.0.12.1`) |
| Maven | ✅ Verified | Apache Maven 3.9.16 |
| Gradle Wrapper | ✅ Verified | Gradle 8.10.2 (`.\gradlew.bat`) |
| Git | ✅ Verified | Git 2.47.0 (MinGit portable configured) |
| Docker | ⚠️ Not Installed | Local machine does not have Docker Desktop running. Documented in MANUAL_STEPS.md; unit tests run via H2 in-memory in MySQL mode. |
| WiX Toolset | ⚠️ Not Installed | Single-file installer generator not present. Fallback `TYPE=app-image` verified successfully via `jpackage`. |
| Android SDK | ⚠️ Not Installed | Android Studio / SDK command-line tools not installed on host. Documented in MANUAL_STEPS.md. |

---

## 2. Phase-by-Phase Execution & Test Log

### Phase 1 - Foundation + Windows Shell (v0.1.0)
- **Built Components**:
  - Monorepo structure with Maven (`server/`) and Gradle multi-project (`core/`, `desktop/`, `android/`).
  - Flyway V1 migration with schema for users, user_keys, conversations, messages, receipts, group keys, and refresh tokens.
  - Spring Boot 3.3.5 backend with Spring Security (stateless JWT), BCrypt password hashing, rotating refresh tokens with reuse detection.
  - Global exception handler with standard `{status, message, errors, timestamp}` envelope.
  - Core library (`core/`) targeting Java 11 with OkHttp 4.12.0, Gson 2.11.0, ApiClient with automatic 401 refresh-and-retry, and ChatApi.
  - Desktop application (`desktop/`) with JavaFX 21.0.5, dark/light theme CSS, FileSecureKeyStorage (AES-GCM encrypted persistence), SqliteLocalStore, LoginScreen, MainScreen, and UserSearchDialog.
  - Windows packaging script (`scripts/package-windows.bat`) with automatic JDK detection and jpackage app-image fallback.
- **Commands Executed & Verified**:
  - `mvn test` in `server/`: **16 tests run, 0 failures, 0 errors, 0 skipped** (BUILD SUCCESS).
  - `.\gradlew.bat :core:test` in root: **4 tests run, 0 failures, 0 skipped** (BUILD SUCCESS).
  - `.\gradlew.bat :desktop:classes` in root: **Compiled successfully** (BUILD SUCCESS).
  - `scripts\package-windows.bat`: **jpackage produced `SecureChat.exe` and bundled runtime** under `desktop\build\dist\SecureChat\` (PACKAGE COMPLETE).
- **Git Commit & Tag**:
  - Commit: `feat: phase 1 - foundation and windows shell`
  - Tag: `v0.1.0`
