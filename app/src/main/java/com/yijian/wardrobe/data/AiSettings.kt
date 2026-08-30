package com.yijian.wardrobe.data

import android.content.Context

class AiSettings(context: Context) {
    private val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
    var deepSeekEnabled: Boolean
        get() = prefs.getBoolean("deepseek_enabled", false)
        set(value) { prefs.edit().putBoolean("deepseek_enabled", value).commit() }
    var bailianEnabled: Boolean
        get() = prefs.getBoolean("bailian_enabled", false)
        set(value) { prefs.edit().putBoolean("bailian_enabled", value).commit() }
    var workspaceId: String
        get() = prefs.getString("workspace_id", "") ?: ""
        set(value) = prefs.edit().putString("workspace_id", value.trim()).apply()
    var deepSeekBaseUrl: String
        get() = prefs.getString("deepseek_base_url", "https://api.deepseek.com") ?: "https://api.deepseek.com"
        set(value) = prefs.edit().putString("deepseek_base_url", value.trimEnd('/')).apply()
    var deepSeekVisionModel: String
        get() = prefs.getString("deepseek_vision_model", "deepseek-v4-flash-vision-exp") ?: "deepseek-v4-flash-vision-exp"
        set(value) = prefs.edit().putString("deepseek_vision_model", value.trim()).apply()
    var deepSeekTextModel: String
        get() = prefs.getString("deepseek_text_model", "deepseek-v4-flash") ?: "deepseek-v4-flash"
        set(value) = prefs.edit().putString("deepseek_text_model", value.trim()).apply()
    var lastDeepSeekTest: String
        get() = prefs.getString("last_deepseek_test", "尚未测试") ?: "尚未测试"
        set(value) = prefs.edit().putString("last_deepseek_test", value.take(240)).apply()
    var lastBailianTest: String
        get() = prefs.getString("last_bailian_test", "尚未测试") ?: "尚未测试"
        set(value) = prefs.edit().putString("last_bailian_test", value.take(240)).apply()

    fun capabilityEnabled(capability: AiCapability): Boolean = prefs.getBoolean("cap_${capability.name}", false)
    fun setCapability(capability: AiCapability, enabled: Boolean) {
        prefs.edit().putBoolean("cap_${capability.name}", enabled).commit()
    }
    fun shoeLockedFor(fingerprint: String): Boolean = prefs.getString("shoe_lock_fingerprint", null) == fingerprint
    fun lockShoe(fingerprint: String) = prefs.edit().putString("shoe_lock_fingerprint", fingerprint).putBoolean("cap_SHOE", false).apply()
    fun unlockShoeAfterVerifiedNewKey(newFingerprint: String) {
        if (prefs.getString("shoe_lock_fingerprint", null) != newFingerprint) prefs.edit().remove("shoe_lock_fingerprint").apply()
    }
    fun clearProvider(provider: AiProvider) {
        val edit = prefs.edit()
        AiCapability.entries.filter { it.provider == provider }.forEach { edit.remove("cap_${it.name}") }
        if (provider == AiProvider.DEEPSEEK) edit.putBoolean("deepseek_enabled", false).remove("last_deepseek_test")
        else edit.putBoolean("bailian_enabled", false).remove("workspace_id").remove("last_bailian_test")
        edit.apply()
    }
}
