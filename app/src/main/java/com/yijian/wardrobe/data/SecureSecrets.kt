package com.yijian.wardrobe.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureSecrets(context: Context) {
    private val prefs = context.getSharedPreferences("secure_secrets", Context.MODE_PRIVATE)
    private val alias = "yijian_byok_aes_v1"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun key(): SecretKey {
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun put(name: String, value: String) {
        require(name == DEEPSEEK_KEY || name == BAILIAN_KEY)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        prefs.edit()
            .putString("${name}_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("${name}_value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun get(name: String): String? = runCatching {
        val iv = Base64.decode(prefs.getString("${name}_iv", null), Base64.NO_WRAP)
        val encrypted = Base64.decode(prefs.getString("${name}_value", null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }.getOrNull()

    fun remove(name: String) {
        prefs.edit().remove("${name}_iv").remove("${name}_value").apply()
    }

    fun masked(name: String): String {
        val value = get(name) ?: return "未配置"
        return "••••${value.takeLast(4)}"
    }

    fun fingerprint(name: String): String? = get(name)?.let {
        MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString("") { b -> "%02x".format(b) }
    }

    companion object {
        const val DEEPSEEK_KEY = "deepseek_key"
        const val BAILIAN_KEY = "bailian_key"
    }
}
