# Yamaha dashboard feature investigation

## Implemented first slice

RideDeck reads NaviLite dashboard commands independently of image acknowledgements. Valid navigation-image start requests (55) resume JPEG transmission and refresh navigation/app state. Stop requests (56) pause images without closing Bluetooth. Other services are recorded as metadata only and ignored. The IMAGE_STOPPED (20) payload is not documented, so RideDeck does not guess it. Hardware testing remains required.

Setup exposes a bounded local diagnostic report with Copy. It records service/type/length and actions, not payloads, addresses, messages or images. This report describes RideDeck traffic only; it cannot capture Y-Connect traffic.

## Remaining features

| Feature | Phone side | Native dashboard side |
|---|---|---|
| Phone battery and charging | Android battery APIs | Y-Connect message mapping unknown |
| Cellular connection and signal | Android connectivity/telephony APIs, permissions depend on detail | Message mapping unknown |
| Headset connection | Bluetooth headset and music profiles | Message mapping unknown |
| Headset battery | Vendor or battery-service support varies by headset | Mapping and dashboard support unknown |
| Music information, play/pause/skip | Existing notification/media-session integration | Transport and button event mapping unknown |
| Volume | Android audio stream controls | Bike button event mapping unknown |
| Notifications and calls | Android notifications and supported actions | Transport, payload and app compatibility unknown |
| Navigation zoom/home/office/stop | Documented NaviLite services | Selected map app control needs a verified implementation |

The CCU advertises separate Garmin navigation and Y-Connect accessory protocols. NaviLite does not establish that status updates use the same channel. Do not assign guessed service IDs or reinterpret navigation buttons as volume buttons.

## Hardware evidence needed

While parked, capture Android Bluetooth HCI traffic with the working Yamaha and Garmin apps: initial connection, phone charging state changes, music title changes, play/pause/skip, volume, headset disconnect/reconnect and notification arrival. Compare one action at a time. Android bug reports/HCI captures may contain private device and communication data; inspect locally and extract only the relevant protocol records. No phone or headset was available for this implementation.

Sources:
- https://www.yamaha-motor.co.jp/mc/lineup/y-connect/garminstreetcross/
- https://github.com/alexandrevega/pillion/blob/main/docs/PROTOCOL.md
- https://developer.android.com/reference/android/bluetooth/BluetoothHeadset

Pillion protocol code remains subject to its existing noncommercial license. This research does not provide commercial authorization.
