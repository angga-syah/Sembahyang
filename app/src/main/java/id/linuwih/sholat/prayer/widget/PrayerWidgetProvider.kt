package id.linuwih.sholat.prayer.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.batoulapps.adhan2.Prayer
import id.linuwih.sholat.MainActivity
import id.linuwih.sholat.R
import id.linuwih.sholat.prayer.data.PrayerCalculationMethod
import id.linuwih.sholat.prayer.data.PrayerLabels
import id.linuwih.sholat.prayer.data.PrayerPrefs
import id.linuwih.sholat.prayer.data.PrayerTimesFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Widget home screen sederhana pakai RemoteViews klasik (bukan Glance) — supaya tidak
 * nambah dependency baru, sesuai prinsip "ringan" (lihat plan). Isi widget dihitung dari
 * lokasi terakhir tersimpan (PrayerPrefs), sama seperti PrayerAlarmScheduler.
 *
 * Hitung mundur memakai Chronometer mode mundur yang berdetak sendiri di launcher, sehingga
 * selalu tepat ke detik tanpa widget disegarkan berkala. Widget hanya perlu disegarkan saat
 * target berganti (tepat di waktu sholat berikutnya) — dijadwalkan sendiri lewat satu alarm,
 * jadi tetap jalan walau notifikasi waktu itu dimatikan, dan cuma ~6 alarm sehari (aman dari
 * kuota alarm Android 12+ untuk app yang jarang dibuka).
 */
class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
        scheduleNextRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // Jam/zona waktu diubah: basis Chronometer (elapsedRealtime) tetap, tapi target waktu
            // sholat dalam jam dinding bergeser — hitung ulang.
            ACTION_REFRESH,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> updateAll(context)

            else -> super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        // Widget terakhir dilepas dari layar — jangan biarkan alarm refresh tersisa.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(refreshPendingIntent(context))
    }

    companion object {
        private const val ACTION_REFRESH = "id.linuwih.sholat.ACTION_WIDGET_REFRESH"

        /** Dipanggil dari PrayerAlarmReceiver (tiap prayer alarm/reschedule), BootReceiver &
         * PrayerViewModel (tiap kali lokasi/metode baru dihitung) supaya widget selalu segar. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { id -> updateWidget(context, manager, id) }
            scheduleNextRefresh(context)
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer)
            val result = computeNextPrayer(context)
            if (result != null) {
                views.setTextViewText(R.id.widget_prayer_name, result.label)
                views.setTextViewText(R.id.widget_prayer_time, result.timeText)
                bindCountdown(context, views, result.targetMillis)
            } else {
                views.setTextViewText(R.id.widget_prayer_name, context.getString(R.string.widget_open_app))
                views.setTextViewText(R.id.widget_prayer_time, context.getString(R.string.widget_detect_location))
                // Sembunyikan pill hitung mundur — sebelumnya tampil sebagai kapsul kosong.
                views.setChronometer(R.id.widget_prayer_chrono, SystemClock.elapsedRealtime(), null, false)
                views.setViewVisibility(R.id.widget_prayer_chrono, View.GONE)
                views.setViewVisibility(R.id.widget_prayer_countdown, View.GONE)
            }
            // Ketuk widget = buka aplikasi (sebelumnya widget tidak bereaksi sama sekali,
            // padahal saat lokasi belum ada teksnya justru menyuruh "Buka app").
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun bindCountdown(context: Context, views: RemoteViews, targetMillis: Long) {
            val remaining = targetMillis - System.currentTimeMillis()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val base = SystemClock.elapsedRealtime() + remaining
                views.setChronometer(
                    R.id.widget_prayer_chrono,
                    base,
                    context.getString(R.string.widget_countdown_format),
                    true,
                )
                views.setChronometerCountDown(R.id.widget_prayer_chrono, true)
                views.setViewVisibility(R.id.widget_prayer_chrono, View.VISIBLE)
                views.setViewVisibility(R.id.widget_prayer_countdown, View.GONE)
            } else {
                // Android < 7: Chronometer belum bisa mundur — tampilkan teks statis (menit),
                // disegarkan tiap pergantian waktu sholat & updatePeriodMillis.
                views.setTextViewText(R.id.widget_prayer_countdown, formatCountdown(context, remaining))
                views.setViewVisibility(R.id.widget_prayer_countdown, View.VISIBLE)
                views.setViewVisibility(R.id.widget_prayer_chrono, View.GONE)
            }
        }

        private fun openAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun refreshPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, PrayerWidgetProvider::class.java).apply { action = ACTION_REFRESH }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /**
         * Satu alarm tepat di waktu sholat berikutnya untuk mengganti target hitung mundur.
         * RTC non-wakeup: kalau layar mati saat itu, alarm dikirim begitu perangkat bangun —
         * tidak ada yang melihat widget selama layar mati, jadi tidak perlu membangunkan.
         */
        private fun scheduleNextRefresh(context: Context) {
            val target = computeNextPrayer(context)?.targetMillis ?: return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = target + 1_000L
            val pendingIntent = refreshPendingIntent(context)
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExact(AlarmManager.RTC, triggerAt, pendingIntent)
            } else {
                alarmManager.setWindow(AlarmManager.RTC, triggerAt, 60_000L, pendingIntent)
            }
        }

        private data class WidgetContent(val label: String, val timeText: String, val targetMillis: Long)

        private fun formatCountdown(context: Context, remainingMillis: Long): String {
            if (remainingMillis <= 0) return context.getString(R.string.prayer_countdown_now)
            val totalMinutes = remainingMillis / 60_000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                totalMinutes < 1 -> context.getString(R.string.prayer_countdown_soon)
                hours > 0 -> context.getString(R.string.prayer_countdown_hm, hours, minutes)
                else -> context.getString(R.string.prayer_countdown_m, minutes)
            }
        }

        private fun computeNextPrayer(context: Context): WidgetContent? {
            val prefs = PrayerPrefs(context)
            val lat = prefs.lastLatitude ?: return null
            val lng = prefs.lastLongitude ?: return null
            val method = prefs.calculationMethod ?: PrayerCalculationMethod.defaultFor(lat, lng)

            val now = kotlin.time.Clock.System.now()
            val todayTimes = PrayerTimesFactory.forToday(context, lat, lng, method)
            // Terbit bukan waktu sholat — widget berlabel "Sholat berikutnya", jadi setelah Subuh
            // yang ditampilkan langsung Dzuhur, bukan "Terbit".
            val next = todayTimes.nextPrayer(now).let { if (it == Prayer.SUNRISE) Prayer.DHUHR else it }

            val (label, instant) = if (next == Prayer.NONE) {
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowTimes = PrayerTimesFactory.forDate(context, lat, lng, method, tomorrow)
                PrayerLabels.label(context, Prayer.FAJR) to tomorrowTimes.fajr
            } else {
                // Tanpa !! — kode ini berjalan di dalam BroadcastReceiver, dan pengecualian di
                // sini bukan sekadar layar kosong melainkan crash proses yang terlihat user.
                val instant = todayTimes.timeForPrayer(next) ?: return null
                PrayerLabels.label(context, next) to instant
            }

            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }
            return WidgetContent(
                label = label,
                timeText = formatter.format(Date(instant.toEpochMilliseconds())),
                targetMillis = instant.toEpochMilliseconds(),
            )
        }
    }
}
