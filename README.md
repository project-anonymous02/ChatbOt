# ChatbOt

A native Android chat client prototype for exploring a Google AI Mode-backed chat experience.

## Current status

- Jetpack Compose chat UI with a dark theme and vector icons.
- Image and document pickers.
- In-session attachment Library.
- HTML response parser with citation extraction and URL validation.
- Parser unit tests.
- Initial WebView adapter that opens Google AI Mode using the user's normal Android WebView session, polls rendered HTML, and returns an incomplete result if a completion marker is not detected.

## Important limitations

Google does not provide a public, stable AI Mode API for this integration. The adapter is an unofficial UI integration and may stop working when Google's page structure changes. It requires a caller-owned, attached WebView and a user-authorized Google session. It does not extract, export, or request cookies or passwords. If Google displays a sign-in or verification screen, the user must handle it directly in the browser. The adapter does not bypass CAPTCHA, access controls, or rate limits.

The current Compose chat screen is not yet wired to this adapter, and persistent chat history / persistent Library are still pending. A complete live conversation flow, attachment upload to Google, and reliable streaming are not yet implemented.

The parser's selector strategy was informed by public projects:
- [MEOMcGill/aiscrape](https://github.com/MEOMcGill/aiscrape) — research reference only. Its PolyForm Noncommercial license means its source is not copied into this MIT-licensed project.
- [Dark2C/Google-AI-Mode-API-Wrapper](https://github.com/Dark2C/Google-AI-Mode-API-Wrapper) — selector and lifecycle design reference only; its selectors are not treated as verified current Google DOM.
- [MatrixAges/ai-mode-copy](https://github.com/MatrixAges/ai-mode-copy) — response extraction concept reference.

## Build

Open the project in Android Studio with JDK 17 and Android SDK 35, or run:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
