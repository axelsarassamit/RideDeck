# RideDeck

Phone controls, selectable external map apps, messaging, music, and split-screen map launch. No external dashboard integration.

Repository: https://github.com/axelsarassamit/RideDeck

App name: **RideDeck**

## Build

Java 17 and Android SDK 36. Run `./gradlew testDebugUnitTest assembleDebug` to verify the unit tests and build. No flavor selection is required.

Signed builds use the existing release service to preserve the original certificate. Run `./scripts/Build-Signed.ps1 -Version 0.12.7` from this project with GitHub CLI authenticated. The service builds the exact committed HEAD and returns an APK, checksums, and source provenance. Private signing keys remain in GitHub secrets. Each project owns its source and update releases.

The phone map launch and split-screen behavior require physical-device verification. Provider keys are never committed. See THIRD_PARTY_NOTICES.md for licensing.

## Licensing

Original source code and original project materials are protected under
[LICENSE](LICENSE). Official releases supplied with these terms may be used
without a license fee for personal, noncommercial use under [EULA.md](EULA.md).
Source reuse, redistribution and commercial or organizational application use
require separate permission, except where another applicable license or law
permits them.

[Third-party notices](THIRD_PARTY_NOTICES.md) and bundled component licenses
remain applicable. These terms do not revoke earlier grants or limit rights
provided by third-party licenses. No commercial rights to Pillion-derived code
are granted by this project.
