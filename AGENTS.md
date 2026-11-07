# NotesAssistant

Android notes + checklist app (Kotlin, Jetpack Compose, Room, Google Drive app-data sync, biometric-locked notes).

## Build & test
- Same as CI (`.github/workflows/ci.yml`, JDK 17): `./gradlew lintDebug testDebugUnitTest`, then `./gradlew assembleDebug`.
- Builds without `local.properties`: AdMob falls back to Google test IDs, release stays unsigned (warning in the log).

## Layout
- Single module `app/`, package `com.gokcank.notesassistant`, MVVM.
- `data/` Room (`notes`, `checklist_items`), DataStore settings, `DriveSyncManager`, `BackupManager` (JSON export/import).
- `smart/DateTimeExtractor.kt` — natural-language date detection ("tomorrow 3pm", "yarın 15:00"); unit-tested.
- `ui/MarkdownLite.kt` — lightweight `**bold**` / `*italic*` / `# heading` markup; unit-tested.
- `AdsConfig.kt` + `ui/AdBanner.kt` — banner ad; ID comes from `BuildConfig`.

## Project rules
- Drive sync must only use the app-private `drive.appdata` scope; merge is last-edit-wins.
- Locked (private) notes hide their preview and are excluded from search; keep it that way.
- `local.properties` keys: `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`,
  `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Keystore in `keystore/` (git-ignored).
- README is bilingual (EN + TR); keep both halves in sync.

## Status (as of 2026-11-07)
- v1.0.0 (versionCode 1). Last change 2026-08-30 (CI + lint fixes). Not on Play as of 2026-07-12. Overall status unknown.

## Open items
- Release plan (Play) undecided.

## More context
Background, decisions and history are in the second brain: `/home/gokcank/ikinci-beyin/projects/notesassistant.md` (read on demand).
