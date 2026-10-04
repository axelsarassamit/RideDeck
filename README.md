# GEARELEC GX12 Companion

An independent Android companion for the GEARELEC GX12 motorcycle headset. The first version checks whether Android has the GX12 in its paired-device list and checks GitHub Releases for app updates.

This project is not affiliated with GEARELEC. The phone's Bluetooth diagnostics show standard A2DP, AVRCP, HFP, and PBAP services, but no documented GX12-specific control service. The app therefore does not claim to change headset settings, read its battery, or control intercom pairing.

## Install

Download `gx12-companion-release.apk` from the [latest release](https://github.com/axelsarassamit/gearelec-gx12-companion/releases/latest). Android will show the normal install confirmation. The initial public release is a sideload, so Android may ask you to allow installs from the app or browser you used to download it.

## In-app updates

Use **Check for updates** in the app. It checks the public GitHub Releases API, downloads the APK and `checksums.txt` over HTTPS, verifies the APK's SHA-256, and opens Android's package installer. Android asks you to approve each installation; the app cannot silently replace itself. Updates are signed with one stable private key so Android can confirm that a release belongs to this app.

New versions are published by pushing a tag such as `v0.1.1`. The GitHub Actions release workflow builds and signs the APK and attaches it and its checksum to a public GitHub Release.

## Release signing setup

The release workflow intentionally fails if signing secrets are missing. Never commit the signing key or its passwords. Configure these repository Actions secrets before publishing a tag:

- `GX12_KEYSTORE_BASE64`: base64 encoding of the release `.jks` file
- `GX12_KEYSTORE_PASSWORD`: keystore password
- `GX12_KEY_ALIAS`: key alias
- `GX12_KEY_PASSWORD`: key password

Keep an offline backup of the keystore and passwords. If the signing key is lost, Android will reject an in-place update; users would have to uninstall and reinstall, losing app data.

## Build locally

Requires JDK 17 and Android SDK platform 36.

```powershell
./gradlew.bat assembleDebug
```

For local release builds, set `GX12_KEYSTORE_PATH`, `GX12_KEYSTORE_PASSWORD`, `GX12_KEY_ALIAS`, and `GX12_KEY_PASSWORD`, then run `./gradlew.bat assembleRelease`.

## Privacy

The app has no analytics or backend. It reads the Android paired-device list after the user grants nearby-device access. The update button contacts GitHub's public release API and downloads the release APK and checksum. No Bluetooth address is sent to GitHub by the app.

## Hardware investigation

The GX12's external USB-C port is documented for charging. These diagnostics do not establish that it exposes USB data or firmware access. Do not connect exposed internal board pins to a computer or open the housing casually: the unit contains a rechargeable lithium battery and opening it can damage the seal. External photos of labels, ports, and cable ends are safe and may help identify the exact hardware revision.
