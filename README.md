# ChatbOt

A native Android chat client prototype that uses Google AI Mode through an embedded WebView UI integration.

## Implemented

- Jetpack Compose dark chat UI with vector icons, message composer, attachment picker, and Stop action.
- Android WebView adapter that opens Google Search AI Mode (`udm=50`), polls rendered page content, and returns only parser-confirmed completion as a completed answer.
- Visible WebView surface when Google sign-in is required; the user handles Google sign-in directly.
- HTML answer parser, citation extraction, external URL validation, and parser unit tests.
- Bounded conversation-context builder: up to eight recent messages are included in a fresh request, with unit tests for limits and latest-prompt retention.
- Local SQLite storage for multiple conversations, messages, and Library metadata. Existing v1 transcript data is migrated into a legacy conversation.
- Document picker uses persistable URI grants where supported; Library deletion also releases unused grants.
- GitHub Actions runs unit tests and builds a debug APK.

## Important limitations

Google does not provide a public, stable AI Mode API for this integration. The adapter is an unofficial UI integration. DOM selectors are compatibility hints, not verified guarantees for every Google account, locale, or rollout; Google can change the page at any time. A timeout or missing completion marker is not treated as a completed answer.

The app uses its own Android WebView session and never reads or exports cookies or passwords. Google may block sign-in in embedded WebViews; this app does not bypass that restriction, CAPTCHA, access controls, or rate limits. Complete sign-in only through Google's supported interface.

Each prompt opens a fresh `udm=50` search URL. ChatbOt carries up to eight recent messages as bounded text context into the next request, which approximates continuity, but it does **not** continue the same native Google AI Mode conversation thread. Selected image/file attachments are currently stored in the app's Library and shown in the chat, but they are **not uploaded to Google AI Mode** yet. Streaming tokens, Google-side multi-turn session reuse, and live DOM compatibility still need real-device verification.

Conversation history and Library metadata are stored locally on the device; they are not synced or backed up to an account. Some document providers may not support persistable URI grants, so file access can expire.

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
