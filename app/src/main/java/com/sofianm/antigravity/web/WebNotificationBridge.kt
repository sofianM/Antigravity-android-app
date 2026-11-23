package com.sofianm.antigravity.web

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sofianm.antigravity.MainActivity
import com.sofianm.antigravity.R
import java.util.concurrent.atomic.AtomicInteger

/**
 * Bridges Web Notification API calls and page-activity signals from Native WebView mode
 * to real Android notifications.
 */
class WebNotificationBridge(
    private val context: Context,
    private val isNotificationsEnabled: () -> Boolean,
    private val isAppInForeground: () -> Boolean
) {
    private val nextId = AtomicInteger(2000)

    init {
        ensureChannels(context)
    }

    @JavascriptInterface
    fun notify(title: String?, body: String?) {
        if (!isNotificationsEnabled()) return
        val safeTitle = title?.trim().orEmpty().ifBlank { "Antigravity" }
        val safeBody = body?.trim().orEmpty().ifBlank { "Nieuwe activiteit in je sessie" }
        postNotification(safeTitle, safeBody)
    }

    @JavascriptInterface
    fun onTitleActivity(newTitle: String?) {
        if (!isNotificationsEnabled() || isAppInForeground()) return
        val cleaned = newTitle?.trim().orEmpty()
        if (cleaned.isBlank()) return
        // Trigger a notification if the web app puts an unread badge like "(1)" or completion cue in title
        val looksLikeAlert = TITLE_ALERT_REGEX.containsMatchIn(cleaned)
        if (looksLikeAlert) {
            postNotification("Antigravity — Nieuwe activiteit", cleaned)
        }
    }

    fun postNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVITY)
            .setSmallIcon(R.drawable.ic_stat_antigravity)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(nextId.incrementAndGet(), notification)
        }
    }

    companion object {
        const val JS_INTERFACE_NAME = "AntigravityNotifications"
        const val CHANNEL_ACTIVITY = "antigravity_activity"
        const val CHANNEL_WATCHER = "antigravity_watcher"

        private val TITLE_ALERT_REGEX = Regex(
            """^\(\d+\)|•|klaar|ready|completed|finished|new message|nieuw bericht""",
            RegexOption.IGNORE_CASE
        )

        fun ensureChannels(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val activityChannel = NotificationChannel(
                CHANNEL_ACTIVITY,
                context.getString(R.string.notification_channel_activity),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_activity_desc)
            }

            val watcherChannel = NotificationChannel(
                CHANNEL_WATCHER,
                context.getString(R.string.notification_channel_watcher),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_watcher_desc)
                setShowBadge(false)
            }

            manager.createNotificationChannel(activityChannel)
            manager.createNotificationChannel(watcherChannel)
        }

        /**
         * Safe JS shim that forwards standard web `new Notification(...)` calls and title updates
         * into the Android notification bridge without interfering with page functionality.
         */
        val INJECTED_NOTIFICATION_JS: String = """
            (function() {
                if (window.__antigravityNotifPatched) return;
                window.__antigravityNotifPatched = true;
                try {
                    if (window.$JS_INTERFACE_NAME) {
                        var NativeNotif = function(title, options) {
                            var body = (options && options.body) ? String(options.body) : '';
                            window.$JS_INTERFACE_NAME.notify(String(title || 'Antigravity'), body);
                            this.close = function() {};
                        };
                        NativeNotif.permission = 'granted';
                        NativeNotif.requestPermission = function(cb) {
                            if (typeof cb === 'function') cb('granted');
                            return Promise.resolve('granted');
                        };
                        window.Notification = NativeNotif;

                        var lastTitle = document.title;
                        var titleEl = document.querySelector('title');
                        if (titleEl && typeof MutationObserver !== 'undefined') {
                            new MutationObserver(function() {
                                if (document.title !== lastTitle) {
                                    lastTitle = document.title;
                                    window.$JS_INTERFACE_NAME.onTitleActivity(lastTitle);
                                }
                            }).observe(titleEl, { childList: true, characterData: true, subtree: true });
                        }
                    }
                } catch (e) {}
            })();
        """.trimIndent()
    }
}
