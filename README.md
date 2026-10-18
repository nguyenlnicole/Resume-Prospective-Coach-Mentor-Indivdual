# PNB Resume Prospective

Coach & Mentor and Individual editions for reviewing résumés, building stronger descriptions, and preparing cover letters.

## Features

- Technical, General, Medical, and Educator résumé workflows.
- Format and content ratings, detailed suggestions, positive feedback, and optional playful roasts.
- Optional job-description matching from pasted text, a file, or a URL.
- Résumé comparisons, PDF and Word downloads, cover letters, and interview preparation.
- Coach review history, saved progress, individual accounts, and access expiration.
- Android welcome animation and edition selection, plus an Electron desktop client.

## Repository contents

`frontend/` contains the React interface. `backend/` contains the Java service, account controls, AI integration, document generation, and tests. `desktop/` packages the interface with Electron. `android/` contains the native Android launcher and WebView client.

This is a fresh, sanitized source release. Private configuration, account databases, uploaded documents, original résumé PDFs and screenshots, deployment credentials, signing keys, and previous Git history are excluded. Demonstration identities and contact information are fictional. No permanent accounts are seeded by this release.

The owner administration interface is excluded. Protected backend administration endpoints remain because they manage approvals, subscriptions, and revocation. They require a configured owner account and authenticated access.

## Run locally

Requirements: Node.js 22 or newer, Java 21, and Maven 3.9 or newer.

From `backend/`, run:

```sh
mvn spring-boot:run
```

From `frontend/`, run:

```sh
npm ci
npm run dev
```

Open `http://127.0.0.1:5173/` for Coach & Mentor. Use `npm run dev:individual` for Individual. The interface forwards `/api` requests to the local backend on port 8080.

To explore without signing in or contacting the AI and email services, open `http://127.0.0.1:5173/?walkthrough=sample`. This demonstration uses fictional data and illustrative feedback; email and account changes are disabled in the demonstration.

## Service configuration

Set server environment variables locally or in your hosting provider. Never put real credentials in tracked files. `backend/service.env.example` lists the available configuration. Java does not automatically load that example file; export the values into the process environment or use an ignored `backend/.local.properties` file with Spring property names.

Configure `ADMIN_EMAIL` for the owner, `REVIEW_EMAIL` for help requests, `PUBLIC_URL` for emailed links, and your email provider before using real sign-in and delivery. Live AI features require `OPENAI_API_KEY` and `OPENAI_MODEL`. Email is disabled by default. Set a private database password and `COOKIE_SECURE=true` for HTTPS deployments.

Coach & Mentor uses the internal edition key `coach`; Individual retains the existing service-compatible key `beta`. Published PNB addresses remain in the Android origin allowlist and host-routing logic. For your own deployment, update the configured addresses consistently in Android `EditionPolicy.java`, frontend edition detection, and backend `SecurityConfig.java`. Do not disable origin validation.

The original owner dashboard and operational deployment setup are not included. Configure and manage owner access through your private administration deployment or the protected service endpoints.

## Build and check

```sh
cd frontend
npm test
npm run build:coach
npm run build:individual
```

```sh
cd backend
mvn test
mvn package
```

The Dockerfile builds Coach & Mentor by default. Use `--build-arg APP_EDITION=beta` for Individual. Supply secrets only at runtime, never as Docker build arguments.

## Desktop

Install dependencies in `desktop/` with `npm ci`. Build the matching frontend first. Set `PNB_EDITION` to `coach` or `beta`, and set `PNB_API_ORIGIN` to the service's HTTPS origin. Run `npm run prepare-ui`, then `npm start`. For local preview only, set `PNB_PREVIEW_BUILD=1` and use `http://127.0.0.1:8080` as the API origin.

Run `node build-edition.cjs coach` or `node build-edition.cjs beta` to package a Windows edition. Signing and distribution credentials are not included.

## Android

Open `android/` in Android Studio with Android SDK 36 and Gradle 8.13, or run `gradle :app:assembleDebug` there. Configure your SDK location in an ignored `local.properties` file. The app loads the hosted editions; the debug build also supports local preview. For an emulator, forward port 5173 with `adb reverse tcp:5173 tcp:5173` while the local frontend is running.

The same robot appears in the launcher and five-second welcome animation. Release signing keys and installed application packages are not included.

## Notices

This public source release does not grant a license to reuse PNB Paws branding or assets. Third-party components retain their respective licenses. The bundled script font's license is preserved in `android/app/src/main/assets/welcome/OFL.txt`. Source comments and commented-out code have been removed; license documents are retained.
