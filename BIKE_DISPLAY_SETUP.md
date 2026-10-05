# Map on the bike, controls on the phone

RideBridge defaults to a landscape phone cockpit: music transport, track artwork,
latest WhatsApp preview, Read aloud, voice and an app dock. The screen has no
scrolling settings list. Setup holds access, updates, headset details and casting.

## Experimental bike-only Google Maps

This mode uses the installed Google Maps on a separate virtual display, adapted
from Pillion. No Google Maps Platform API key or billing account is involved.
It is not ordinary whole-phone mirroring. Maps keeps its existing Google login;
RideBridge does not collect account passwords.

Requirements: Android 11+, Developer options, Wireless debugging, Wi-Fi during
setup, installed Google Maps and an already paired compatible Yamaha CCU.
This build has not been tested on a physical phone or XMAX. OEM restrictions can
prevent helper startup, task movement or sustained capture. It is experimental.

1. Park. Close StreetCross and other apps using the navigation connection.
2. Open RideBridge > Setup > Set up bike-only map > Developer options.
3. Enable Wireless debugging yourself. This gives RideBridge local debugging
   access when you pair it. Only proceed if you want that access.
4. Choose Pair device with pairing code in Android. In RideBridge's Pair dialog,
   enter its pairing port and six-digit code. Keep Android's pairing-code dialog
   active in split-screen if it closes when switching apps.
5. Return to the main Wireless debugging screen. Note its connection port. This
   port is different from the pairing port.
6. In RideBridge, choose Connect and enter that port. RideBridge connects to this
   phone through 127.0.0.1, prepares the display, and opens the Yamaha device picker.
7. Choose the Yamaha CCU. Google Maps moves to the separate display. Use Map in
   the phone dock to enter a destination before moving. Guidance uses Maps'
   standard driving mode, not a promised motorcycle-specific route profile.
8. Cast ends the session when pressed again. The casting notification also has Stop.
   Android 13+ users can enable casting notifications in Setup to show this action.

Repeat Connect for each new session and after rebooting. An unused prepared
helper expires after 30 seconds. Stop or a lost local connection exits the helper
and ends the dedicated Maps session. No automatic helper restart is performed.

The helper uses privileged Android interfaces, whose availability varies across
phone builds. We do not enable legacy TCP debugging, grant usage stats, change
phone power settings or modify Bluetooth pairing. The shell helper's loopback
image stream requires a random per-session token. Frames are not saved or uploaded.
Wireless debugging may stop the helper when Wi-Fi/debugging disconnects on some
phones; a parked test is required before depending on it for a ride.

Revoke access in Android > Wireless debugging > Paired devices > RideBridge.
You can then turn Wireless debugging off yourself. The app's locally generated
debugging identity remains in private storage until uninstall/data clear.

## Bike-only requirement (0.11.1)

Phone mirroring is disabled. If the helper is not prepared, or Maps cannot move to the separate display, casting fails. Render size follows the bike handshake: 960x468 at 320 dpi for XMAX 480x234, or 960x480 for 480x240. Stop ends the dedicated navigation app session instead of reopening it on the phone. Wi-Fi-off survival and map readability are unverified until tested on the actual phone and bike for 15 minutes with mobile data and Bluetooth.

## Music and messaging

Sign in through Spotify or your other music app. Optional Android notification
access exposes compatible media sessions; Spotify is preferred, with another
active player used if Spotify has no session. Available skip actions vary by player.
WhatsApp previews come from local notifications, not full chat history. Read aloud
is explicitly requested and uses Android's text-to-speech engine. That engine's
privacy and online/offline behaviour depend on the phone's selected engine.
Accounts remain in the original app. Optional voice-to-text replies use its notification reply action after a Send confirmation; voice-message recording opens the original app.
Y-Connect and StreetCross remain app launch integrations.

## App and mount choices (0.7.0)

Setup > Layout + apps sets mount position, selected messaging sources and the bike-display app. Google Maps remains the default. Waze, Grab Driver, LINE MAN RIDER and StreetCross use their own login and UI. Their external-display support is experimental; the whole selected app is captured, including any visible customer/job information. Configure routes/jobs in the original app while parked. RideBridge can only hand a destination to Google Maps in this mode. Change app before preparing the helper; stop an active session first.

Mount position is a manual reach/layout preference, not GPS tracking or automatic mount detection. Chat previews are notification-based and remain in memory, one per selected app. Select SMS again if you change the default SMS app. Read aloud uses the selected Android speech engine and may use its online services.
