package com.axelsarassamit.gx12;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;

import java.security.MessageDigest;

public final class DashPackageVerifier {
    public static final String PACKAGE_NAME = "com.axelsarassamit.ridedeck.dash";
    public static final Uri PROVIDER_URI = Uri.parse("content://" + PACKAGE_NAME + ".bridge");
    private static final String CERT_SHA256_HEX = "cef073342e133ad4c6650da1a276ae4ad03e3396cecac67143154aeb6e6c731b";
    private static final byte[] CERT_SHA256 = decodeHex(CERT_SHA256_HEX);

    private DashPackageVerifier() { }

    public static boolean isInstalledAndSigned(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(PACKAGE_NAME, Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES);
            if (info.applicationInfo == null || !info.applicationInfo.enabled) return false;
            Signature[] signers;
            if (Build.VERSION.SDK_INT >= 28) {
                if (info.signingInfo == null) return false;
                signers = info.signingInfo.getApkContentsSigners();
            } else {
                signers = info.signatures;
            }
            if (signers == null || signers.length != 1) return false;
            byte[] actual = MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray());
            return MessageDigest.isEqual(CERT_SHA256, actual);
        } catch (Exception unavailableOrUntrusted) {
            return false;
        }
    }

    public static boolean supportsPendingIntentRecording(Context context) {
        if (!isInstalledAndSigned(context)) return false;
        try {
            String version = context.getPackageManager().getPackageInfo(PACKAGE_NAME, 0).versionName;
            if (version == null) return false;
            String[] current = version.replaceFirst("^[vV]", "").split("\\.");
            int[] required = {0, 5, 3};
            for (int i = 0; i < required.length; i++) {
                int value = i < current.length ? Integer.parseInt(current[i]) : 0;
                if (value != required[i]) return value > required[i];
            }
            return true;
        } catch (Exception unavailableOrInvalidVersion) {
            return false;
        }
    }

    private static byte[] decodeHex(String value) {
        byte[] decoded = new byte[value.length() / 2];
        for (int i = 0; i < decoded.length; i++) {
            decoded[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return decoded;
    }
}
