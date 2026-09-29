package id.linuwih.sholat.prayer.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import id.linuwih.sholat.MainActivity
import id.linuwih.sholat.R
import id.linuwih.sholat.prayer.data.AdhanSound

/**
 * Memutar adzan sebagai AUDIO sungguhan, bukan sebagai suara notifikasi.
 *
 * Kenapa tidak menumpang suara notification channel seperti sebelumnya: suara notifikasi berhenti
 * begitu notifikasinya dibuka atau dibuang user. Untuk dentingan pendek itu tidak terasa, tapi
 * adzan berdurasi ~2,5 menit jadi selalu terpotong — persis gejala yang dilaporkan. Notification
 * channel memang dirancang untuk isyarat pendek, bukan rekaman panjang.
 *
 * Dengan MediaPlayer di foreground service, adzan berbunyi sampai selesai tak peduli notifikasinya
 * disentuh atau tidak, dan notifikasinya berubah peran jadi tombol "Berhenti" — bukan sumber suara.
 *
 * Atribut USAGE_ALARM dipertahankan supaya tetap terdengar saat HP dalam mode hening.
 */
class AdhanPlaybackService : Service() {

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var prayerLabel: String = ""

    // True begitu MediaPlayer benar-benar mulai berbunyi. Membedakan "gagal SEBELUM sempat
    // bersuara" (perlu fallback notifikasi biasa) dari "berhenti SETELAH sempat bersuara"
    // (selesai normal, atau error di tengah jalan -- adzan sudah sempat terdengar, tidak
    // perlu fallback).
    private var startedPlaying = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
            ACTION_PLAY -> {
                prayerLabel = intent.getStringExtra(EXTRA_PRAYER_LABEL).orEmpty()
                startForegroundNotification(prayerLabel)
                startAdhan()
            }
            else -> stopEverything()
        }
        return START_NOT_STICKY
    }

    private fun startAdhan() {
        // Hentikan sisa pemutaran sebelumnya (mis. dua waktu berdekatan) sebelum mulai lagi.
        releasePlayer()
        startedPlaying = false

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        requestAudioFocus(attributes)

        player = MediaPlayer().apply {
            setAudioAttributes(attributes)
            setOnCompletionListener { stopEverything() }
            setOnErrorListener { _, _, _ ->
                if (!startedPlaying) notifyFallback()
                stopEverything()
                true
            }
            runCatching {
                setDataSource(this@AdhanPlaybackService, AdhanSound.resolve(this@AdhanPlaybackService)!!)
                prepareAsync()
                setOnPreparedListener {
                    startedPlaying = true
                    it.start()
                }
            }.onFailure {
                // AdhanPlaybackService.play() di caller SUDAH lama return true begitu
                // startForegroundService() berhasil secara SINKRON -- PrayerAlarmReceiver tidak
                // pernah tahu MediaPlayer gagal siap DI SINI (berkas custom rusak/hilang, storage
                // penuh, dll) dan tidak akan jatuh ke notifikasi biasa dengan sendirinya. Fallback
                // itu harus ditampilkan dari titik ini, atau waktu sholat lewat dalam diam total.
                notifyFallback()
                stopEverything()
            }
        }
    }

    /** Tampilkan notifikasi biasa sebagai pengganti adzan penuh yang gagal berbunyi sama sekali. */
    private fun notifyFallback() {
        runCatching { NotificationHelper.showPrayerNotification(this, prayerLabel, FALLBACK_NOTIFICATION_ID) }
    }

    private fun requestAudioFocus(attributes: AudioAttributes) {
        val manager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Musik/video yang sedang berjalan dijeda selama adzan, lalu boleh lanjut sesudahnya.
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .build()
            focusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun abandonAudioFocus() {
        val manager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { manager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }

    private fun releasePlayer() {
        runCatching {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        }
        player = null
    }

    private fun stopEverything() {
        releasePlayer()
        abandonAudioFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        releasePlayer()
        abandonAudioFocus()
        super.onDestroy()
    }

    private fun startForegroundNotification(prayerLabel: String) {
        ensureChannel()

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AdhanPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.prayer_notif_title_format, prayerLabel))
            .setContentText(getString(R.string.prayer_notif_text_format, prayerLabel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            // Membuka notifikasi TIDAK lagi menghentikan adzan — suaranya kini milik MediaPlayer,
            // bukan milik notifikasi ini.
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** Channel ini sengaja TANPA suara: suaranya diputar MediaPlayer, bukan oleh channel. */
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.adhan_playback_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.adhan_playback_channel_description)
                setSound(null, null)
                enableVibration(true)
            },
        )
    }

    companion object {
        const val ACTION_PLAY = "id.linuwih.sholat.ADHAN_PLAY"
        const val ACTION_STOP = "id.linuwih.sholat.ADHAN_STOP"
        private const val EXTRA_PRAYER_LABEL = "prayer_label"
        private const val CHANNEL_ID = "adhan_playback"
        private const val NOTIFICATION_ID = 4001

        // ID notifikasi terpisah dari NOTIFICATION_ID (yang dipakai notifikasi foreground
        // service adzan) supaya fallback tidak menimpa/tertimpa notifikasi lain yang sedang
        // tampil. Beda pula dari REMINDER_REQUEST_OFFSET (200) dan slot 0-4 milik
        // PrayerAlarmScheduler, jadi tidak akan bentrok dengan notifikasi waktu sholat biasa.
        private const val FALLBACK_NOTIFICATION_ID = 4002

        /**
         * Return true kalau PERMINTAAN memulai service berhasil dikirim (startForegroundService
         * tidak melempar exception secara sinkron — mis. karena pembatasan mulai FGS dari
         * background sejak Android 12, yang alarm presisi biasanya dikecualikan darinya).
         *
         * PENTING: true DI SINI TIDAK BERARTI adzan benar-benar akan berbunyi. Kegagalan yang
         * terjadi SETELAH service mulai (MediaPlayer gagal siap, berkas suara rusak/hilang) sudah
         * di luar jangkauan return value ini — itu terjadi asinkron, setelah fungsi ini selesai.
         * Fallback untuk kasus itu ditangani AdhanPlaybackService sendiri lewat notifyFallback(),
         * BUKAN oleh pemanggil di sini.
         */
        fun play(context: Context, prayerLabel: String): Boolean = runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AdhanPlaybackService::class.java).apply {
                    action = ACTION_PLAY
                    putExtra(EXTRA_PRAYER_LABEL, prayerLabel)
                },
            )
            true
        }.getOrDefault(false)
    }
}
