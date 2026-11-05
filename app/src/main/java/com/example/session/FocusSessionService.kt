package com.example.session

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val NOTIFICATION_ID = 1
private const val CHANNEL_RUNNING = "running_timer"
private const val CHANNEL_DONE = "times_up"

/**
 * Keeps the process alive for as long as a session needs it, whatever else is on the phone,
 * and shows what's going on in one notification. It goes away by itself after that.
 */
class FocusSessionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_RUNNING, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Running timer")
                .setShowBadge(false)
                .build()
        )
        // It pops up, but makes no sound of its own: the alert is already playing
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_DONE, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Time's up")
                .setSound(null, null)
                .setVibrationEnabled(false)
                .setShowBadge(false)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val session = FocusSession.get(this)
        // Android requires this on every start, even if the session has just ended
        show(session.currentNotice())
        if (watching == null) {
            watching = scope.launch {
                session.notices.collect { notice ->
                    if (notice == SessionNotice.None) {
                        ServiceCompat.stopForeground(this@FocusSessionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        show(notice)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun show(notice: SessionNotice) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notificationFor(notice), type)
    }

    private fun notificationFor(notice: SessionNotice): Notification {
        val builder = when (notice) {
            is SessionNotice.Running -> NotificationCompat.Builder(this, CHANNEL_RUNNING)
                .setContentTitle(notice.label)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setSilent(true)
                // The phone counts this down by itself, so the app doesn't have to wake up
                .setWhen(notice.endAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
            is SessionNotice.Done -> NotificationCompat.Builder(this, CHANNEL_DONE)
                .setContentTitle("${notice.label} is done")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(0, "Silence", silenceIntent())
            SessionNotice.None -> NotificationCompat.Builder(this, CHANNEL_RUNNING)
                .setContentTitle(getString(R.string.app_name))
                .setSilent(true)
        }
        return builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun openAppIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun silenceIntent(): PendingIntent {
        return PendingIntent.getBroadcast(
            this,
            1,
            Intent(this, SessionReceiver::class.java).setAction(SessionReceiver.ACTION_SILENCE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
