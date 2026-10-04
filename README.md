# GEARELEC GX12 Companion

An independent portrait Android ride cockpit for GEARELEC GX12 users. It brings together one-tap access to Google Maps, Spotify, Yamaha Y-Connect, Garmin StreetCross, WhatsApp, and the phone's voice assistant, with music controls, optional WhatsApp notification previews, headset status, a ride timer, a pre-ride checklist, and signed in-app updates.

The phone's Bluetooth diagnostics show standard A2DP, AVRCP, HFP, and PBAP services, but no documented GX12-specific control service. The app does not claim to change headset settings or read its battery.

## Ride tools

- Start, pause, resume, and reset a ride timer. Timer state is kept on the phone.
- Tick off helmet, headset, phone mount, and route reminders. Checklist state is kept on the phone.
- Open Google Maps or enter a destination and hand it off to Maps.
- Use **Open map beside app dock** to ask Android to place Google Maps next to the GX12 dock in split-screen. The phone's Android version and manufacturer determine whether adjacent app launch is supported; if it is ignored, Maps opens normally. From the dock, Google Maps, StreetCross, Spotify, Y-Connect, and WhatsApp can be launched into the adjacent pane when Android permits it.
- The main ride actions use larger touch targets for easier tapping with riding gloves; the dock scrolls vertically, and media controls are stacked rather than crowded into small side-by-side buttons.
- Launch Garmin StreetCross where installed and supported by the motorcycle/region. StreetCross and Google Maps are separate navigation apps; the hub does not combine their maps.
- Launch Spotify and control Spotify playback from the ride screen after granting optional Android Notification access.
- Open the official Yamaha Motorcycle Connect (Y-Connect) app.
- Open WhatsApp and optionally show the latest WhatsApp notification preview on this phone. It does not access chat history, send replies, or upload message content.
- Start the phone's configured voice assistant without requesting microphone access. Set Google as Android's assistant for Google voice commands.

These tools do not record GPS, distance, speed, or a route. Yamaha account, motorcycle telemetry, settings, and ride logs remain inside Yamaha's app. No public Yamaha integration is included. Set navigation before moving and follow local road safety laws.

## Music controls and privacy

Android requires the user to enable GX12 Companion in **Notification access** before an app can view and control Spotify's active media session or receive WhatsApp notifications. This is a broad and sensitive Android permission. GX12 Companion uses Spotify's active media session for track details and playback buttons. For WhatsApp, it reads only new notifications from WhatsApp/WhatsApp Business and keeps the latest preview in memory while the app process runs. It does not read chat history, persist message content, process other apps' notification text, or transmit notification content. Access can be revoked at any time from Android Settings.

Android may block Notification access for a sideloaded app. If you choose to enable Spotify controls, open **Settings > Apps > GX12 Companion > ⋮ > Allow restricted settings**, then return to GX12 Companion and enable Notification access. This is optional; Maps, Y-Connect, the ride timer, checklist, Spotify launch shortcut, and headset controls can be used without it.

Spotify must expose a compatible Android media session. Some controls may not be available for every item. GX12 Companion does not send proprietary commands to the headset; audio still goes through Android's normal Bluetooth connection. The Talk to Google button calls Android's configured voice assistant. This app requests Android split-screen placement for a map and app dock; the phone decides whether to honor adjacent-app launch. The dock cannot draw inside the Maps, Y-Connect, or StreetCross app, and a live embedded Google map would require a Google Maps Platform API key and billing setup. Android Auto itself requires a compatible vehicle or aftermarket head unit.

## Install and update

Download `gx12-companion-release.apk` from the [latest release](https://github.com/axelsarassamit/gearelec-gx12-companion/releases/latest). Android will show the normal install confirmation. The initial public release is a sideload, so Android may ask you to allow installs from the app or browser you used to download it.

Use **Check for updates** in the app. It checks the public GitHub Releases API, downloads the APK and `checksums.txt` over HTTPS, verifies the APK's SHA-256, and opens Android's package installer. Android asks you to approve each installation; the app cannot silently replace itself. Updates are signed with one stable private key so Android can confirm that a release belongs to this app.

New versions are published by pushing a tag such as `v0.3.1`. The GitHub Actions release workflow builds and signs the APK and attaches it and its checksum to a public GitHub Release.

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

The app has no analytics or backend. It reads the Android paired-device list after the user grants nearby-device access. Notification access is optional and used to query Spotify's media session and send playback commands; notification text is not read or retained. Ride timer and checklist state stay in app-local preferences. The update button contacts GitHub's public release API and downloads the release APK and checksum. No Bluetooth address is sent to GitHub by the app.

## Hardware investigation

The GX12's external USB-C port is documented for charging. These diagnostics do not establish that it exposes USB data or firmware access. Do not connect exposed internal board pins to a computer or open the housing casually: the unit contains a rechargeable lithium battery and opening it can damage the seal. External photos of labels, ports, and cable ends are safe and may help identify the exact hardware revision.
