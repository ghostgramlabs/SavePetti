# PettiBox

Save anything from anywhere, then find it instantly later.

A calm, colorful, offline-first Android app that lives in the Android share sheet.

## Build

You need:
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

Open this project folder in Android Studio. Let it sync Gradle, then run on a device or emulator (`minSdk 24`).

Or from a shell with the wrapper:

```bash
gradlew :app:assembleDebug
```

If the wrapper scripts are missing locally, use Android Studio's Gradle integration or restore `gradlew` / `gradlew.bat` and `gradle-wrapper.jar`.

## What's Where

| Layer       | Folder |
|-------------|--------|
| Theme       | `app/src/main/java/com/ghostgramlabs/pettibox/ui/theme/` |
| Components  | `app/src/main/java/com/ghostgramlabs/pettibox/ui/components/` |
| Screens     | `app/src/main/java/com/ghostgramlabs/pettibox/ui/screens/` |
| Database    | `app/src/main/java/com/ghostgramlabs/pettibox/data/local/` |
| OCR worker  | `app/src/main/java/com/ghostgramlabs/pettibox/data/ocr/` |
| Share entry | `app/src/main/java/com/ghostgramlabs/pettibox/ui/screens/save/ShareReceiverActivity.kt` |

## Stack

Kotlin, Jetpack Compose, Material 3, Room with FTS4, Hilt, WorkManager, ML Kit text recognition, Coil, and Jsoup.
