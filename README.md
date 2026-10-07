# RideDeck

Phone controls, selectable external map providers, and split screen. Yamaha dashboard transport is excluded.

Repository: https://github.com/axelsarassamit/RideDeck

App name: **RideDeck**

## Build

Java 17 and Android SDK 36. Run `./gradlew assembleDebug` or `./gradlew assembleRelease`. No flavor selection is required.

Signed builds use the historical repository signing service to preserve the original certificate. Run `./scripts/Build-Signed.ps1 -Version 0.12.0` from this project with GitHub CLI authenticated. The service builds the exact committed HEAD and returns an APK, checksums, and source provenance. Private signing keys remain in GitHub secrets. Each project owns its source and update releases.

No tests or bike verification were performed for this split. Provider keys are never committed. See THIRD_PARTY_NOTICES.md for licensing.
