# PEGASUS NEXUS — Kalıcı Güncelleme ve Gemini Live Entegrasyonu

## Değişmez Android kimliği
- Package/applicationId: `ai.pegasus.nexus.stable`.
- Stable v1: versionCode 500, versionName 1.0.0-stable.
- Production signing certificate SHA-256: `3e4fc38052b9dbd7bb9324158303817d4b2015315737e2983af49458a2d73afc`.
- Release signing uses protected GitHub Actions secrets, **never** randomized debug signing.
- Every later update uses the **same** package and signer, a strictly greater versionCode, and `:app:assembleRelease`.
- Before release: verify APK package, version, signature, and a second independently built incremental APK with matching certificate.
- Do not change app ID, delete existing app data, or remove/re-register AccessibilityService component without careful migration.
- Existing Android accessibility permission is ordinarily retained by in-place updates. Exceptions: service off, Android security changes, package/service identity changes, new feature permissions.
- Do not mix this project with PANDORA; separate isolated branch.

## Locked visual design
- PEGASUS NEXUS black-white monochrome, glossy central P orb, thin Jupiter/Saturn-like string rings, 4 bottom tabs (Ana ekran/Sohbet/Ajanlar/Ayarlar).
- No Notes and no appearance settings.
- Debug accessibility overlay is optional and hidden by default in mature releases, accessible only via settings; always have immediately available STOP controls.

## Gemini Live implementation order
1. Voice pipeline: manual opt-in start, runtime RECORD_AUDIO request, Bluetooth/headphone routing observation, foreground notification for active microphone sessions, clear stop and emergency controls. No ambient listening until permission and user enablement.
2. Secure Gemini authorization: mobile app does **not** bundle a long-lived API key. Authenticated backend provisions limited-lived Gemini Live ephemeral tokens. Tokens must be short-lived and constrained; when backend is not configured show honest 'Bağlı değil' state.
3. Use Gemini Live WebSocket v1beta. Audio input PCM 16-bit little-endian 16 kHz, output PCM 16-bit little-endian 24 kHz. Handle setup, server messages, playback, interruption, reconnect/session resumption, session limits, and redacted errors. Test actual API connection before claiming it works.
4. Action layer: model proposes structured, strictly allowlisted phone actions; on-device policy checks package/window fresh state; user confirms sensitive actions (message send, calls, purchases, transfers, delete); emergency STOP cancels actions and voice; no actions merely from model prose.
5. Live Android hardware test: selected headset, permissions, mic, screen scan, safe tap/swipe, pause/resume, STOP, and app restoration. No live trading or credential automation.
6. Batch completed features into a **single signed Stable update**, not separate installable packages; test upgrade over versionCode 500.

## Verified baseline
- GitHub Actions run 42431839736 compiled stable versionCode 500 and the 501 compatibility check with the same package and signing certificate.
- Current stable is a local Android screen control prototype; Gemini Live, mic recording, and backend are **not connected**.
- Official reference: https://ai.google.dev/gemini-api/docs/live-api
- Official ephemeral token design: https://ai.google.dev/gemini-api/docs/live-api/ephemeral-tokens

## Safety / release gate
- No API keys or keystore committed to source control.
- No voice or screen data sent without user-controlled permissions.
- Don't claim device behavior works from a CI build alone.
- Release APK is not published until unit/integration CI validation and an explicit device-test checklist are ready.
