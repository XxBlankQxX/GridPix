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

- 2026-10-06: skeleton scaffolded and building (commit a7f82fe); repo https://github.com/XxBlankQxX/GridPix (private).
- 2026-10-07: `game/` core done with 43 unit tests (commit c6fb8ab). Next: Room + DataStore in `data/`, then the puzzle screen (S4).
- Build note: JAVA_HOME must point at `C:\Program Files\Android\Android Studio\jbr` (now set as a user env var).
