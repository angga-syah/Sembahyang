package id.linuwih.sholat.prayer.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.batoulapps.adhan2.Prayer
import id.linuwih.sholat.prayer.data.PrayerCalculationMethod
import id.linuwih.sholat.prayer.data.PrayerPrefs
import id.linuwih.sholat.prayer.data.PrayerTimesFactory
import java.util.Calendar

/**
 * Jadwalkan notifikasi 5 waktu sholat hari ini via AlarmManager (bukan foreground
 * service/polling — hemat baterai, lihat plan). Dipanggil ulang tiap hari (lewat alarm
 * "reschedule" sendiri) dan setelah reboot (lihat BootReceiver).
 *
 * Catatan: scheduler ini butuh koordinat terakhir yang tersimpan di PrayerPrefs
 * (diisi PrayerViewModel saat lokasi berhasil didapat) — kalau belum pernah ada
 * lokasi, tidak ada yang bisa dijadwalkan (no-op).
 */
object PrayerAlarmScheduler {
    const val ACTION_PRAYER_ALARM = "id.linuwih.sholat.ACTION_PRAYER_ALARM"
    const val ACTION_RESCHEDULE = "id.linuwih.sholat.ACTION_RESCHEDULE"
    const val EXTRA_PRAYER_ID = "prayer_id"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_MINUTES_BEFORE = "minutes_before"

    // Alarm pengingat pakai blok request code terpisah supaya tidak menimpa alarm waktu sholat
    // itu sendiri (PendingIntent dibedakan oleh request code).
    private const val REMINDER_REQUEST_OFFSET = 200

    // Prayer.name() dipakai sebagai identitas stabil (key SharedPreferences, extra Intent) —
    // TIDAK berubah walau bahasa aplikasi berganti. Label yang ditampilkan ke user selalu
    // di-resolve terpisah lewat PrayerLabels, di saat notifikasi benar-benar tampil.
    private val PRAYER_SLOTS = listOf(
        0 to Prayer.FAJR,
        1 to Prayer.DHUHR,
        2 to Prayer.ASR,
        3 to Prayer.MAGHRIB,
        4 to Prayer.ISHA,
    )
    private const val RESCHEDULE_REQUEST_CODE = 100

    /** 5 waktu sholat, urut sama seperti dipakai untuk penjadwalan — dipakai UI Setelan notifikasi. */
    val prayers: List<Prayer> = PRAYER_SLOTS.map { it.second }

    /**
     * Sejak Android 12 alarm presisi butuh izin khusus yang TIDAK otomatis diberikan. Tanpa izin
     * itu penjadwalan turun ke alarm tidak presisi, yang di mode Doze bisa meleset beberapa menit
     * — untuk pengingat waktu sholat itu masalah nyata, dan selama ini terjadi diam-diam tanpa
     * user pernah tahu. Dipakai UI Setelan untuk menampilkan peringatan.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun scheduleForToday(context: Context) {
        val prefs = PrayerPrefs(context)
        val lat = prefs.lastLatitude
        val lng = prefs.lastLongitude
        if (lat == null || lng == null) return

        val method = prefs.calculationMethod ?: PrayerCalculationMethod.defaultFor(lat, lng)
        val times = PrayerTimesFactory.forToday(context, lat, lng, method)
        val now = System.currentTimeMillis()

        val instants = listOf(times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha)
        val reminderMinutes = prefs.reminderMinutesBefore
        PRAYER_SLOTS.forEachIndexed { index, (requestCode, prayer) ->
            val triggerAt = instants[index].toEpochMilliseconds()
            val enabled = prefs.isNotificationEnabled(prayer)
            if (triggerAt > now && enabled) {
                scheduleAlarm(context, triggerAt, requestCode, prayer)
            } else {
                // Waktu sudah lewat, atau notifikasi untuk waktu ini dimatikan user —
                // batalkan alarm lama kalau ada (mis. baru saja dimatikan lewat Setelan).
                cancelAlarm(context, requestCode)
            }

            val reminderCode = REMINDER_REQUEST_OFFSET + requestCode
            val reminderAt = triggerAt - reminderMinutes * 60_000L
            if (reminderMinutes > 0 && enabled && reminderAt > now) {
                scheduleAlarm(context, reminderAt, reminderCode, prayer, minutesBefore = reminderMinutes)
            } else {
                cancelAlarm(context, reminderCode)
            }
        }

        scheduleReschedule(context)
    }

    /** Batalkan semua alarm terjadwal (dipanggil saat notifikasi dimatikan total dari Setelan). */
    fun cancelAll(context: Context) {
        PRAYER_SLOTS.forEach { (requestCode, _) ->
            cancelAlarm(context, requestCode)
            cancelAlarm(context, REMINDER_REQUEST_OFFSET + requestCode)
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply { action = ACTION_RESCHEDULE }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RESCHEDULE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply { action = ACTION_PRAYER_ALARM }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleAlarm(
        context: Context,
        triggerAtMillis: Long,
        requestCode: Int,
        prayer: Prayer,
        minutesBefore: Int = 0,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
            putExtra(EXTRA_PRAYER_ID, prayer.name)
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    private fun scheduleReschedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_RESCHEDULE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            RESCHEDULE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setAlarm(alarmManager, nextMidnight, pendingIntent)
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // Izin exact alarm belum diberikan (API 31+) — tetap jadwalkan, cuma tidak presisi ke detik.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
