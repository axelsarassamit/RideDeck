package com.axelsarassamit.gx12

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Provider keys are local, encrypted, excluded from backup and never logged. */
object NavigationSecrets {
    /** Debug builds can be configured through USB into app-private storage, never exported. */
    fun importDebugSetup(context: Context) {
        if (!BuildConfig.DEBUG) return
        val file = java.io.File(context.filesDir, "provider-setup.json")
        if (!file.exists()) return
        try {
            require(file.length() < 4096)
            val setup = org.json.JSONObject(file.readText())
            for (name in listOf("maptiler", "graphhopper")) {
                val value = setup.optString(name)
                if (value.isNotBlank()) save(context, name, value)
            }
            RidePreferences.prefs(context).edit().putString("routing_profile", setup.optString("profile", "scooter")).apply()
            BikeDiagnostics.record(context, "Provider keys configured in encrypted private storage")
        } finally { file.delete() }
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("navigation_provider_keys", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("navigation_provider_keys", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun save(context: Context, name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        context.getSharedPreferences("navigation_keys", 0).edit()
            .putString(name, Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
    }
    @Synchronized fun read(context: Context, name: String): String = runCatching {
        val value = context.getSharedPreferences("navigation_keys", 0).getString(name, null) ?: return ""
        val parts = value.split(':')
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        }
        String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrDefault("")
}
