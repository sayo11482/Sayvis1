package com.example.sayvis.screentranslate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sayvis.MainActivity
import com.example.sayvis.R
import com.example.sayvis.i18n.SayvisStrings
import com.example.sayvis.settings.SettingsStore

/**
 * "Permanent" without being unaccountable.
 *
 * Android does not let an app keep a screen-capture session across a reboot — the owner must
 * approve capture again, every session, by design. What SAYVIS *can* do is remember that the
 * translator was left armed, and after a reboot quietly offer to resume it: one tap opens the
 * translator screen with the consent dialog ready.
 *
 * Nothing is captured, no service is started and no notification is posted when the owner
 * turned the option off, when the emergency lock is engaged, or when the owner has muted
 * SAYVIS notifications.
 */
class ScreenTranslatorBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val settings = runCatching { SettingsStore.get(context).current() }.getOrElse {
            Log.w(TAG, "Settings unavailable after boot: ${it.message}")
            return
        }
        if (!settings.screenTranslation.resumeAfterBoot) return
        if (settings.emergencyLockActive) return

        val isPersian = settings.isPersian(SayvisStrings.deviceIsPersian())
        val strings = SayvisStrings.of(isPersian)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemManager = context.getSystemService(NotificationManager::class.java)
            if (systemManager != null && systemManager.getNotificationChannel(CHANNEL_ID) == null) {
                systemManager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        strings.t("یادآور ترجمهٔ زندهٔ صفحه", "Live screen translator reminder"),
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = strings.t(
                            "پس از راه‌اندازی دوبارهٔ دستگاه یادآوری می‌کند که ترجمهٔ زنده فعال شود",
                            "Reminds the owner to re-arm live translation after a restart"
                        )
                    }
                )
            }
        }

        val open = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(ScreenTranslatorService.EXTRA_OPEN_SCREEN_TRANSLATOR, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            11,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_screen_translate)
            .setContentTitle(strings.t("ترجمهٔ زندهٔ صفحه", "Live screen translation"))
            .setContentText(
                strings.t(
                    "برای فعال‌سازی دوباره، ضبط صفحه را تأیید کنید.",
                    "Approve screen capture to arm the translator again."
                )
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching { manager.notify(NOTIFICATION_ID, notification) }
            .onFailure { Log.w(TAG, "Resume notification failed: ${it.message}") }
    }

    companion object {
        private const val TAG = "ScreenTranslatorBoot"
        private const val CHANNEL_ID = "sayvis_screen_translator_boot"
        private const val NOTIFICATION_ID = 4212
    }
}
