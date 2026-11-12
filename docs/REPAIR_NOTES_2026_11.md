# November development review

Standard stays on 0.12.32. Version 1.x remains reserved for the Play Store launch.

The four-app family uses the same light/dark colour tokens, dialog surfaces and
64dp general setup actions. Compact riding/editor controls keep their specific
layouts. Standard continues to open external navigation apps.

This review preserves the owner's Gradle/JDK migration and repairs queued ride
commands, pause-service teardown, reply editing over recreation, cancellable route
work and GPX handling. Update downloads enforce package, signer, version, size,
storage and cancellation checks. Debug companions use isolated package IDs and
the same debug certificate; release companions retain their original signer pins.

Local validation: 68 JVM tests; debug app and instrumentation assembly; Android
lint with zero errors and 95 warnings. Seven focused lifecycle, setup and GPX
instrumentation tests also passed on an Android 15 emulator and the authorized
Xiaomi phone. These checks do not prove incoming-call audio, Yamaha hardware or
long-running background recording. Original-key delivery and exact-source CI
are separate gates and must be checked before installing a release.
