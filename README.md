# SavePetti

Save anything from anywhere — find it instantly later.

A calm, colorful, offline-first Android app that lives in the share sheet.

## Build

You need:
- **Android Studio Hedgehog (2023.1.1)** or newer
- **JDK 17**
- **Android SDK 34**

Open the `SavePetti` folder in Android Studio. Let it sync Gradle, then run on a device or emulator (`minSdk 24`).

Or from a shell with the wrapper:

```
gradlew :app:assembleDebug
```

(You'll need to add the actual `gradlew` / `gradlew.bat` scripts and the `gradle-wrapper.jar` from any Android Studio project — they're not included.)

## What's where

| Layer       | Folder                                   |
|-------------|------------------------------------------|
| Theme       | `app/src/main/java/com/savepetti/ui/theme/` |
| Components  | `app/src/main/java/com/savepetti/ui/components/` |
| Screens     | `app/src/main/java/com/savepetti/ui/screens/` |
| Database    | `app/src/main/java/com/savepetti/data/local/` |
| OCR worker  | `app/src/main/java/com/savepetti/data/ocr/`  |
| Share entry | `app/src/main/java/com/savepetti/ui/screens/save/ShareReceiverActivity.kt` |

## Stack

Kotlin · Jetpack Compose · Material 3 · Room (FTS4) · Hilt · WorkManager · ML Kit text recognition · Coil · Jsoup
