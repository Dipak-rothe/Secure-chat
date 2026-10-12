# SecureChat — Project Verification & Build Status

**Last Updated:** 2026-10-12  
**Monorepo Version:** v0.1.0  
**Configured Server Port:** 8090 (migrated from standard 8080/8081)

---

## 1. Environment & Tooling Verification

| Component | Status | Verified Version / Details |
|---|---|---|
| Operating System | ✅ Verified | Windows 10/11 x64 |
| JDK | ✅ Verified | Oracle JDK 21.0.12.1 (`C:\Program Files\Java\jdk-21.0.12.1`) |
| Maven | ✅ Verified | Apache Maven 3.9.16 |
| Gradle Wrapper | ✅ Verified | Gradle 8.10.2 (`.\gradlew.bat`) |
| Git | ✅ Verified | Git 2.47.0 (Portable MinGit) |
| WiX Toolset | ✅ Verified | WiX 3.11 (`%USERPROFILE%\tools\wix311`) — used by `jpackage` to create `.exe` installer |
| Android SDK | ✅ Verified | Android SDK 34.0.0 (`%USERPROFILE%\tools\android-sdk`) with cmdline-tools, platform 34, build-tools 34.0.0 |
| Docker | ⚠️ Offline | Unit & Integration tests verify with H2 in-memory mode; production Dockerfile & compose files provided |

---

## 2. Artifacts Produced & Verified

### 1. Windows Installer (`.exe`)
- **Location:** `desktop/build/dist/SecureChat-0.1.0.exe`
- **Size:** 83,696,128 bytes (~83.7 MB)
- **Built With:** `scripts/package-windows.bat` via JDK 21 `jpackage` + WiX 3.11
- **Includes:** Bundled minimal Java runtime, JavaFX 21 modules, dependencies, desktop launcher.

### 2. Android Debug APK (`.apk`)
- **Location:** `android/build/outputs/apk/debug/android-debug.apk`
- **Size:** 7,337,625 bytes (~7.3 MB)
- **Built With:** `.\gradlew.bat :android:assembleDebug`
- **Includes:** Android 14 (API 34) target, AndroidX, EncryptedSharedPreferences storage, Material 3 UI, vector launcher icons.

---

## 3. Server & API Verification

- **Default Port:** `8090` (configured across `server/src/main/resources/application.yml`, desktop, and Android).
- **Live Verification:**
  - `POST http://localhost:8090/api/auth/register` -> `201 Created` with JWT access token + refresh token.
  - `POST http://localhost:8090/api/auth/login` -> `200 OK`.
  - `GET http://localhost:8090/api/users/me` -> `200 OK` (with Bearer JWT).
  - Swagger UI: `http://localhost:8090/swagger-ui/index.html` -> `200 OK`.

---

## 4. Test Suite Execution

- `server/`: `mvn test` -> **16 tests run, 0 failures, 0 errors, 0 skipped** (BUILD SUCCESS).
- `core/`: `.\gradlew.bat :core:test` -> **4 tests run, 0 failures, 0 skipped** (BUILD SUCCESS).
- `desktop/`: `.\gradlew.bat :desktop:classes` -> **BUILD SUCCESS**.
- `android/`: `.\gradlew.bat :android:assembleDebug` -> **BUILD SUCCESS**.
