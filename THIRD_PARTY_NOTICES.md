# Pillion

Required Notice: Copyright 2026 the Pillion authors

Source: https://github.com/alexandrevega/pillion
Revision: 29497f4ea3bccc5cd40c4647f8e4d8345eeddda3

The Kotlin protocol files and core ByteChannel, FrameReader, Handshake and
NaviLiteDisplay are adapted from Pillion. They remain under the PolyForm
Noncommercial License 1.0.0, copied in app/src/main/assets/PILLION_LICENSE.md.
https://polyformproject.org/licenses/noncommercial/1.0.0/

Changes: Android-only logger, bounded frame parsing, checksum validation,
authentication bounds checks. RideBridge uses a separate Android capture
service, explicit paired-device selection, session timeouts and stop controls.
PillionAdb and DashServer are also adapted from the same revision under the same
license. Changes include authenticated local frame transport, Maps-only launch,
session teardown and keeping phone controls visible. RideBridge does not invoke
the upstream usage-stats grants, legacy TCP debugging or phone panel-off logic.
Pillion's XMAX CCU mapping selects 480 x 234 pixels for 006-B3952 part numbers.

This distribution is for noncommercial personal and hobby use. No commercial
rights to Pillion's code are granted here. RideBridge is independent of Yamaha,
Google, Garmin, Spotify, WhatsApp and the Pillion project.

## Local debugging dependencies

LibADB Android 3.1.1: https://github.com/MuntashirAkon/libadb-android
Used under its Apache-2.0 option (upstream dual licenses Apache-2.0 or GPL-3.0-or-later).
Its COPYING and component license texts are included in app/src/main/assets/libadb-licenses.
Upstream source retains the authors' notices and is available at the link above.
Bouncy Castle 1.81: https://www.bouncycastle.org/licence.html
Conscrypt Android 2.5.3: https://github.com/google/conscrypt (Apache-2.0).
Dependency META-INF license and notice resources are merged into the package.
