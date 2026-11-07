/*
 * SPDX-FileCopyrightText: 2026 The Thunderbird for Android contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package net.thunderbird.feature.notification.impl.receiver

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.util.Locale

/**
 * Copies a verification code to the clipboard without opening the app.
 *
 * Codes are short-lived: the receiver refuses anything older than [OTP_TTL_MS].
 */
class CopyOtpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getStringExtra(EXTRA_CODE) ?: return
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0L)
        val age = System.currentTimeMillis() - timestamp
        if (timestamp <= 0L || age > OTP_TTL_MS) return

        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, code))

        val message = if (Locale.getDefault().language == "tr") "Kod kopyalandı" else "Code copied"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_CODE = "net.thunderbird.feature.notification.extra.OTP_CODE"
        const val EXTRA_TIMESTAMP = "net.thunderbird.feature.notification.extra.OTP_TIMESTAMP"
        const val OTP_TTL_MS = 10 * 60 * 1000L

        private const val CLIP_LABEL = "verification_code"
    }
}
