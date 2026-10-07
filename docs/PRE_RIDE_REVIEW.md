# RideDeck pre-ride review

Reviewed 7 October 2026 for v0.11.9.

## Scope and evidence

Source review covered manifest/build/release configuration, cockpit/setup/preferences, notifications/replies, headset routing/speech, camera, updater, NaviLite authentication/framing, Bluetooth casting and the separate-display helper. No Android device was attached. Unit tests cover pure logic, not Android UI, audio routing or hardware compatibility.

## Corrections

- Automatic preparation runs in the foreground service so activity rotation does not cancel it.
- Failure/timeout requests phone fallback across activity recreation. Explicit Stop clears fallback, and Map can retry.
- Fallback waits while Setup is open. Setup is restored after rotation, and destruction removes pending UI callbacks.
- An old display-reader cannot clear a newer session or close its socket. Startup clears stale frames and checks app installation before spawning the helper.
- The ADB manager has a 20-second connection wait, and shell-stream reads have a 20-second close deadline and bounded output. Platform socket/open-stream timing remains dependent on libadb/Android.
- Long read-aloud text is chunked below Android's speech limit, preserving emoji pairs.
- Status errors are safe when the music label is absent. App selection releases unused prepared display access.

## Remaining limits

| Area | Current behavior / limit |
|---|---|
| Auto / Manual | Saved bike/phone choices; physical reconnect and fallback still need testing |
| Split-screen | Android owns support and placement; older Android needs manual entry; RideDeck cannot reliably close another app's pane |
| Bike map | Trusted separate display with Bluetooth images; startup, navigation, readability and Wi-Fi-off survival remain unverified |
| Resolution | Mapped scooter CCU uses 480x234, others default to 480x240; unknown CCUs are not verified by a dropdown |
| Address entry | Google Maps only; other listed apps require route preparation in their own UI and unverified external-display support |
| Messages | Selected notification previews, full reader, newest-first queue, Seen/next; source read/reply actions depend on each app |
| Headset microphone | Bluetooth route requested, but external dictation/assistant may override it; camera does not explicitly select headset mic |
| Camera | Tap preview for front/rear photo/video; local saving and repeated captures need phone testing |
| Music | Android media sessions; requires access and an available player session |
| Updates | GitHub checksum and Android installation; Play Protect prompts are Android-controlled |
| Native bike battery/signal/music/volume | Not implemented; Y-Connect protocol evidence still required |
| Cardo/Sena vendor controls, crash detection | Not implemented |

## Parked test sequence

1. Install v0.11.9. Allow Nearby devices and Music + message access; select message apps.
2. Start music and receive a test message. Check transport, source label, full reader, Read aloud, Seen/next and a headset voice reply.
3. Select Manual > Phone split-screen. Check portrait/landscape and camera capture/save.
4. Select Google Maps and Auto for the bike trial. Prepare debugging access while the phone has Wi-Fi. Close StreetCross/Pillion before RideDeck opens the navigation connection.
5. Start navigation while parked. Check viewport fill, phone controls, and entering a destination without disconnecting.
6. Turn phone Wi-Fi off while Bluetooth/mobile data remain on. Check stable maps for 15 minutes.
7. Disconnect the bike: confirm phone fallback. Start again, rotate during connection and repeat destination entry. Copy Setup > Bike connection diagnostics if it fails.

The Pillion-derived code carries a noncommercial notice. Commercial/Google Play publication is not cleared by this review. No Play publishing or third-party contact was performed.

## Automated results

31 unit tests passed; debug build and lint passed with no lint errors. Lint retains 79 warnings and 2 hints, mainly localization, dependencies and privileged helper APIs. Trust-manager warnings point to bundled ADB/BouncyCastle dependencies; the updater uses ordinary HTTPS validation. The installed libadb mDNS implementation filters discovered addresses against the phone's own network interfaces. This review is not a complete dependency security audit. Private display APIs remain an OEM compatibility risk.
