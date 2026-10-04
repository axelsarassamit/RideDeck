# RideBridge

An independent Android motorcycle cockpit targeting the Yamaha XMAX 2024 Tech MAX. It brings together Google Maps, Spotify, Yamaha Y-Connect, Garmin StreetCross, WhatsApp and Google voice, with large controls, headset status, a timer and signed in-app updates. Previously called GX12 Companion; the package identity and release download name stay the same so existing installations can update.

## Mount position and app choices

Open **Setup > Layout + apps** (also available from the Apps dock).

- **Left mount**: music panel on the left, quick actions in their normal order.
- **Centre mount**: balanced panels.
- **Right mount**: music panel on the right and mirrored quick-action order.
- Select several notification sources: WhatsApp, WhatsApp Business, LINE, Messenger, TikTok and the phone's current default SMS app. Defaults stay WhatsApp only until changed. A separate current preview is held in memory for each selected app. **Next app** cycles through them, and Read aloud reads the displayed preview. Currently active notifications are loaded when access connects; dismissed notification history is not fetched.
- Choose Google Maps, Waze, Grab Driver, LINE MAN RIDER or Garmin StreetCross as the navigation/rider app. Map launches the selected app when not using the dedicated display.

All non-Google apps on the dedicated bike display are experimental, not hardware-verified integrations. The whole selected rider app is shown, not a separately extracted map. Their sign-in, orders, routing and protected-screen behaviour stay inside the original apps. RideBridge does not accept jobs or automate rider workflows. Only Google Maps has RideBridge's destination handoff in dedicated display mode. Choose a new display app before preparing a session.

Chat integration means notification previews, opening the original notification and optional explicit speech, not account login, full inbox access or replies. SMS uses notifications without READ_SMS. TikTok notifications can include general alerts as well as messages. Missing, hidden or redacted notification content cannot be recovered by RideBridge. Removing app selections clears old previews.

## Landscape cockpit and bike-only map

Version 0.6.0 replaces the scrolling ride homepage with a landscape cockpit. Music and WhatsApp stay in fixed panels, with large playback controls, track artwork, explicit Read aloud, voice and an app dock. Setup holds permissions, update checks and device tools.

The optional **Set up bike-only map** flow prepares a separate Google Maps display through local Wireless debugging. The phone then stays on music/messages while the XMAX receives map images. It uses the installed Maps app and its existing login, with no paid Google Maps Platform project. This mode is experimental and has not been tested on the user's physical phone/bike. See [the full setup guide](BIKE_DISPLAY_SETUP.md) for pairing ports, session teardown and OEM limitations.

Without that setup, ordinary screen sharing and Maps split-screen remain available. This fallback needs the map visible on the phone. Accounts remain in their official apps; RideBridge does not collect login credentials or provide a personal WhatsApp login.

## Experimental Yamaha dash casting

RideBridge includes Pillion's NaviLite protocol code, pinned to revision `29497f4ea3bccc5cd40c4647f8e4d8345eeddda3`, with its license and required notice. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Display size follows Pillion CCU part-number detection: `006-B3952` uses 480 × 234, other compatible NaviLite units use 480 × 240. No physical bike validation has been performed for this build.

1. Park the bike. Keep the Yamaha CCU paired through the normal bike setup.
2. Close StreetCross or another app currently casting to the dash.
3. Tap **Cast map to Yamaha dash** and select your paired Yamaha CCU. The headset is a separate Bluetooth device.
4. Approve Android screen sharing. Select **Google Maps** only if Android offers single-app sharing. Whole-screen mode exposes everything visible on the phone, including message previews.
5. Open Google Maps and rotate the phone landscape with normal phone controls. Use the dash's navigation view. A portrait map is letterboxed to fit the wide dash.
6. End with **Stop casting**, the notification Stop action, or Android's sharing indicator.

Android must approve every screen-sharing session. Frames remain in memory and are sent by Classic Bluetooth RFCOMM to the explicitly chosen paired CCU using service `00007220-0000-1000-8000-00805f9b34fb`. There are no stored recordings or screen uploads. Connection and acknowledgement timeouts close the socket. Capture stops after connection failure, process death or revoked screen sharing. This is map-image casting, not integration with bike controls or replacement of safety instruments. No automatic reconnect or Bluetooth pairing changes are performed. The separate-display mode is described above.

