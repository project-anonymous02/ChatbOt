# ChatbOt

A native Android chat client prototype for exploring a Google AI Mode-backed chat experience.

## Current status

- Jetpack Compose chat UI with a dark theme and vector icons.
- Image and document pickers.
- In-session attachment Library.
- Initial HTML response parser for rendered AI Mode snapshots.
- Parser unit tests for answer extraction, citation deduplication, Google redirect unwrapping, unsafe URL schemes, and completion-marker handling.

## Important limitations

The app does **not** currently connect to Google AI Mode or generate live AI answers. The parser is a standalone component awaiting a browser/session adapter. Google does not provide a public, stable AI Mode API for this integration, and browser DOM selectors can change without notice. A parsed answer is not considered complete unless a completion marker is detected; even that marker is a compatibility hint, not a universal guarantee.

The parser's selector strategy was informed by public projects:
- [MEOMcGill/aiscrape](https://github.com/MEOMcGill/aiscrape) — research reference only. Its PolyForm Noncommercial license means its source is not copied into this MIT-licensed project.
- [Dark2C/Google-AI-Mode-API-Wrapper](https://github.com/Dark2C/Google-AI-Mode-API-Wrapper) — selector and lifecycle design reference only; its selectors are not treated as verified current Google DOM.
- [MatrixAges/ai-mode-copy](https://github.com/MatrixAges/ai-mode-copy) — response extraction concept reference.

## Build

Open the project in Android Studio with JDK 17 and Android SDK 35, or run:

\`\`\`bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
\`\`\`

A successful CI build has not yet been established.
