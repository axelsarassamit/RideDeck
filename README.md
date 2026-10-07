# RideDeck

Two Android motorcycle cockpit editions share music, messages, calls, headset controls and portrait-only Setup.

| Edition | Navigation | Bike display |
| --- | --- | --- |
| RideDeck | Selected external map app in Android split screen | None |
| RideDeck Yamaha | Own MapLibre map with configured map and routing services | Compatible Yamaha NaviLite CCUs over Bluetooth |

In the Yamaha edition, independently choose map, turn arrows or music for the phone panel and bike display. Calls occupy the message area in both editions.

The 0.12.0 implementation is a prototype. It replaces the old debugging-based map helper; it needs physical validation before relying on navigation while riding. See [edition setup, scope and limitations](docs/EDITIONS.md).

## Build

```
./gradlew.bat assemblePhoneDebug assembleYamahaDebug
```

Signed builds are produced by the GitHub workflow using the existing private signing key. The phone edition retains the original application identity; the Yamaha edition has its own package and can coexist. API keys are entered in Yamaha Setup and stored encrypted on that phone. No provider credentials belong in this repository.

## Notices

The Yamaha Bluetooth protocol derives from Pillion and remains noncommercial personal/hobby code. MapLibre does not change that restriction. See [third-party notices](THIRD_PARTY_NOTICES.md). The phone APK excludes that protocol and all ADB/display-helper dependencies.

[Older README](docs/LEGACY_README.md) documents the retired approach and is historical only.