Pillion-derived code is licensed under PolyForm Noncommercial 1.0.0. This distribution is for noncommercial personal and hobby use. Required Notice: Copyright 2026 the Pillion authors.

The phone's Bluetooth diagnostics show standard A2DP, AVRCP, HFP, and PBAP services, but no documented GX12-specific control service. The app does not claim to change headset settings or read its battery.

## Ride tools

- Start, pause, resume, and reset a ride timer. Timer state is kept on the phone.
- Tick off helmet, headset, phone mount, and route reminders. Checklist state is kept on the phone.
- Open Google Maps or enter a destination and hand it off to Maps.
- Use **Open map beside app dock** to ask Android to place Google Maps next to the RideBridge dock in split-screen. The phone's Android version and manufacturer determine whether adjacent app launch is supported; if it is ignored, Maps opens normally. From the dock, Google Maps, StreetCross, Spotify, Y-Connect, and WhatsApp can be launched into the adjacent pane when Android permits it.
- The main ride actions use larger touch targets for easier tapping with riding gloves; the ride screen stays fixed, and full-screen transport buttons are 72dp tall. The compact split-screen companion reduces controls to 56dp and shows a shorter message preview.
- Launch Garmin StreetCross where installed and supported by the motorcycle/region. StreetCross and Google Maps are separate navigation apps; the hub does not combine their maps.
- Launch Spotify and control compatible active music sessions (Spotify preferred) from the ride screen after granting optional Android Notification access.
- Open the official Yamaha Motorcycle Connect (Y-Connect) app.
- Open WhatsApp and optionally show the latest WhatsApp notification preview on this phone. It does not access chat history or upload message content. Optional voice-to-text replies use the source notification reply action after an explicit Send confirmation.
- Start the phone's configured voice assistant without requesting microphone access. Set Google as Android's assistant for Google voice commands.

These tools do not record GPS, distance, speed, or a route. Yamaha account, motorcycle telemetry, settings, and ride logs remain inside Yamaha's app. Set navigation before moving.

## Music controls and privacy

Android requires the user to enable RideBridge in **Notification access** before an app can view and control Spotify's active media session or receive WhatsApp notifications. This is a broad and sensitive Android permission. RideBridge uses Spotify's active media session for track details and playback buttons. For messaging, it reads new and currently active notifications from the sources you select and keeps one current preview per app in memory while the app process runs. It does not read chat history, persist message content, process unselected apps' notification text, or transmit notification content. Access can be revoked at any time from Android Settings.

Android may block Notification access for a sideloaded app. If you choose to enable Spotify controls, open **Settings > Apps > RideBridge > ⋮ > Allow restricted settings**, then return to RideBridge and enable Notification access. This is optional; Maps, Y-Connect, the ride timer, checklist, Spotify launch shortcut, and headset controls can be used without it.

Spotify must expose a compatible Android media session. Some controls may not be available for every item. RideBridge does not send proprietary commands to the headset; audio still goes through Android's normal Bluetooth connection. The Talk to Google button calls Android's configured voice assistant. This app requests Android split-screen placement for a map and app dock; the phone decides whether to honor adjacent-app launch. The dock cannot draw inside the Maps, Y-Connect, or StreetCross app, and a live embedded Google map would require a Google Maps Platform API key and billing setup. Android Auto itself requires a compatible vehicle or aftermarket head unit.

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

The app has no analytics or backend. It reads paired devices after nearby-device permission. Optional notification access provides Spotify controls and the selected message previews held only in memory. Screen sharing is separately approved by Android for each casting session; whole-screen sharing includes any visible messages. Timer and checklist state stay in local preferences. The update button contacts GitHub and downloads the APK and checksum. Bluetooth addresses and messages are not uploaded to GitHub.

## Hardware investigation

