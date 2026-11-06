package id.linuwih.sholat.quran.hafalan

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.linuwih.sholat.MainActivity
import id.linuwih.sholat.R
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Pengingat hafalan harian di jam pilihan user (default mati). Isinya: berapa ayat jatuh tempo
 * murajaah dan sisa target ayat baru hari ini. Kalau tidak ada yang perlu dikerjakan, notifikasi
 * tidak ditampilkan sama sekali — pengingat yang selalu muncul cepat diabaikan.
 *
 * Alarm tidak presisi (jendela 15 menit) sudah cukup: ini ajakan, bukan waktu sholat.
 */
object HafalanReminder {
    private const val REQUEST_CODE = 4100
    private const val CHANNEL_ID = "hafalan_reminder"
    private const val NOTIFICATION_ID = 4101
    private const val WINDOW_MS = 15 * 60_000L
    const val ACTION_REMIND = "id.linuwih.sholat.ACTION_HAFALAN_REMIND"

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context)
        alarmManager.cancel(pendingIntent)
        val hour = HafalanPrefs(context).reminderHour
        if (hour < 0) return

        val now = System.currentTimeMillis()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, next, WINDOW_MS, pendingIntent)
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, HafalanReminderReceiver::class.java).apply { action = ACTION_REMIND }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal suspend fun showIfNeeded(context: Context) {
        val dao = HafalanDatabase.getInstance(context).dao()
        val today = todayEpochDay()
        val due = dao.dueCount(today)
        val target = HafalanPrefs(context).dailyTarget
        val newLeft = (target - dao.memorizedOn(today)).coerceAtLeast(0)
        if (due == 0 && newLeft == 0) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)

        val text = when {
            due > 0 && newLeft > 0 -> context.getString(R.string.hafalan_reminder_both, due, newLeft)
            due > 0 -> context.getString(R.string.hafalan_reminder_review, due)
            else -> context.getString(R.string.hafalan_reminder_new, newLeft)
        }
        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.hafalan_reminder_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.hafalan_reminder_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}

class HafalanReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != HafalanReminder.ACTION_REMIND) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                HafalanReminder.showIfNeeded(context)
                HafalanReminder.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
