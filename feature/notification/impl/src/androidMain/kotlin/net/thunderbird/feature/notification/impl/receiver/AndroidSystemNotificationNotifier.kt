package net.thunderbird.feature.notification.impl.receiver

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationRegistry
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.style.SystemNotificationStyle
import net.thunderbird.feature.notification.impl.ui.action.NotificationActionCreator
import java.util.Locale

private const val TAG = "AndroidSystemNotificationNotifier"

@OptIn(ExperimentalTime::class)
internal class AndroidSystemNotificationNotifier(
    private val logger: Logger,
    private val applicationContext: Context,
    private val notificationRegistry: NotificationRegistry,
    private val notificationActionCreator: NotificationActionCreator<SystemNotification>,
) : SystemNotificationNotifier {
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(applicationContext)

    override suspend fun show(
        notification: SystemNotification,
    ): NotificationId {
        logger.debug(TAG) { "show() called with: notification = $notification" }
        val id = notificationRegistry.register(notification)
        val androidNotification = notification.toAndroidNotification()
        notificationManager.notify(id.value, androidNotification)
        return id
    }

    override suspend fun dismiss(id: NotificationId) {
        logger.debug(TAG) { "dismiss() called with: id = $id" }
        notificationManager.cancel(id.value)
        notificationRegistry.unregister(id)
    }

    override fun dispose() {
        logger.debug(TAG) { "dispose() called" }
    }

    private suspend fun SystemNotification.toAndroidNotification(): Notification {
        logger.debug(TAG) { "toAndroidNotification() called with systemNotification = $this" }
        val systemNotification = this
        return NotificationCompat
            .Builder(applicationContext, channel.id)
            .apply {
                setSmallIcon(
                    checkNotNull(icon.systemNotificationIcon) {
                        "A icon is required to display a system notification"
                    },
                )
                setContentTitle(title)
                setTicker(accessibilityText)
                contentText?.let(::setContentText)
                subText?.let(::setSubText)
                setOngoing(severity.dismissable.not())
                setWhen(createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
                asLockscreenNotification()?.let { lockscreenNotification ->
                    if (lockscreenNotification.notification != systemNotification) {
                        setPublicVersion(lockscreenNotification.notification.toAndroidNotification())
                    }
                }

                val overrideTap = systemNotification
                    .actions
                    .firstOrNull {
                        it is NotificationAction.Tap && it.override != null
                    } as? NotificationAction.Tap

                val tapAction = notificationActionCreator.create(
                    notification = systemNotification,
                    action = overrideTap?.override ?: NotificationAction.Tap(),
                )
                setContentIntent(tapAction.pendingIntent)

                setNotificationStyle(notification = systemNotification)

                val actionsWithoutTap = actions.filterNot { it is NotificationAction.Tap }
                if (actionsWithoutTap.isNotEmpty()) {
                    for (action in actionsWithoutTap) {
                        val notificationAction = notificationActionCreator
                            .create(notification = systemNotification, action)

                        addAction(
                            /* icon = */
                            notificationAction.icon ?: 0,
                            /* title = */
                            notificationAction.title,
                            /* intent = */
                            notificationAction.pendingIntent,
                        )
                    }
                }

                OtpExtractor.extract(systemNotification)?.let { code ->
                    val label = if (Locale.getDefault().language == "tr") {
                        "Kodu kopyala"
                    } else {
                        "Copy code"
                    }
                    addAction(0, label, createOtpPendingIntent(code))
                }
            }
            .build()
    }

    private fun createOtpPendingIntent(code: String): PendingIntent {
        val intent = Intent(applicationContext, CopyOtpReceiver::class.java).apply {
            putExtra(CopyOtpReceiver.EXTRA_CODE, code)
            putExtra(CopyOtpReceiver.EXTRA_TIMESTAMP, System.currentTimeMillis())
        }
        return PendingIntent.getBroadcast(
            applicationContext,
            code.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun NotificationCompat.Builder.setNotificationStyle(
        notification: SystemNotification,
    ) {
        when (val style = notification.systemNotificationStyle) {
            is SystemNotificationStyle.BigTextStyle -> setStyle(
                NotificationCompat.BigTextStyle().bigText(style.text),
            )

            is SystemNotificationStyle.InboxStyle -> {
                val inboxStyle = NotificationCompat.InboxStyle()
                    .setBigContentTitle(style.bigContentTitle)
                    .setSummaryText(style.summary)

                style.lines.forEach(inboxStyle::addLine)

                setStyle(inboxStyle)
            }

            SystemNotificationStyle.Undefined -> Unit
        }
    }
}
