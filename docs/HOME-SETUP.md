# Home button and power-on setup

Sharky registers as a HOME and Leanback launcher. Fire OS 8 still routes the physical Home key to Amazon. This cannot be changed by Sharky's normal app permission alone.

Use the separate [Home on Fire](https://github.com/toolicious/home-on-fire) companion. Its current documentation supports Fire OS 8 and includes startup/wake redirection; firmware-specific testing is still necessary.

1. In Downloader install the [official Home on Fire APK](https://github.com/toolicious/home-on-fire/releases/latest/download/home-on-fire.apk).
2. Open Sharky > Settings > Home button & startup > Open Home on Fire.
3. Enable its accessibility service, choose **Sharky Home** as target, and enable **Launch on boot**.
4. Test Home from a different app, then restart and test waking from standby.

If Fire TV's Accessibility settings show no service toggle, a one-time ADB permission grant is necessary. Enable ADB debugging on the TV and run `setup-home-redirect.bat` on the PC. It downloads Google's platform tools, asks for your TV's IP, checks the connection, and grants the already-installed companion `WRITE_SECURE_SETTINGS` so its own accessibility switch works. It does not disable Amazon's launcher. Accept the TV's debugging prompt, then finish the target and boot choices on the TV.

The helper may briefly show Amazon's screen before redirecting. Use its long-press Home escape to return to Amazon; disable its redirect/service to undo the setup. An actual restart differs from merely powering off the television.

# Updating Sharky

Published builds now use a permanent signing key held privately in a GitHub Actions secret. The excluded `.signing/sharky.jks` is a local backup: keep it safe and never publish it. Losing the signing key prevents compatible updates.

Earlier builds used automatically generated debug certificates. If Android refuses the first new update with an "App not installed" message, one uninstall/reinstall may be necessary; that removes Sharky's old preferences. Later builds use the same permanent key and install over one another. Sharky does not yet include an automatic updater.
