# CLAUDE.md — GridPix

## Project

- App: GridPix — offline nonogram (picture-logic) puzzle game. Infinite generated puzzles + hand-made picture packs. No ads; in-app purchases only. Audience "Everyone" (kid-friendly, not Families-designated).
- Spec (source of truth, read before any feature work): `C:\Users\chris\OneDrive\160_STUDIO - Documents\AppDev\Apps\GridPix\SPEC.md`
- Shared knowledge base + decisions: `C:\Users\chris\OneDrive\160_STUDIO - Documents\AppDev\` — read `00_START_HERE.md` and `01_Decisions.md` once per session. Billing notes: `03_Research\Monetisation_AdMob_and_Play_Billing.md` (ignore the AdMob parts — this app has no ads).
- Sister project for reference patterns (Hilt/Room/DataStore/Billing already working): `C:\dev\Apps\LogbookZA\` — reuse its billing and DataStore approach where it fits; do not copy blindly.
- Owner: Christopher (bookkeeper) — directs, does not write code. Explain *why* briefly, with real file names and values. Direct tone, no padding, no praise.
- Developer name: Blanks Studio. Package / applicationId: `com.blanksstudio.gridpix`. App name "GridPix".
- NEVER use the word "Picross" anywhere (Nintendo trademark). Say "nonogram".

## Stack (do not change without asking)

Kotlin · Jetpack Compose · Material 3 · Hilt · Room · DataStore (Preferences) · Navigation Compose · Kotlin DSL Gradle + version catalog.
`compileSdk 37`, `targetSdk 36`, `minSdk 26`. Single module `:app`. Folders: `ui/` (screens + ViewModels), `game/` (pure Kotlin: grid model, clue computation, generator, line solver, hint logic — NO Android imports), `data/` (Room, DataStore, pack loader), `billing/`, `di/`.
No network except Play Billing. No ads SDK. No Firebase.

## Rules

- `game/` is pure Kotlin and fully unit-tested: clue computation, solver (must finish without guessing), generator (seeded, reproducible), hint selection, win detection. Every pack JSON in `assets/packs/` is loaded by a unit test and must pass the solver.
- Build after every change: `gradlew.bat assembleDebug`. Run `gradlew.bat testDebugUnitTest` after any `game/` or data change. Never report "done" without a green build; paste the tail of the output.
- User-facing text in `res/values/strings.xml`. No hard-coded strings in Compose.
- Room schema change ⇒ bump version + `Migration`. Never `fallbackToDestructiveMigration` in release.
- Puzzle progress auto-saves on every move; process death mid-puzzle must lose nothing.
- No new dependencies without saying why first. Compose `Canvas` for the grid; no game engine.
- Ask before: deleting files, changing package name, changing schema, touching signing/keystores, adding permissions.
- Product IDs are fixed by the spec §6; never invent new ones. Billing keys only via `local.properties` → `BuildConfig`; never commit them.
- Commit after each working step with conventional messages (`feat:`, `fix:`, `chore:`, `test:`). **No Co-Authored-By or AI attribution trailers.**
- One feature per session. If a task grows beyond the prompt, stop and say so.

## Current status

- 2026-10-07 (evening): **v1.2.0** (build 5): daily reminder (WorkManager, opt-in), Play review prompt, auto-cross, share picture (with off switch), level-up hints, seasonal Halloween/Christmas packs, es/pt/fr/de translations (`res/values-xx`, `assets/packs/i18n/xx.json`, TranslationsTest keeps them complete). 72 unit tests.
- 2026-10-07 (later): **v1.1.0 redesign** (build 4): colour reveal, Anime pack (`pack_anime`), level/XP, badges, streak calendar, Collection, new icon. 62 unit tests. Pack JSON now has `palette` + `colors` per puzzle (validated by the tool and PackContentTest).
- 2026-10-07: **v1.0.0 code complete.** All screens (S1-S8 + tutorial), Room + DataStore, Play Billing, 5 packs x 30 puzzles, 54 unit tests, signed release bundle with R8, smoke-tested on the API 37 emulator. Repo https://github.com/XxBlankQxX/GridPix (public, so GitHub Pages can host docs/privacy.html).
- Remaining work is Play Console only; steps in OneDrive `Apps/GridPix/Play_Console_Checklist.md`; store text in `Store_Listing.md`; screenshots and graphics in `Apps/GridPix/screenshots/` and `graphics/`.
- Release signing: `keystore.properties` (gitignored) -> `C:\dev\Apps\keys\gridpix-upload.jks`. Build with `gradlew.bat bundleRelease`.
- Pack art validator: `python tools/nonogram_check.py app/src/main/assets/packs/<id>.json` (same algorithm as `game/LineSolver.kt`).
- Build note: JAVA_HOME must point at `C:\Program Files\Android\Android Studio\jbr` (set as a user env var 2026-10-07).
- Emulator `Medium_Phone_API_37.0` exists; adb install/input/screencap works for UI smoke tests.
