package com.axelsarassamit.gx12;

import java.util.Set;

/** Pure validation shared by the release check and the archive verifier. */
final class UpdatePolicy {
    static final long MAX_APK_BYTES = 80L * 1024 * 1024;
    static final long MAX_TEXT_BYTES = 256 * 1024;
    static int versionCode(String tag) {
        if (tag == null || !tag.matches("v?[0-9]{1,3}\\.[0-9]{1,2}\\.[0-9]{1,2}"))
            throw new IllegalArgumentException("Release tag must use numeric major.minor.patch.");
        String[] parts = tag.replaceFirst("^v", "").split("\\.");
        return Integer.parseInt(parts[0]) * 10000 + Integer.parseInt(parts[1]) * 100 + Integer.parseInt(parts[2]);
    }
    static boolean candidate(String installedPackage, String archivePackage, long installedVersion,
                             long archiveVersion, long releaseVersion, Set<String> installedSigners, Set<String> archiveSigners) {
        return installedPackage.equals(archivePackage) && archiveVersion > installedVersion
            && archiveVersion == releaseVersion && !installedSigners.isEmpty()
            && installedSigners.equals(archiveSigners);
    }
    private UpdatePolicy() { }
}
