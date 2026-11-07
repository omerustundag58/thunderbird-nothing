/*
 * SPDX-FileCopyrightText: 2026 The Thunderbird for Android contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package net.thunderbird.feature.notification.impl.receiver

import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.ui.style.SystemNotificationStyle

/**
 * Detects one-time passwords / verification codes in system notifications.
 *
 * This is intentionally a pure, local heuristic: nothing leaves the device.
 * It only runs when the notification text contains a code-related keyword,
 * which keeps false positives low (dates, order numbers, etc.).
 */
internal object OtpExtractor {
    private val keywordRegex = Regex(
        pattern = """(?iu)\b(kod|doğrulama|dogrulama|onay|şifre|sifre|giriş|giris|code|verification|verify|otp|passcode|security)\b""",
    )

    private val codeRegex = Regex(pattern = """(?<!\d)(\d{4,8})(?!\d)""")

    private val digitSeparatorRegex = Regex(pattern = """(?<=\d)[\s\u00A0\-–—]+(?=\d)""")

    fun extract(notification: SystemNotification): String? {
        val texts = buildList {
            add(notification.title)
            notification.contentText?.let(::add)
            when (val style = notification.systemNotificationStyle) {
                is SystemNotificationStyle.BigTextStyle -> add(style.text)
                is SystemNotificationStyle.InboxStyle -> {
                    add(style.bigContentTitle)
                    add(style.summary)
                    style.lines.forEach { add(it.toString()) }
                }
                SystemNotificationStyle.Undefined -> Unit
            }
        }

        val combined = texts.joinToString(separator = " ")
        if (!keywordRegex.containsMatchIn(combined)) return null

        // Some senders format the code as "123 456" or "123-456".
        val normalized = combined.replace(digitSeparatorRegex, "")

        return codeRegex.find(normalized)?.groupValues?.get(1)
    }
}
