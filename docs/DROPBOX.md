# Sharky DropBox

A separate Fire TV APK (`com.sharky.dropbox`) and a PC sender window. The Fire TV hosts the PC interface locally; no cloud account or Dropbox service is involved.

## Use

1. Open **Sharky DropBox** in Sharky Home's first row on the TV.
2. Open the **Sharky DropBox** shortcut on the PC. The provided launcher opens an Edge app window at `http://192.168.0.39:8787`; a regular browser also works.
3. Enter the six-digit pairing code shown on the TV, paste a direct HTTPS APK link, and select **Send to Fire Stick**.
4. The Fire TV downloads the APK. On first use, allow **Sharky DropBox** under Fire TV's **Install unknown apps** setting; then select **Install downloaded APK** if the installer does not reopen automatically.
5. Confirm the Android installation prompt on the TV. Installing is never silent.

Keep the receiver open. Moving to the installer or another app closes the listening server; returning to DropBox opens it again. If the Fire TV's IP changes, use the new address displayed on TV or update the PC `.bat` file. Both devices must be on the same private network, without guest Wi-Fi isolation.

## Behaviour and limits

- Exact duplicated links are normalized to a single link.
- Direct HTTPS links only. Redirects stay HTTPS and are capped at eight hops.
- One download at a time, cancellable on TV, up to 1 GB and within available space.
- APKs are checked with Android's package parser before an installation prompt opens. HTML error pages and split `.apks` bundles are rejected.
- APKs stay in the app's private downloads folder; only the current validated APK is retained. A previous APK is replaced only after a new download validates successfully.
- Failed, cancelled or partial downloads are cleaned up. No broad storage permission is requested.
- The local control page uses HTTP on private Wi-Fi; downloaded APKs use HTTPS. A session pairing code, request-size limits, rate limiting, same-origin checks and numeric Host validation protect the control endpoint. Never port-forward it onto the internet.
- The PC window is a browser app served by the Fire TV, not a separate Windows installer.

## Build

`gradlew.bat :dropbox:assembleDebug :dropbox:testDebugUnitTest`

APK: `dropbox/build/outputs/apk/debug/dropbox-debug.apk`.
GitHub release asset: `sharky-dropbox.apk`.
Both Sharky APKs use the same permanent private signing key and increasing build version numbers.
