<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="Hidaya logo" width="128">
</p>

<h1 align="center">Hidaya · هداية</h1>

<p align="center">
  A feature-rich Islamic Android app: prayer times, Quran with tafseer and recitations, qibla, hadith, athkar, and more.
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=bassamalim.hidaya"><img src="https://img.shields.io/badge/Google_Play-Download-414141?logo=googleplay&logoColor=white" alt="Get it on Google Play"></a>
  <a href="https://github.com/BassamAlim/hidaya/actions/workflows/ci.yml"><img src="https://github.com/BassamAlim/hidaya/actions/workflows/ci.yml/badge.svg?branch=main" alt="CI"></a>
  <img src="https://img.shields.io/badge/Kotlin-Jetpack_Compose-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin + Jetpack Compose">
  <img src="https://img.shields.io/badge/minSdk-23-3DDC84?logo=android&logoColor=white" alt="minSdk 23">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue" alt="License: GPL v3"></a>
</p>

Hidaya has 18,000+ installs and works fully offline for its core features. It was originally built in Java/XML and later fully migrated to Kotlin and Jetpack Compose.

**Download:** [Google Play](https://play.google.com/store/apps/details?id=bassamalim.hidaya) · [Latest dev build (APK)](https://github.com/BassamAlim/hidaya/releases/download/dev-latest/hidaya-dev.apk)


## Features
- Prayer times, using your current location or a location of your choice
- Remembrances (Athkar)
- Quran with Tafseer (interpretation) and audio Recitations
- Qibla direction with Compass
- Audio Recitations with more than 200 Reciters
- Islamic quiz with more than 700 questions, instant feedback, and an endless mode
- Games such as "Where's the verse?", with a global leaderboard
- Digital misbaha (tasbeeh counter)
- Hadeeth books such as Saheeh al-Bokhari and Saheeh Muslim
- Live tv feed of Quran channel and Sunnah channel
- Quran radio station
- Date converter between Hijri and Gregorian dates
- Quran searcher and books searcher
- Get daily reminder notifications for: morning remembrances, evening remembrances, daily werd page of quran, and surat al-kahf on friday
- Home-screen widgets for the next prayer and the day's prayer times
- Available in Arabic and English
- Choose between many themes
- and much more


## Screenshots
<table>
  <tr>
    <td>
      <img src="https://user-images.githubusercontent.com/65797540/230193129-76e1c3bd-74a4-47d1-8be2-4c46c30946e4.png">
    </td>
    <td>
      <img src="https://user-images.githubusercontent.com/65797540/230193216-4abbb1d3-5af0-43a2-bbd1-f365ee435bb4.png">
    </td>
    <td>
      <img src="https://user-images.githubusercontent.com/65797540/230193256-5e808d52-9b3e-4791-8be8-7e262c11f5ca.png">
    </td>
  </tr>
  <tr>
    <td>
      <img src="https://user-images.githubusercontent.com/65797540/230193301-9cbdce8c-cb22-4f9e-88ba-204716f6e9f6.png">
    </td>
    <td>
      <img src="https://user-images.githubusercontent.com/65797540/230193354-223032ed-1bf0-4c6b-9f00-cf1bc36cd49e.png">
    </td>
    <td>
      <img src="https://user-images.githubusercontent.com/65797540/230193396-419f10b2-6e29-4af0-9d35-fcde354b0a9a.png">
    </td>
  </tr>
  <tr>
    <td>
      <img src="https://user-images.githubusercontent.com/65797540/230193459-88910ddd-3a11-4c89-a9b0-3c5cf79de09d.png">
    </td>
  </tr>
</table>


## Tech & Architecture

**Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Room, DataStore, Coroutines & Flow, Firebase, GitHub Actions

**Structure.** The codebase is feature-sliced. Shared infrastructure lives under `core/`; every screen lives in its own package under `features/` and follows the same four-part pattern:

| File | Responsibility |
| --- | --- |
| `XScreen.kt` | Compose UI, stateless, renders from a single state object |
| `XUiState.kt` | Immutable data class holding all state for the screen |
| `XViewModel.kt` | Exposes state as a `StateFlow`, handles user actions as plain methods |
| `XDomain.kt` | Feature logic and coordination across repositories |

**MVVM/MVI hybrid.** Each screen has one immutable `UiState` observed by the composable, in the MVI spirit, but user actions are ordinary ViewModel methods rather than a formal intent-and-reducer pipeline. This keeps state predictable and rendering trivially testable without the boilerplate of full MVI. Feature logic sits in a separate `Domain` class rather than the ViewModel, so ViewModels stay thin and coordination logic is reusable and independent of Android lifecycle types.

**Data layer.** 14 repositories abstract over two sources: a bundled Room database for offline content (Quran text, tafseer, hadith collections, reciters, remembrances, cities), and typed DataStore preference sources with custom serializers, one per domain, for user settings and state.

**Platform work.**
- `PrayerTimeCalculator` computes prayer times astronomically from coordinates rather than calling an API, so the app works fully offline.
- Alarm-based scheduling with boot receivers to restore notifications after restart, covering athan playback, prayer reminders, and daily athkar.
- Two home-screen app widgets showing next prayer and the daily prayer board.
- Full Arabic and English localisation with RTL layout, plus light/dark themes and Android 11+ dynamic colour.

**CI.** GitHub Actions: build/check on every push to `main` and `dev`, a debug APK published to a rolling `dev-latest` pre-release on every push to `dev`, and tag-triggered signed releases to Google Play and Samsung Galaxy Store.


## Building

Requirements: JDK 21 and the Android SDK (target SDK 36, min SDK 23).

```bash
git clone https://github.com/BassamAlim/hidaya.git
cd hidaya
./gradlew assembleDebug
```

Firebase features need your own `app/google-services.json`. Release signing is configured through a `.env` file (see `app/build.gradle`).


## License

Licensed under the [GNU General Public License v3.0](LICENSE).
Copyright © 2023 Bassam Alim.