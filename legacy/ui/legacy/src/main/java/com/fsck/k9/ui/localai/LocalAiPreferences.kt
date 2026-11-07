package com.fsck.k9.ui.localai

import android.content.Context

/**
 * Small, dedicated preference store for the optional local language model.
 *
 * The general settings data store is backed by the (serialized) app settings model. Adding fields there would
 * require a settings migration, so the local AI toggle lives in its own SharedPreferences file. Category results
 * live in [CategoryStore].
 */
class LocalAiPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var isLlmEnabled: Boolean
        get() = preferences.getBoolean(KEY_LLM_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_LLM_ENABLED, value).apply()

    var lastScanAt: Long
        get() = preferences.getLong(KEY_LAST_SCAN_AT, 0L)
        set(value) = preferences.edit().putLong(KEY_LAST_SCAN_AT, value).apply()

    private companion object {
        const val PREFERENCES_NAME = "local_ai_preferences"
        const val KEY_LLM_ENABLED = "local_ai_llm_enabled"
        const val KEY_LAST_SCAN_AT = "local_ai_last_scan_at"
    }
}
