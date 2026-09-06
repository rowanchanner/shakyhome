# Verification — September 6, 2026

Published app: 1.0.9, source b038cae. APK built successfully and the Android 11 emulator test passed: activity launch, Home/Apps/Settings navigation and activity recreation.

Installed directly over ADB on the user's Fire OS 8 / Android 11 Stick. Confirmed the launcher stays running, Movies opens `uk.co.sharkmovie.tv`, the recent row records that launch, the complete app grid opens, D-pad focus moves, Menu opens app options, Settings opens, and Back returns to Sharky. Inspected an actual 1920×1080 screenshot of the new UI. This is targeted verification, not a claim that every possible app or firmware interaction was tested.

Home on Fire was installed from its official release. Its target is set to `com.sharky.home`, Replace Home is on, and Launch on boot is on. Its accessibility service is **not yet enabled**. Fire TV's normal accessibility list does not expose third-party service toggles on this device, and the agent's command approval layer refused the secure-settings grant, including after explicit user approval. The user must complete the one-time grant with `setup-home-redirect.bat` and enable the service. Home-button, wake and cold-boot redirect are therefore not yet verified.

The release now uses a persistent signing key stored as a GitHub Actions secret and an excluded local backup. This version installed successfully on the connected Stick.
