# Little Library

A cozy little Android app for keeping the reels, posts and shorts you want to come back to.

## How it works

1. In Instagram or YouTube, tap **Share** on anything and pick **Little Library**.
2. A small sheet slides up asking which shelf (tag) it belongs on.
   - As you type, tags you've already created are suggested (matches that start with your text come first, then the most-used).
   - With nothing typed, your most-used tags are shown as quick picks.
   - Add several tags: tap a suggestion, press Done, or type a comma.
   - Brand-new tags are created on the spot and remembered for next time.
3. Open the app to browse the library: tap a shelf (tag) to filter, search the catalogue, tap a card to open the original link, tap ✕ to remove it.

Sharing the same link twice doesn't duplicate it; the new tags are just added to the existing entry.

## Build & run

1. Open this folder in Android Studio (Ladybug or newer). It will sync Gradle on its own.
2. Plug in a phone (or start an emulator), press Run.
3. For a shareable file: Build > Build Bundle(s) / APK(s) > Build APK(s).

Requirements: Android 8.0 (API 26) or newer. No accounts, no internet permission, everything stays on the phone.

## Code map

- `ShareActivity.kt` — the Share-sheet pop-up with tag input and suggestions
- `MainActivity.kt` — the library (shelves + index cards)
- `LibraryDb.kt` — SQLite storage for items and tags
- `Models.kt` — link parsing from Share intents, tag normalisation and suggestion ranking
- `Components.kt`, `Theme.kt` — the book-spine chips, shelf plank, library-card look and colours
