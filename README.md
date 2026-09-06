# Sharky Home

Sharky Home is a remote-first Android TV / Fire TV launcher for Fire OS 8. It is a normal Android HOME app; Fire OS remains responsible for hardware, accounts, networking, and installed apps.

## What it does

- `HOME` intent filter, landscape TV layout and Fire TV remote / D-pad focus.
- Prominent Movies tile, configurable from **Apps → Sharky settings**. The default package is `com.sharky.movies` and is safely handled when missing.
- Direct shortcuts for Apps, Fire TV Settings, YouTube, Netflix, Prime Video and Spotify. Missing apps show an actionable message rather than crashing.
- Installed-app discovery, app icons, long-press Select for favourite toggling or App info, date/time, connection state and available private storage.
- Optional best-effort boot activity attempt. Modern Android/Fire OS may block background launches; it is off by default.

## Build

Open this folder in Android Studio, ensure Android SDK Platform 35 is installed, then run:

```powershell
.\gradlew.bat assembleDebug
```

The result is `app\build\outputs\apk\debug\app-debug.apk`.

GitHub publishes the latest successful build as a downloadable release APK.

## Install on Fire OS 8

1. On the Fire TV, enable **Developer Options** (select the device name in *My Fire TV > About* seven times if it is hidden), then enable **ADB debugging** and **Install unknown apps** as required by your sideload method.
2. Put the Fire TV and PC on the same network. From this folder: `adb connect FIRE_TV_IP:5555`, then `adb install -r app\build\outputs\apk\debug\app-debug.apk`.
3. Open Sharky Home once. Press Home and, if Fire OS shows a launcher chooser, select Sharky Home. If it does not offer the chooser, use the Home redirect method below.
4. In Sharky, open **Apps → Sharky settings** and set the exact package name for the existing movie app. Package names can be found with `adb shell pm list packages`.

## Reliable Home / boot behaviour on Fire OS 8

Amazon can restrict third-party launcher selection, so do not rely on the optional in-app boot attempt. Install and configure the current version of the open-source **Home on Fire** project separately, following its official documentation and releases. Set `com.sharky.home` / Sharky Home as its target launcher, enable its Home-button redirect, and use its documented boot option if offered for the device firmware. This project does not ship, modify, or conceal any redirect utility.

Expected experience: Fire TV boot screen → Home on Fire redirect → Sharky Home; physical Home → Sharky Home. Test this after every Fire OS update, because Amazon can change launcher behaviour.

## Controls

- Direction pad: move between tiles and apps.
- Select: open tile/app.
- Long-press Select on an installed app: favourite toggle or app info.
- Apps tile: complete installed-app list plus Sharky settings.

## Privacy

Sharky Home has no account, tracking, advertisements, network calls, or media catalogue. `QUERY_ALL_PACKAGES` is solely used to build the installed-app launcher list; Fire TV requires broad visibility for launcher functionality.
