# Layzee

Move to the next Reels, TikTok, or Instagram video without touching your phone. Just **pinch** (thumb meets index finger) in front of the front camera, and Layzee sends one swipe up to the next page.

Layzee is a light, precise "air trackpad" for Android, built for a phone propped on a stand while you watch a feed.

## Install

1. Download the APK for your phone from the [latest release](https://github.com/rahmanfaisal653/Layzee/releases/latest):
   - `app-arm64-v8a-release.apk` for almost every phone from the last few years.
   - `app-armeabi-v7a-release.apk` for older 32-bit phones.
2. Open the file on your phone and allow installs from that source when Android asks.
3. Open Layzee and follow the setup guide in the **Dashboard**.

### If Play Protect blocks the install

Layzee is not on Google Play yet. In some countries (Indonesia among them), Play Protect blocks APKs installed from a browser, chat app, or file manager when they ask for the Accessibility Service. Layzee needs it only to send the swipe; it does not read screen content, and the full source code is in this repository if you want to check.

To install anyway:

1. Open the **Play Store**, tap your profile picture, then **Play Protect** → ⚙️ (settings).
2. Turn off **Scan apps with Play Protect**.
3. Install the Layzee APK.
4. Turn **Scan apps with Play Protect** back on.

Or install it from a computer over USB debugging:

```bash
adb install -r app-arm64-v8a-release.apk
```

### If the Accessibility switch is greyed out

On Android 13 and newer, sideloaded apps start with restricted settings. Open **Settings → Apps → Layzee**, tap ⋮ (top right), choose **Allow restricted settings**, then go back to the Dashboard and turn on the Accessibility Service.

## How to use

1. Open the **Dashboard**, grant the permissions in the setup guide, then tap **Start**.
2. Prop your phone on a stand and open your favorite feed app.
3. Pinch in front of the front camera: one pinch, one swipe up.

A floating **overlay** icon sits on top of other apps, and its color and shape show the current status. Tap it for a small menu: **Stop** or **Buka dashboard** (open dashboard).

## Features

- **Pinch = swipe.** As soon as a pinch is detected, Layzee sends one quick flick up from the middle of the screen. Holding the pinch does not repeat it, and hand movement is ignored.
- **Works in any app** in the foreground, with no allowlist.
- **Reliable detection.** The thumb-to-index distance is measured against hand size, so it does not depend on how far your hand is from the camera. False pinches are deliberately harder to trigger than missed ones, because an accidental swipe skips a video.
- **Light on the phone.** Low analysis resolution, an adaptive frame rate (low with no hand in view, higher once a hand appears), and only one hand is tracked. Built for low-end and mid-range phones.
- **Pinch test** screen in the Dashboard: camera preview with hand landmarks, a live status label, and a list you can scroll by pinching.
- **Dark and light theme** that follows the system.

## Status

| Status | Meaning |
| --- | --- |
| **Off** | Layzee is stopped: the camera is not used and the overlay is hidden. |
| **Idle** | Layzee is running, but no hand is in view. |
| **Tracking** | A hand is in view, no pinch yet. |
| **Pinch** | A pinch was detected and a swipe was sent. |

Layzee never turns itself off: only you can stop it (Stop from the overlay or the Dashboard). When the screen turns off, the camera is released, and it comes back automatically when the screen turns on.

## Permissions

| Permission | Why |
| --- | --- |
| **Accessibility Service** | Sends the swipe gesture to other apps. Used only for that swipe; it does not read screen content. |
| **Camera** | Reads the pinch through the front camera. |
| **Display over other apps** | Shows the overlay icon. |
| **Notifications** | The foreground service notification that keeps the camera running. |
| **Battery optimization / autostart exception** | Stops the system from killing Layzee in the background (important on phones with aggressive battery management). Checked off manually from the Dashboard. |

## Privacy

- Every image is processed **on your phone**. No image is stored or sent anywhere.
- No account, no cloud, no analytics, and no internet permission.

## Requirements

- Android 8.0 (API 26) or newer.
- A phone with a front camera. Stand mode (phone propped up) is the first target; handheld mode comes later.

## Build from source

Requires JDK 17 and the Android SDK (compile SDK 36).

```bash
./gradlew :app:assembleRelease
```

The APKs (one per ABI) land in `app/build/outputs/apk/release/`.

Release builds are signed with the key described in a `keystore.properties` file at the project root (not committed):

```properties
storeFile=/path/to/layzee-release.jks
storePassword=...
keyAlias=layzee
keyPassword=...
```

Without that file, release builds fall back to the debug key. Run the unit tests with `./gradlew :app:testDebugUnitTest`.

## Tech

Native Kotlin with CameraX and MediaPipe Hand Landmarker (CPU), classic Views without Compose, and as few dependencies as possible. R8 shrinking and ABI splits keep the APK small.

## Project status

Version `0.1.0`, an MVP distributed as a direct APK download (sideload). The app interface is currently in Indonesian.
