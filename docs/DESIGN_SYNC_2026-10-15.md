# RideDeck family design sync, 15 October 2026

Reference: Yamaha main e8907a7. Standard keeps external navigation apps. Yamaha branding, CCU, bike display, provider credentials, built-in routing and vehicle data remain exclusive to Yamaha.

Shared surfaces: background #0C111B, panel #141D2B, raised #1C293B, border #2B3A50, text #F4F6FB, secondary #A3B2C9. Sans-serif and medium headings, 8dp surface corners, readable large touch controls, dark dialogs and inset-aware screens. Recording/error colors keep their meaning.

Standard gains separate track/artist/source metadata, truthful playback timeline, album-art fallback, setup category icons, compact customization rows, and editable large reply review with draft preservation, explicit send, copy/open fallback, wearable and invisible notification reply discovery. Existing theme indices and selected music/maps/messages remain intact. Ride and trusted Dash recording protocols are preserved.

Dash uses matching shared surfaces, dark dialogs, density-aware spacing and typography. Its preview-first recorder, camera settings, clip library, overlays, editing, map and power gates remain app-specific. Both hosts share the refreshed timer/Dash control surfaces.

Validation: local unit tests and Android builds, exact-head CI and signed-build checks, and connected-phone portrait/landscape/menu smoke checks are tracked separately. This design sync does not establish road, Yamaha hardware, stabilization or screen-off reliability.
