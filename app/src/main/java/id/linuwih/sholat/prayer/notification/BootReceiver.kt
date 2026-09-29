package id.linuwih.sholat.prayer.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Alarm AlarmManager hilang setelah reboot — jadwalkan ulang dari lokasi terakhir tersimpan.
 *
 * Tidak hanya ACTION_BOOT_COMPLETED: sebagian ROM (Xiaomi/HTC dan turunannya — justru merek yang
 * sudah diperingatkan di layar Tentang) mengirim QUICKBOOT_POWERON saat menyalakan ulang lewat
 * mode cepat, dan tidak pernah mengirim BOOT_COMPLETED. Tanpa menangani keduanya, adzan bisa diam
 * seharian setelah restart tanpa user tahu sebabnya.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            -> PrayerAlarmScheduler.scheduleForToday(context)
        }
    }
}
