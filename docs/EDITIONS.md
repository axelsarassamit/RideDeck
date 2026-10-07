# RideDeck editions, 0.12.0 prototype

## Phone edition

The existing package, `com.axelsarassamit.gx12`, becomes the phone edition. A signed update preserves its music and message preferences. Choose the installed navigation app in Setup. Map opens it beside RideDeck using Android split screen. Window placement belongs to Android; this edition does not use debugging access to reposition windows.

The phone APK excludes the Pillion protocol, MapLibre library, dashboard service, display helper and ADB dependencies. Bluetooth remains for headset controls and microphone routing. Calls replace the message panel, while music remains available.

## Yamaha edition

The separate package `com.axelsarassamit.gx12.yamaha` can be installed alongside the phone edition. It has its own preferences and permission grants. MapLibre draws the app's own navigation map. No Google Maps app capture, virtual display, Wireless debugging, pairing code, connection port or Wi-Fi dependency is used. Online map and routing requests still need internet, which can be cellular.

Setup > Map + panel settings offers independent phone and dashboard choices: map, turn arrows or music. The phone panel occupies the former music position. Calls temporarily replace messages. Music on the dashboard shows metadata from the selected music session; hardware music-button integration is not implemented by this change.

Pair a compatible Yamaha CCU using Bluetooth settings. Choose it once in RideDeck Yamaha Setup. The saved bike is retried while the cockpit is visible if automatic connection is enabled. Automatic launch when the app is closed or after reboot is not implemented. Close StreetCross before connecting if it owns the same connection. Stop explicitly pauses reconnection.

## Provider setup

- MapTiler: map style, vector tiles and address suggestions. Create an account and API key. The free-plan logo and credits remain in map images; phone credits link to the providers.
- GraphHopper: route geometry and turn instructions. Configure a routing key and a profile available to that account. `scooter` is the initial profile from the provider's advertised capability list. That list does not establish plan access or motorcycle suitability in Thailand. Unsupported profiles fail visibly; no car fallback is used.
- Keys are stored using Android Keystore AES-GCM. They are not compiled into either APK, copied into diagnostics or uploaded to GitHub. Android backup is disabled.
- Provider calls send map areas, search queries, origins and destinations to their respective services. Provider quotas and account terms apply. No offline map-download system is implemented.

Allow precise location, then tap Map. Search suggestions appear after three characters. Choose a place and Navigate, or save it as a favorite. A shared Google Maps location can be resolved and searched inside Yamaha navigation, even without a connected bike. The phone edition forwards shared places to its selected app; individual app URI support varies.

Navigation waits for a fresh GPS fix, updates position from a location foreground service, estimates route progress using nearby route segments, requests rerouting after repeated off-route fixes and speaks upcoming instructions when enabled. These are an initial implementation, not a mature map-matching engine. Background rendering and GPS need real-device validation. The Stop notification ends the navigation service. Disconnecting the bike leaves phone navigation running.

Home and Work bike commands currently require latitude,longitude. Existing address-only favorites must be selected through search and saved with coordinates. Nearby fuel uses the existing Overpass lookup and supplies coordinates. Bike zoom changes the native map's camera. Adding intermediate stops is not implemented.

## Build and release

Build with `assemblePhoneDebug assembleYamahaDebug` or the corresponding Release tasks. The workflow can create signed artifacts manually without publishing a release. Release downloads have edition-specific names, `ridedeck-phone-release.apk` and `ridedeck-yamaha-release.apk`. The updater checks its own edition's asset only. Prototype tag releases are prereleases.

Compilation was performed during implementation. No automated tests or ride tests were run for this change. Prior Bluetooth transport worked on the user's XMAX; that evidence does not verify the replacement renderer or routing system.

## Licensing

MapLibre is separate from hosted map/search/routing services. Their licences and account terms all apply. The Yamaha protocol remains adapted from Pillion under PolyForm Noncommercial 1.0.0, so the Yamaha edition is for noncommercial personal and hobby use unless those rights are resolved. Removing ADB or selecting MapLibre does not remove that restriction. The phone edition excludes Pillion code.