The GX12's external USB-C port is documented for charging. These diagnostics do not establish that it exposes USB data or firmware access. Do not connect exposed internal board pins to a computer or open the housing casually: the unit contains a rechargeable lithium battery and opening it can damage the seal. External photos of labels, ports, and cable ends are safe and may help identify the exact hardware revision.

## Quick camera

Tap Camera on the ride dock and choose Front photo, Rear photo, Front video or Rear video. The built-in camera has large capture, record, stop, mode and camera-switch buttons. Photos save to Pictures/RideBridge and videos to Movies/RideBridge on Android 10+, visible in the gallery. Camera permission is requested on first use; video sound requires microphone access and is silent if denied. Android 8/9 require storage permission. Recording is foreground-only and stops when leaving the camera. Hardware capture needs a phone test.

## Portrait and message reading (0.9.0)

RideBridge follows the phone orientation setting and supports portrait and landscape. Portrait stacks music and messages. Choose app lists all enabled sources, including sources without a current notification. Latest from all apps shows the newest received notification; new arrivals return the card to that latest view. Tap the preview for a large scrollable message view. Text comes from Android notifications (up to 20,000 characters), not full chat history; redacted or truncated source notifications cannot be expanded by RideBridge. Telegram, Signal, Instagram, Viber, Discord and WeChat are additional selectable sources. Actual notifications and layout require a physical phone check.

## Voice replies (0.9.1)

Tap a preview to expand it; tap the expanded text again to close. Reply uses the method chosen in Setup > Layout + app choices > Reply method. Voice to text launches the phone speech recognition service, shows the recognized words, recipient and app, and sends only after Send is tapped. The recognition provider may process speech online. Direct replies require an active notification with a free-form Android reply action. Voice message mode opens the original conversation for that app to record/send audio; RideBridge does not record a chat voice note itself. These paths need a phone test with each messaging app.

## Bike selector (0.10.0)

Setup includes a saved bike dropdown. The list follows [Pillion compatibility reports](https://pillion.app/en/bikes/), with an automatic/other StreetCross-compatible Yamaha option. Pillion reports MT-07 (2025), R9 (2026), MT-09 (2024/2025), MT-09 SP (2026) and XSR900 (2025); XSR900 GP, Tracer 9 GT+, Niken GT, TMAX and XMAX are listed as likely rather than confirmed there. NMAX is included based on the upstream scooter CCU mapping. A supported navigation dash and Garmin StreetCross compatibility are required; model names alone do not guarantee it. RideBridge hardware validation remains outstanding. Actual CCU detection controls frame dimensions, not the dropdown. Other manufacturers and Android Auto displays use different protocols and are not covered.

## More navigation choices (0.10.1)

The navigation picker also includes HERE WeGo, Sygic, MAPS.ME, OsmAnd, OsmAnd+ and Organic Maps. Install the chosen app separately and prepare routes/downloads in it. Existing app subscriptions or paid features still apply. Launching, adjacent view and experimental bike projection use the selected app; destination handoff inside RideBridge remains Google Maps only. Other apps must have their route prepared in their own interface. Bike projection behavior has not been tested for these apps.

## Full-screen messages (0.10.2)

Tap the message card to open a full-screen reader with Close, Reply, Read aloud and Read/seen. Close leaves read state unchanged. If the source notification provides Android's semantic Mark as read action, RideBridge invokes it; otherwise Read/seen acknowledges only inside RideBridge. Latest received text remains in memory even if the notification disappears, until replaced by a newer message from that app, deselected or the process ends. Removed notification actions are invalidated. This does not fetch chat history or synchronize read status without the source action.

## Screen chrome (0.10.3)

Ride cockpit, message reader and camera hide system bars for more usable space. Swipe inward from the screen edge to reveal Android navigation temporarily. Setup shows dark system bars and reserves room for them, display cutouts and the Done button. OEM overlays and system-controlled installation/permission screens may still appear. Physical layout verification is outstanding.

## Simple ride controls (0.10.4)

The bottom ride dock now has only Map, Camera and Voice. Apps and Cast are removed from it. Setup is a smaller header control. Start/stop casting happens in parked Setup under Bike display. App choices remain in Setup; music and messages keep their own direct controls. The ride header no longer displays verbose casting setup text.
