# ChatbOt

A native Android chat client prototype with a Google AI Mode WebView integration.

## Implemented

- Jetpack Compose chat UI with a dark theme and vector icons.
- Image and document pickers plus an in-session attachment Library.
- HTML response parser with citation extraction and URL validation.
- Parser unit tests.
- Android WebView adapter that opens Google Search AI Mode (`udm=50`), polls rendered page content, and distinguishes confirmed answers from incomplete snapshots.
- Chat composer wired to the adapter, progress state, a visible Google sign-in browser surface, citation text, and coroutine cancellation via Stop.
- Android CI runs parser unit tests and builds a debug APK.

## Important limitations

Google does not provide a public, stable AI Mode API for this integration. The adapter is an unofficial UI integration and may stop working when Google's page structure changes. Its selectors are compatibility hints, not verified guarantees for every Google account, locale, or rollout. A timeout or missing completion marker is never treated as a completed answer.

The app uses a caller-owned Android WebView session and does not read, export, or request passwords or cookies. Google may restrict sign-in inside embedded WebViews on some flows; if that happens, this prototype cannot bypass that restriction. Complete sign-in only through Google's own supported interface.

Each submitted prompt currently opens a fresh `udm=50` search URL. Chat bubbles remain visible in the app, but Google-side multi-turn conversation continuity is **not implemented yet**. The app's selected image/file attachments are currently listed in the message but are **not uploaded to Google AI Mode**. Persistent chat history and persistent Library storage are also pending. The live DOM, answer extraction, and citation rendering still need testing on real Android devices and current Google AI Mode pages.

## Research references

- [MEOMcGill/aiscrape](https://github.com/MEOMcGill/aiscrape) — research reference only. Its PolyForm Noncommercial license means its source is not copied into this MIT-licensed project.
- [Dark2C/Google-AI-Mode-API-Wrapper](https://github.com/Dark2C/Google-AI-Mode-API-Wrapper) — selector and lifecycle design reference only; its selectors are not treated as verified current Google DOM.
- [MatrixAges/ai-mode-copy](https://github.com/MatrixAges/ai-mode-copy) — response extraction concept reference.

## Build

Open the project in Android Studio with JDK 17 and Android SDK 35, or run:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
