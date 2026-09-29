package id.linuwih.sholat.prayer.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import id.linuwih.sholat.MainActivity
import id.linuwih.sholat.R
import id.linuwih.sholat.prayer.data.AdhanSound
import id.linuwih.sholat.prayer.data.NotificationStyle
import id.linuwih.sholat.prayer.data.PrayerPrefs

object NotificationHelper {
    private const val CHANNEL_PREFIX = "prayer_reminders"

    /**
     * Versi DEFINISI channel — dinaikkan tiap kali perilaku channel diubah DI KODE.
     *
     * Ini menutup bug yang membuat adzan tetap bisu di mode hening setelah aplikasi diperbarui.
     * Suara & getar sebuah channel tidak bisa diubah setelah channel dibuat, dan [ensureChannel]
     * berhenti lebih awal kalau channel-nya sudah ada. Dulu nomor versi hanya naik saat USER
     * mengubah setelan — jadi bagi yang sudah memasang aplikasi sebelumnya, channel lama (dengan
     * atribut suara kelas notifikasi, yang memang diredam mode hening) tetap dipakai dan
     * perubahan ke kelas alarm tidak pernah berlaku.
     *
     * Naikkan angka ini setiap mengubah [buildChannel].
     */
    private const val CHANNEL_DEFINITION_VERSION = 2

    fun channelId(context: Context): String =
        "${CHANNEL_PREFIX}_d${CHANNEL_DEFINITION_VERSION}_v${PrayerPrefs(context).channelVersion}"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = channelId(context)
        if (manager.getNotificationChannel(id) == null) {
            manager.createNotificationChannel(buildChannel(context, id))
        }
        deleteStaleChannels(manager, keep = id)
    }

    /**
     * Dipanggil setelah user mengubah gaya notifikasi / nada. Menaikkan versi channel, membuat
     * channel baru dengan setelan terbaru, lalu menghapus channel lama supaya tidak menumpuk di
     * daftar setelan notifikasi sistem.
     */
    fun applySettingsChange(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val prefs = PrayerPrefs(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        prefs.channelVersion = prefs.channelVersion + 1
        val id = channelId(context)
        manager.createNotificationChannel(buildChannel(context, id))
        deleteStaleChannels(manager, keep = id)
    }

    /** Buang semua channel adzan selain yang sedang dipakai — termasuk sisa versi definisi lama. */
    private fun deleteStaleChannels(manager: NotificationManager, keep: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        manager.notificationChannels
            .map { it.id }
            .filter { it.startsWith(CHANNEL_PREFIX) && it != keep }
            .forEach { manager.deleteNotificationChannel(it) }
    }

    /**
     * Bunyikan notifikasi contoh sekarang juga. Adzan hanya berbunyi saat waktu sholat tiba, jadi
     * tanpa ini satu-satunya cara memastikan setelan suara benar-benar berlaku adalah menunggu
     * berjam-jam — dan kalau ternyata salah, baru ketahuan setelah terlewat.
     */
    fun showTestNotification(context: Context) {
        show(
            context = context,
            notificationId = TEST_NOTIFICATION_ID,
            title = context.getString(R.string.notif_test_title),
            body = context.getString(R.string.notif_test_body),
        )
    }

    private const val TEST_NOTIFICATION_ID = 999

    private fun buildChannel(context: Context, id: String): NotificationChannel {
        val prefs = PrayerPrefs(context)
        val style = prefs.notificationStyle
        val importance = if (style == NotificationStyle.SILENT) {
            NotificationManager.IMPORTANCE_LOW
        } else {
            NotificationManager.IMPORTANCE_HIGH
        }

        return NotificationChannel(id, context.getString(R.string.prayer_notif_channel_name), importance).apply {
            description = context.getString(R.string.prayer_notif_channel_description)
            enableVibration(style != NotificationStyle.SILENT)
            when (style) {
                NotificationStyle.SOUND -> {
                    // USAGE_ALARM, bukan USAGE_NOTIFICATION_EVENT: suara kelas notifikasi ikut
                    // dibisukan saat HP dalam mode hening/getar, sementara adzan diharapkan tetap
                    // terdengar — persis seperti alarm weker. Konsekuensinya memang mengabaikan
                    // mode hening, jadi user tetap bisa mematikannya lewat setelan aplikasi ini
                    // (gaya notifikasi Getar/Senyap, atau matikan per waktu sholat).
                    setSound(
                        AdhanSound.resolve(context),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                }
                // Getar & senyap: tanpa suara sama sekali.
                NotificationStyle.VIBRATE, NotificationStyle.SILENT -> setSound(null, null)
            }
        }
    }

    fun showPrayerNotification(context: Context, prayerLabel: String, notificationId: Int) {
        show(
            context = context,
            notificationId = notificationId,
            title = context.getString(R.string.prayer_notif_title_format, prayerLabel),
            body = context.getString(R.string.prayer_notif_text_format, prayerLabel),
        )
    }

    /** Pengingat "sebentar lagi masuk waktu X" — dijadwalkan N menit sebelum waktunya. */
    fun showReminderNotification(context: Context, prayerLabel: String, minutesBefore: Int, notificationId: Int) {
        show(
            context = context,
            notificationId = notificationId,
            title = context.getString(R.string.prayer_reminder_title_format, prayerLabel),
            body = context.getString(R.string.prayer_reminder_text_format, minutesBefore, prayerLabel),
        )
    }

    private fun show(context: Context, notificationId: Int, title: String, body: String) {
        ensureChannel(context)

        val openAppIntent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val style = PrayerPrefs(context).notificationStyle
        val builder = NotificationCompat.Builder(context, channelId(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(
                if (style == NotificationStyle.SILENT) NotificationCompat.PRIORITY_LOW
                else NotificationCompat.PRIORITY_HIGH,
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Di bawah Android 8 belum ada notification channel — suara/getar diatur per-notifikasi.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            when (style) {
                NotificationStyle.SOUND -> builder
                    .setSound(AdhanSound.resolve(context), android.media.AudioManager.STREAM_ALARM)
                    .setVibrate(longArrayOf(0, 400, 200, 400))
                NotificationStyle.VIBRATE -> builder.setSound(null).setVibrate(longArrayOf(0, 400, 200, 400))
                NotificationStyle.SILENT -> builder.setSound(null).setVibrate(null)
            }
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}
