# SecureChat — Manual Setup and Deployment Steps

This document lists steps for external services, accounts, and native tools that cannot be automated without user credentials.

---

## 1. WiX Toolset v3.11 Installation (for `.exe` Installer)

To package `SecureChat-0.1.0.exe` as a single-file Windows installer:
1. Download WiX Toolset v3.11 from: `https://wixtoolset.org/releases/v3.11/`
2. Run the installer or install via winget / chocolatey:
   ```cmd
   winget install --id WiXToolset.WiX -e
   ```
3. Ensure `candle.exe` and `light.exe` are in your PATH or under `C:\Program Files (x86)\WiX Toolset v3.11\bin`.
4. Run `scripts\package-windows.bat`. The script detects WiX and generates `desktop\build\dist\SecureChat-0.1.0.exe`.
5. *(Fallback)*: Without WiX, the script automatically produces a standalone Windows `app-image` containing `SecureChat.exe` and a bundled Java runtime in `desktop\build\dist\SecureChat\`.

---

## 2. Docker & Docker Compose Setup

To run MySQL 8 and Redis 7 via Docker:
1. Install Docker Desktop for Windows: `https://www.docker.com/products/docker-desktop/`
2. Start Docker Desktop and verify:
   ```powershell
   docker --version
   docker compose version
   ```
3. From the `secure-chat/` root, start the local development services:
   ```powershell
   docker compose up -d
   ```
4. To verify containers are running:
   ```powershell
   docker ps
   ```

---

## 3. Firebase Cloud Messaging (FCM) Setup for Android (Optional)

The Android app features a foreground WebSocket service and works independently of Firebase. To enable cloud push notifications when the app is completely terminated:
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a project named `SecureChat`.
3. Add an Android app with package name: `com.dipak.securechat`.
4. Download `google-services.json` and place it in `android/app/google-services.json`.
5. In Firebase Project Settings, create a Service Account and download the Admin SDK private key JSON.
6. Configure the server with the key path:
   ```yaml
   app:
     firebase:
       credentials-path: "/path/to/firebase-service-account.json"
   ```

---

## 4. Production TLS Certificates with Let's Encrypt

When deploying `deploy/docker-compose.prod.yml` behind Nginx:
1. Point your domain DNS (e.g. `chat.yourdomain.com`) to your server IP.
2. Obtain a free TLS certificate with Certbot:
   ```bash
   sudo certbot certonly --standalone -d chat.yourdomain.com
   ```
3. Copy or symlink the certificates to `deploy/ssl/cert.pem` and `deploy/ssl/key.pem`.
4. Start production cluster:
   ```bash
   docker compose -f deploy/docker-compose.prod.yml up -d
   ```
