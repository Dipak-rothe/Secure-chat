# SecureChat — Complete Software Architecture, Frontend, Backend & Operations Guide

**Version:** 0.1.0  
**Author:** Dipak Rothe  
**Application Type:** Full-Stack End-to-End Encrypted Cross-Platform Chat System (Windows `.exe` + Android `.apk` + Spring Boot Server)  
**Default Server Port:** `8090`

---

## Table of Contents

1. [Executive Summary & Interview Pitch](#1-executive-summary--interview-pitch)
2. [High-Level System Architecture](#2-high-level-system-architecture)
3. [Complete Monorepo File Structure](#3-complete-monorepo-file-structure)
4. [Backend Deep Dive (`server/`)](#4-backend-deep-dive-server)
5. [Shared Core Library Deep Dive (`core/`)](#5-shared-core-library-deep-dive-core)
6. [Windows Desktop Frontend Deep Dive (`desktop/`)](#6-windows-desktop-frontend-deep-dive-desktop)
7. [Android Mobile Frontend Deep Dive (`android/`)](#7-android-mobile-frontend-deep-dive-android)
8. [Database Schema & Persistence (`Flyway V1`)](#8-database-schema--persistence-flyway-v1)
9. [Security & Encryption Architecture](#9-security--encryption-architecture)
10. [Step-by-Step Guide: How to Run the Software](#10-step-by-step-guide-how-to-run-the-software)
11. [How to Connect & Chat Across Two Different PCs](#11-how-to-connect--chat-across-two-different-pcs)
12. [How to Build the `.exe` and `.apk` Installers](#12-how-to-build-the-exe-and-apk-installers)
13. [Troubleshooting & Common Fixes](#13-troubleshooting--common-fixes)

---

## 1. Executive Summary & Interview Pitch

**SecureChat** is a production-architected, real-time messaging platform built for **Windows Desktop (`SecureChat-0.1.0.exe`)** and **Android (`android-debug.apk`)**, powered by a stateless **Spring Boot 3** backend.

### Interview Pitch
> *"SecureChat is a WhatsApp-style cross-platform messaging system for Windows and Android with end-to-end encryption, JWT authentication with rotating refresh tokens and reuse detection, shared client networking in a pure Java 11 core module, and native installers built with JDK 21 `jpackage` (WiX) and the Android Gradle Plugin."*

---

## 2. High-Level System Architecture

```
+-----------------------------------------------------------------------+
|                           CLIENT APPLICATIONS                         |
|                                                                       |
|  +---------------------------------+  +----------------------------+  |
|  |   Windows Desktop App (JavaFX)  |  |  Android App (Native Java) |  |
|  |  - LoginScreen / MainScreen     |  |  - LoginActivity           |  |
|  |  - UserSearchDialog             |  |  - MainActivity            |  |
|  |  - FileSecureKeyStorage (AES)   |  |  - EncryptedSharedPrefs    |  |
|  |  - SqliteLocalStore (SQLite)    |  |  - RoomLocalStore (SQLite) |  |
|  +----------------+----------------+  +-------------+--------------+  |
|                   |                                 |                 |
|                   +----------------+----------------+                 |
|                                    |                                  |
|                                    v                                  |
|             +---------------------------------------------+           |
|             |       Shared Core Library (Java 11)         |           |
|             |  - ChatApi (High-level REST facade)         |           |
|             |  - ApiClient (OkHttp 4.12 + Gson 2.11)      |           |
|             |  - Auto 401 JWT Refresh & Retry (ThreadSafe)|           |
|             +----------------------+----------------------+           |
+------------------------------------|----------------------------------+
                                     |
                        HTTP/1.1 REST & WebSocket
                        (Port 8090 / Reverse Tunnel)
                                     |
                                     v
+-----------------------------------------------------------------------+
|                     SPRING BOOT 3.3.5 BACKEND SERVER                  |
|                                                                       |
|  +-----------------------------------------------------------------+  |
|  | Security Filter Chain (JwtAuthFilter + SecurityConfig)          |  |
|  +-----------------------------------------------------------------+  |
|  | REST Controllers:                                               |  |
|  |  - AuthController (/api/auth/register, /login, /refresh, /logout)|  |
|  |  - UserController (/api/users/me, /search, /keys)               |  |
|  |  - ConversationController (/api/conversations/direct, /messages)|  |
|  +-----------------------------------------------------------------+  |
|  | Business Services:                                              |  |
|  |  - AuthService (BCrypt cost-12)                                 |  |
|  |  - TokenService (SHA-256 refresh token rotation & reuse check)  |  |
|  |  - UserService & ConversationService                            |  |
|  +-----------------------------------------------------------------+  |
|  | Persistence Layer (Spring Data JPA + Hibernate + Flyway):       |  |
|  |  - Local Dev: H2 File Database (./data/securechatdb.mv.db)      |  |
|  |  - Production: MySQL 8.0 + Redis 7                              |  |
|  +-----------------------------------------------------------------+  |
+-----------------------------------------------------------------------+
```

---

## 3. Complete Monorepo File Structure

```text
secure-chat/
├── run-live.bat                  # One-click live runner (starts server, tunnel & desktop app)
├── settings.gradle               # Gradle multi-project definition (core, desktop, android)
├── build.gradle                  # Root Gradle configuration
├── gradle.properties             # AndroidX & JVM memory settings
├── local.properties              # Local Android SDK path pointer
│
├── server/                       # BACKEND (Maven Project — Java 17, Spring Boot 3.3.5)
│   ├── pom.xml                   # Maven dependencies & plugins
│   └── src/
│       ├── main/
│       │   ├── java/com/dipak/server/
│       │   │   ├── SecureChatServerApplication.java
│       │   │   ├── config/       # OpenApiConfig (Swagger), RedisConfig
│       │   │   ├── controller/   # AuthController, UserController, ConversationController
│       │   │   ├── domain/       # User, UserKey, RefreshToken, Conversation,
│       │   │   │                 # ConversationMember, Message
│       │   │   ├── dto/          # RegisterRequest, LoginRequest, AuthResponse, UserDto,
│       │   │   │                 # ConversationDto, MessageDto, SendMessageRequest, ErrorResponse
│       │   │   ├── exception/    # AppException, GlobalExceptionHandler
│       │   │   ├── repository/   # UserRepository, RefreshTokenRepository, ConversationRepository,
│       │   │   │                 # ConversationMemberRepository, MessageRepository
│       │   │   ├── security/     # JwtService, JwtAuthFilter, SecurityConfig, SecurityUtils
│       │   │   └── service/      # AuthService, TokenService, UserService, ConversationService
│       │   └── resources/
│       │       ├── application.yml           # Server config (Port 8090, H2 file DB, JWT settings)
│       │       ├── application-prod.yml      # Production MySQL + Redis config
│       │       └── db/migration/V1__init.sql # Complete Flyway SQL database schema
│       └── test/                 # JUnit 5 + MockMvc integration & unit tests (16 tests)
│
├── core/                         # SHARED CLIENT LIBRARY (Gradle — Java 11)
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/dipak/chatcore/
│       │   ├── api/              # ApiClient.java, ChatApi.java, ApiException.java
│       │   ├── model/            # UserModel, AuthResponse, ConversationModel, MessageModel, etc.
│       │   └── store/            # SecureKeyStorage.java, LocalStore.java (Interfaces)
│       └── test/                 # ApiClientTest.java (OkHttp MockWebServer tests)
│
├── desktop/                      # WINDOWS DESKTOP APP (Gradle — Java 17/21, JavaFX 21.0.5)
│   ├── build.gradle
│   ├── build/dist/               # Output folder containing SecureChat-0.1.0.exe
│   └── src/main/
│       ├── java/com/dipak/desktop/
│       │   ├── Launcher.java     # Non-JavaFX entrypoint required for fat-jar/jpackage
│       │   ├── App.java          # Main JavaFX Application class & theme manager
│       │   ├── store/            # FileSecureKeyStorage (AES-256-GCM), SqliteLocalStore
│       │   └── ui/               # LoginScreen.java, MainScreen.java, UserSearchDialog.java
│       └── resources/styles/     # dark.css, light.css
│
├── android/                      # ANDROID MOBILE APP (Gradle — Java, Android SDK 34)
│   ├── build.gradle
│   ├── build/outputs/apk/debug/  # Output folder containing android-debug.apk
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/dipak/securechat/
│       │   ├── SecureChatApp.java
│       │   ├── service/          # ChatForegroundService.java
│       │   ├── store/            # AndroidKeystoreStorage.java, RoomLocalStore.java
│       │   └── ui/               # LoginActivity.java, MainActivity.java
│       └── res/                  # XML layouts, Material 3 themes, adaptive launcher icons
│
├── scripts/
│   ├── package-windows.bat       # Builds SecureChat-0.1.0.exe using jpackage + WiX 3.11
│   └── run-live.bat              # Runs server + desktop live from source code
│
├── deploy/                       # Production Docker Compose & Nginx config
└── docs/                         # Architecture, API, Protocol, Security & Status docs
```

---

## 4. Backend Deep Dive (`server/`)

### Technology Stack
* **Language & Runtime:** Java 17 (runs on JDK 17 or JDK 21)
* **Framework:** Spring Boot 3.3.5
* **Security:** Spring Security 6 (Stateless filter chain) + `jjwt` 0.12.6 (HMAC-SHA256 JWTs) + BCrypt (cost factor 12)
* **Database & Migration:** Spring Data JPA (Hibernate 6.5) + Flyway 10.15 + H2 (local file persistence) / MySQL 8.0 (production)
* **API Documentation:** `springdoc-openapi` 2.6.0 (Swagger UI at `http://localhost:8090/swagger-ui.html`)
* **Hot Reload:** `spring-boot-devtools` enabled for live development reloads
* **Port:** `8090`

### How Backend Request Processing Works
1. **Incoming HTTP Request** hits port `8090`.
2. **`JwtAuthFilter`** inspects the `Authorization: Bearer <token>` header:
   * If present and valid (verified by `JwtService`), it extracts the `userId` subject claim and populates Spring's `SecurityContextHolder`.
3. **`SecurityConfig`** enforces route protection:
   * Public routes: `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `/swagger-ui/**`, `/actuator/health`.
   * Protected routes: All `/api/users/**` and `/api/conversations/**` endpoints require a valid JWT; otherwise returns a clean `401 Unauthorized` JSON envelope.
4. **Controllers & Services:**
   * **`AuthController` / `AuthService`**:
     * `POST /api/auth/register`: Normalizes email, checks uniqueness (`409 Conflict` if duplicate), hashes password with BCrypt(12), saves user, issues a 15-minute JWT access token + 7-day opaque refresh token.
     * `POST /api/auth/login`: Verifies email and BCrypt password hash in constant time, returns tokens and `UserDto`.
     * `POST /api/auth/refresh`: Uses `TokenService` to hash the raw refresh token with SHA-256, checks if it was already revoked (**Reuse Detection**: if a stolen/old token is reused, revokes *all* tokens for that user immediately), issues a fresh token pair.
   * **`UserController` / `UserService`**:
     * `GET /api/users/me`: Returns current authenticated user's profile.
     * `GET /api/users/search?q=<2-10 chars>`: Case-insensitive prefix search by name or email, excluding the requester.
     * `PUT /api/users/me/keys` & `GET /api/users/{id}/keys`: Uploads and retrieves E2E public key bundles.
   * **`ConversationController` / `ConversationService`**:
     * `POST /api/conversations/direct/{targetUserId}`: Computes canonical key `"minId:maxId"` so two users always share a single unique direct conversation; creates `Conversation` and `ConversationMember` rows if new.
     * `GET /api/conversations`: Lists all conversations for the logged-in user and resolves the peer's display name.
     * `POST /api/conversations/{id}/messages`: Verifies membership, saves the message (`ciphertext`, `iv`, `senderId`, `clientMessageId`), and returns `MessageDto`.
     * `GET /api/conversations/{id}/messages`: Returns ordered messages in the conversation.

---

## 5. Shared Core Library Deep Dive (`core/`)

To avoid duplicating networking and authentication logic between Windows and Android, `core/` is compiled strictly to **Java 11 bytecode** (`options.release = 11`) with zero platform-specific dependencies.

### Key Components
1. **`ApiClient.java`**:
   * Built on **OkHttp 4.12.0** and **Gson 2.11.0**.
   * Configured with `Protocol.HTTP_1_1`, `retryOnConnectionFailure(true)`, and `X-Pinggy-No-Screen: true` so it works seamlessly over both local LAN and reverse SSH/HTTPS tunnels.
   * **Automatic Token Injection**: Reads `access_token` from `SecureKeyStorage` and attaches `Authorization: Bearer <token>`.
   * **Thread-Safe 401 Refresh-and-Retry**: If any API call returns `401 Unauthorized`, `ApiClient` enters a `synchronized (refreshLock)` block, exchanges the `refresh_token` at `/api/auth/refresh` for a new token pair, saves them, and transparently retries the original request once.
2. **`ChatApi.java`**:
   * High-level strongly-typed methods: `register()`, `login()`, `logout()`, `getMe()`, `searchUsers()`, `createOrGetDirectConversation()`, `getConversations()`, `getMessages()`, `sendMessage()`.
   * Deserializes arrays using typed array classes (`UserModel[].class`, `ConversationModel[].class`, `MessageModel[].class`) to prevent Java generic type erasure issues.
3. **`SecureKeyStorage` & `LocalStore`**:
   * Clean interfaces implemented natively by the Desktop and Android modules.

---

## 6. Windows Desktop Frontend Deep Dive (`desktop/`)

### Technology Stack
* **UI Framework:** JavaFX 21.0.5 (pure Java code layout, zero FXML overhead, styled with CSS)
* **Local Storage:**
  * `FileSecureKeyStorage`: Encrypts tokens and private keys on disk at `~/.securechat/keys.enc` using **AES-256-GCM** with a machine-bound key.
  * `SqliteLocalStore`: Caches user profile in SQLite (`~/.securechat/cache.db`) via `sqlite-jdbc`.
* **Packaging:** JDK 21 `jpackage` + WiX Toolset 3.11 producing a self-contained Windows installer (`SecureChat-0.1.0.exe`).

### UI Screens & Workflow
1. **`Launcher.java` & `App.java`**:
   * `Launcher` boots JavaFX without module-path issues.
   * `App` initializes storage, manages Dark/Light theme switching (`dark.css` / `light.css`), and transitions between Login and Main screens.
2. **`LoginScreen.java`**:
   * Displays the **Server URL** input box (defaults to `http://localhost:8090`, editable to any LAN IP or tunnel URL).
   * Provides **Sign In** and **Register** tabs with background thread execution (`ExecutorService`) and `Platform.runLater()` UI updates so the window never freezes.
3. **`MainScreen.java`**:
   * **Left Sidebar**: Current user's name, `+ New Chat` button, `Toggle Theme` button, `Log Out` button, and the live `Chats` list (`ListView<ConversationModel>`).
   * **Center Chat Pane**:
     * **Header**: Contact name (`Chat with <name>`) and `🔒 End-to-End Encrypted` badge.
     * **Message History (`ListView<MessageModel>`)**: Custom bubble cell renderer — your sent messages align right in blue (`#89b4fa`), received messages align left in dark slate (`#313244`).
     * **Bottom Input Bar**: Text field (`Type a message...`) + `Send` button (supports pressing `Enter` to send).
     * **Real-Time Polling Engine**: A daemon `ScheduledExecutorService` polls `/api/conversations/{id}/messages` every **1 second** while a chat is open, automatically appending incoming messages and scrolling to the newest bubble.
4. **`UserSearchDialog.java`**:
   * Modal popup triggered by `+ New Chat`. Searches users by prefix (`2-10` chars), renders `Name (email)`, and opens/creates the direct conversation upon clicking **Start Chat**.

---

## 7. Android Mobile Frontend Deep Dive (`android/`)

### Technology Stack
* **Language & SDK:** Native Java, `minSdk = 26` (Android 8.0), `targetSdk = 34` (Android 14)
* **UI:** AndroidX AppCompat, Material Components 3 (`Theme.Material3.DayNight.NoActionBar`), ConstraintLayout
* **Security & Persistence:**
  * `AndroidKeystoreStorage`: Uses Jetpack Security `EncryptedSharedPreferences` backed by the hardware `AndroidKeyStore` (`MasterKey.KeyScheme.AES256_GCM`).
  * `RoomLocalStore`: SQLite database caching user profile on device.
* **Screens & Services:**
  * `LoginActivity`: Material 3 login & registration card with configurable Server URL (defaults to `http://10.0.2.2:8090` for emulator / editable for real devices).
  * `MainActivity`: Displays profile, user search dialog, theme toggle, and starts the foreground service.
  * `ChatForegroundService`: Persistent notification service keeping the real-time connection alive.

---

## 8. Database Schema & Persistence (`Flyway V1`)

Managed automatically by Flyway (`server/src/main/resources/db/migration/V1__init.sql`):
* **`users`**: `id`, `name`, `email` (unique), `password_hash`, `last_seen`, `created_at`, `totp_secret`, `totp_enabled`
* **`user_keys`**: `user_id` (PK), `public_key`, `key_fingerprint`, `encrypted_private_key_backup`, `kdf_salt`, `kdf_iterations`
* **`refresh_tokens`**: `id`, `user_id`, `token_hash` (unique SHA-256), `expires_at`, `revoked_at`, `replaced_by`
* **`conversations`**: `id`, `type` (`DIRECT`/`GROUP`), `title`, `direct_key` (unique `"minId:maxId"`), `created_by`, `created_at`
* **`conversation_members`**: `(conversation_id, user_id)` PK, `role`, `joined_at`, `last_read_message_id`
* **`messages`**: `id`, `conversation_id`, `sender_id`, `client_message_id`, `type`, `ciphertext`, `iv`, `key_version`, `created_at`
* **`message_receipts`** & **`message_reactions`** & **`group_keys`**

In local development, data is saved permanently on disk inside:
```text
server/data/securechatdb.mv.db
```

---

## 9. Step-by-Step Guide: How to Run the Software

### Method 1: One-Click Live Runner (Recommended for Development)
Double-click **`run-live.bat`** in the project root (`c:\Users\Dipak.Rothe\Desktop\project-chatting application\secure-chat\run-live.bat`).
1. It checks if the backend server is listening on port `8090`. If not, it opens a console window and starts `mvn spring-boot:run`.
2. It asks if you want to open a public Pinggy tunnel (`Y/N`) for other PCs.
3. It compiles and launches the Desktop app directly from source code (`gradlew.bat :desktop:run`).
4. When you edit any code in `core/` or `desktop/`, close the app window and press `Enter` in the console to recompile and relaunch in 2 seconds.

---

### Method 2: Manual Terminal Startup

#### Step 1 — Start the Backend Server:
```powershell
cd "C:\Users\Dipak.Rothe\Desktop\project-chatting application\secure-chat\server"
mvn spring-boot:run
```
Wait until you see: `Tomcat started on port 8090 (http)`.

#### Step 2 — Run the Desktop App:
Either launch the installed **SecureChat** app from Windows Start Menu, or run from source:
```powershell
cd "C:\Users\Dipak.Rothe\Desktop\project-chatting application\secure-chat"
.\gradlew.bat :desktop:run
```

---

## 10. How to Connect & Chat Across Two Different PCs

### Scenario A: Using the Public HTTPS Tunnel (Works Across Any Network / Firewall)
1. Make sure the **Backend Server** is running on **PC 1** (port `8090`).
2. Open PowerShell on **PC 1** and start the tunnel over port 443:
   ```powershell
   ssh -p 443 -R 80:localhost:8090 a.pinggy.io
   ```
3. Copy the generated `https://xxxx.free.pinggy.net` URL shown in the terminal.
4. **On PC 1**:
   * Open SecureChat.
   * Keep **Server URL** as `http://localhost:8090`.
   * Sign in (e.g., `drothe619@gmail.com` / `password123`).
5. **On PC 2**:
   * Install and open `SecureChat-0.1.0.exe`.
   * In **Server URL**, paste the `https://xxxx.free.pinggy.net` URL.
   * Sign in (e.g., `snehavjaiswal12@gmail.com` / `password123`).
6. Click **`+ New Chat`**, search for `sneha` or `Dipak`, click **Start Chat**, and send messages back and forth in real time!

### Scenario B: Using Direct Local Network (Same Wi-Fi / Mobile Hotspot)
1. Connect both PC 1 and PC 2 to the same Wi-Fi or phone hotspot.
2. Find PC 1's IPv4 address using `ipconfig` (e.g., `10.10.104.98` or `192.168.43.x`).
3. On PC 2, enter `http://<PC1-IP>:8090` as the **Server URL**.

---

## 11. How to Build the `.exe` and `.apk` Installers

### 1. Build Windows Installer (`SecureChat-0.1.0.exe`)
```powershell
cd "C:\Users\Dipak.Rothe\Desktop\project-chatting application\secure-chat"
cmd.exe /c "scripts\package-windows.bat"
```
* **Output File:** `desktop\build\dist\SecureChat-0.1.0.exe` (~79.8 MB)

### 2. Build Android APK (`android-debug.apk`)
```powershell
cd "C:\Users\Dipak.Rothe\Desktop\project-chatting application\secure-chat"
$env:ANDROID_HOME = "$env:USERPROFILE\tools\android-sdk"
.\gradlew.bat :android:assembleDebug
```
* **Output File:** `android\build\outputs\apk\debug\android-debug.apk` (~7.0 MB)

---

## 12. Troubleshooting & Common Fixes

| Symptom / Error | Cause | Fix |
| :--- | :--- | :--- |
| `Login failed: Network error: unexpected end of stream` | Backend server on port `8090` is not running, or the Pinggy tunnel URL expired. | Start the backend server (`run-live.bat` or `mvn spring-boot:run`). On PC 1 use `http://localhost:8090`. On PC 2 use a fresh `ssh -p 443 -R 80:localhost:8090 a.pinggy.io` URL. |
| `ssh: connect to host a.pinggy.io port 22: Connection refused` | Network firewall blocks outbound SSH port 22. | Add `-p 443` so SSH connects over HTTPS port 443: `ssh -p 443 -R 80:localhost:8090 a.pinggy.io`. |
| `Search query must be 2-10 characters` | Search term is 1 character or >10 characters. | Enter a name or email prefix between 2 and 10 characters (e.g. `sneha` or `dipak`). |
| `Email already registered (409)` | Trying to click **Register** with an email that already has an account. | Switch to the **Sign In** tab and log in with that email and password. |
