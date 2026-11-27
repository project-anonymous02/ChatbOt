# ChatbOt

A native Android chat client designed around Google AI Mode, with a ChatGPT-inspired conversation interface, attachments, and a personal Library.

> **Development status:** early UI foundation (0.1.0). Google AI Mode is not connected yet. The current app is a local UI prototype and does not send prompts to Google or generate AI answers.

## Current foundation

- Kotlin + Jetpack Compose + Material 3.
- Dark chat interface with a message composer.
- Navigation drawer with Chat, Library, and History destinations.
- Image and document pickers.
- Temporary in-memory attachment list.
- Vector Material icons; no emoji-based interface icons.
- Explicit UI status explaining that the AI adapter is not connected.

## Planned architecture

```text
Android app
  ├── Chat UI / Composer
  ├── Conversation & History store
  ├── Attachment picker / Library
  └── ChatRepository interface
          |
          v
Google AI Mode adapter (planned)
  ├── Browser session lifecycle
  ├── Prompt submission
  ├── Response completion detection
  ├── Answer + citation extraction
  └── Error / timeout / cancellation handling
```

The parser research references are:

- [MEOMcGill/aiscrape](https://github.com/MEOMcGill/aiscrape) — AI Mode DOM inspection, answer extraction, and citations. Its PolyForm Noncommercial license must be respected; this repository is a reference for research, not copied source.
- [Dark2C/Google-AI-Mode-API-Wrapper](https://github.com/Dark2C/Google-AI-Mode-API-Wrapper) — browser-driven chat lifecycle and completion detection. Its selectors are implementation-specific and must be revalidated.
- [MatrixAges/ai-mode-copy](https://github.com/MatrixAges/ai-mode-copy) — response extraction and HTML-to-Markdown presentation.

Google AI Mode has no official public API contract for this integration. Browser automation can break when Google changes its UI, and may encounter consent, sign-in, availability, or rate-limit restrictions. The implementation must use an authorized user session and must not bypass CAPTCHA or access controls.

## Build

Requirements:
- Android Studio with Android SDK 35.
- JDK 17.
- Gradle 8.10.2 or compatible Android Studio-managed Gradle installation.

Open this repository in Android Studio and sync the Gradle project. To build from a shell with Gradle installed:

```bash
gradle :app:assembleDebug
```

## Roadmap

- [x] Public repository and Android Compose project foundation.
- [x] Initial chat, attachment, Library, and History UI.
- [ ] Persistent conversations and Library storage.
- [ ] Chat repository abstraction and streaming state model.
- [ ] Google AI Mode browser adapter.
- [ ] Robust DOM probing and parser tests.
- [ ] Citation rendering and Markdown response renderer.
- [ ] Cancel/stop request lifecycle.
- [ ] Device and accessibility testing.

## License

MIT. See [LICENSE](LICENSE).
