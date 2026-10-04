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
Pillion's XMAX CCU mapping selects 480 x 234 pixels for 006-B3952 part numbers.

This distribution is for noncommercial personal and hobby use. No commercial
rights to Pillion's code are granted here. RideBridge is independent of Yamaha,
Google, Garmin, Spotify, WhatsApp and the Pillion project.
