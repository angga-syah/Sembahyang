package id.linuwih.sholat.prayer.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.batoulapps.adhan2.Prayer
import id.linuwih.sholat.prayer.data.NotificationStyle
import id.linuwih.sholat.prayer.data.PrayerLabels
import id.linuwih.sholat.prayer.data.PrayerPrefs
import id.linuwih.sholat.prayer.widget.PrayerWidgetProvider

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PrayerAlarmScheduler.ACTION_PRAYER_ALARM -> {
                val prayerId = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_ID) ?: return
                val prayer = runCatching { Prayer.valueOf(prayerId) }.getOrNull() ?: return
                val notificationId = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, 0)
                val minutesBefore = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_MINUTES_BEFORE, 0)
                // Label di-resolve di sini (saat notifikasi benar-benar tampil), bukan saat
                // dijadwalkan, supaya selalu ikut bahasa aplikasi yang berlaku saat itu.
                val label = PrayerLabels.label(context, prayer)
                when {
                    minutesBefore > 0 ->
                        NotificationHelper.showReminderNotification(context, label, minutesBefore, notificationId)

                    // Adzan penuh diputar lewat pemutar audio, bukan sebagai suara notifikasi —
                    // suara notifikasi berhenti begitu notifikasinya disentuh, sehingga adzan
                    // sepanjang ~2,5 menit selalu terpotong. Kalau service gagal dijalankan
                    // (pembatasan foreground service), jatuh ke notifikasi biasa supaya waktu
                    // sholat tetap terkabar walau tanpa adzan penuh.
                    PrayerPrefs(context).notificationStyle == NotificationStyle.SOUND ->
                        if (!AdhanPlaybackService.play(context, label)) {
                            NotificationHelper.showPrayerNotification(context, label, notificationId)
                        }

                    else -> NotificationHelper.showPrayerNotification(context, label, notificationId)
                }
                PrayerWidgetProvider.updateAll(context)
            }
            PrayerAlarmScheduler.ACTION_RESCHEDULE -> {
                PrayerAlarmScheduler.scheduleForToday(context)
                PrayerWidgetProvider.updateAll(context)
            }
        }
    }
}
