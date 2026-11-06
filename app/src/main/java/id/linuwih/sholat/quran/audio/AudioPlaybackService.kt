package id.linuwih.sholat.quran.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import id.linuwih.sholat.MainActivity
import id.linuwih.sholat.R
import id.linuwih.sholat.quran.data.QuranRepository
import id.linuwih.sholat.quran.data.Surah
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Memutar murottal di foreground service supaya bacaan tidak terputus saat user pindah tab,
 * menutup layar surah, atau mematikan layar. Sebelumnya pemutar hidup di dalam ViewModel, jadi
 * audio ikut mati begitu layar surah dibuang.
 *
 * Foreground service dipakai (bukan service biasa) karena Android akan mematikan service latar
 * biasa saat app tidak terlihat — dan notifikasi permanennya memang disyaratkan sistem, sekaligus
 * jadi tempat tombol jeda/berhenti.
 *
 * CATATAN JUJUR: ini SATU-SATUNYA proses latar belakang di aplikasi, dan hanya hidup selama
 * murottal diputar — bukan pemantauan terus-menerus. Klaim di layar Tentang sudah disesuaikan.
 */
class AudioPlaybackService : Service() {

    private val player = AyahAudioPlayer()
    private val hafalanPlayer = RepeatingAyahPlayer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var audioRepository: AudioRepository
    private lateinit var quranRepository: QuranRepository

    private var surahs: List<Surah> = emptyList()
    private var currentSurah: Int = 0
    private var currentReciter: Reciter = Reciter.ALAFASY

    override fun onCreate() {
        super.onCreate()
        audioRepository = AudioRepository(this)
        quranRepository = QuranRepository(this)
        // getSurahs() hanya membaca metadata surah (±19 KB), bukan seluruh teks ayat.
        scope.launch { surahs = quranRepository.getSurahs() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val surah = intent.getIntExtra(EXTRA_SURAH, 0)
                val reciterName = intent.getStringExtra(EXTRA_RECITER)
                val reciter = reciterName?.let { name ->
                    runCatching { Reciter.valueOf(name) }.getOrNull()
                } ?: Reciter.ALAFASY
                startPlayback(surah, reciter)
            }
            ACTION_PLAY_HAFALAN -> {
                val reciter = intent.getStringExtra(EXTRA_RECITER)?.let { name ->
                    runCatching { Reciter.valueOf(name) }.getOrNull()
                } ?: Reciter.ALAFASY
                val plan = HafalanPlan(
                    surah = intent.getIntExtra(EXTRA_SURAH, 0),
                    fromAyah = intent.getIntExtra(EXTRA_FROM, 1),
                    toAyah = intent.getIntExtra(EXTRA_TO, 1),
                    ayahRepeat = intent.getIntExtra(EXTRA_AYAH_REPEAT, 1).coerceAtLeast(1),
                    rangeRepeat = intent.getIntExtra(EXTRA_RANGE_REPEAT, 1).coerceAtLeast(0),
                    gapFactor = intent.getFloatExtra(EXTRA_GAP, 0f),
                    speed = intent.getFloatExtra(EXTRA_SPEED, 1f),
                )
                startHafalan(plan, reciter)
            }
            ACTION_TOGGLE -> togglePause()
            ACTION_STOP -> stopEverything()
            else -> stopEverything()
        }
        return START_NOT_STICKY
    }

    private fun startPlayback(surahNumber: Int, reciter: Reciter) {
        hafalanPlayer.stop()
        scope.launch {
            if (surahs.isEmpty()) surahs = quranRepository.getSurahs()
            val surah = surahs.find { it.number == surahNumber } ?: return@launch stopEverything()

            currentSurah = surahNumber
            currentReciter = reciter
            PlaybackState.update {
                it.copy(reciter = reciter, surah = surahNumber, ayah = 1, isPlaying = true, isActive = true, hafalan = null)
            }
            promoteToForeground()

            val files = (1..surah.totalVerses).map { audioRepository.localFile(reciter, surahNumber, it) }
            player.playSequence(
                files = files,
                onAyahChanged = { index ->
                    PlaybackState.update { it.copy(ayah = index + 1, isPlaying = true) }
                    updateNotification()
                },
                onFinished = { continueOrStop(surahNumber) },
            )
        }
    }

    /**
     * Sesi hafalan: satu rentang ayat diulang sesuai rencana, lalu berhenti — tidak pernah
     * lanjut ke surah berikutnya seperti murottal biasa.
     */
    private fun startHafalan(plan: HafalanPlan, reciter: Reciter) {
        player.stop()
        currentSurah = plan.surah
        currentReciter = reciter
        val start = HafalanPosition(plan.fromAyah, 1, 1, inGap = false)
        PlaybackState.update {
            it.copy(
                reciter = reciter,
                surah = plan.surah,
                ayah = plan.fromAyah,
                isPlaying = true,
                isActive = true,
                hafalan = HafalanSession(plan, start),
            )
        }
        promoteToForeground()
        scope.launch { if (surahs.isEmpty()) surahs = quranRepository.getSurahs() }
        hafalanPlayer.start(
            plan = plan,
            fileFor = { ayah -> audioRepository.localFile(reciter, plan.surah, ayah) },
            onPosition = { position ->
                PlaybackState.update {
                    it.copy(ayah = position.ayah, hafalan = HafalanSession(plan, position))
                }
                updateNotification()
            },
            onFinished = { stopEverything() },
        )
    }

    /**
     * Lanjut ke surah TERUNDUH berikutnya dengan qori yang sama.
     *
     * Sebelumnya hanya surah tepat berikutnya (justFinished + 1) yang diperiksa, jadi satu surah
     * yang belum terunduh langsung menghentikan seluruh sesi — padahal unduhan jarang berurutan
     * rapi: unduh per-juz memotong surah di batas juz, dan user juga bisa memilih surah satu per
     * satu. Sekarang dicari maju sampai ketemu surah yang benar-benar lengkap terunduh.
     */
    private fun continueOrStop(justFinished: Int) {
        scope.launch {
            if (surahs.isEmpty()) surahs = quranRepository.getSurahs()
            val reciter = currentReciter
            val next = kotlinx.coroutines.withContext(Dispatchers.IO) {
                surahs.asSequence()
                    .filter { it.number > justFinished }
                    .sortedBy { it.number }
                    .firstOrNull { audioRepository.isSurahDownloaded(reciter, it.number, it.totalVerses) }
            }
            if (next != null) startPlayback(next.number, reciter) else stopEverything()
        }
    }

    private fun togglePause() {
        val hafalanMode = PlaybackState.snapshot.value.hafalan != null
        val playing = if (hafalanMode) hafalanPlayer.isPlaying else player.isPlaying
        if (playing) {
            if (hafalanMode) hafalanPlayer.pause() else player.pause()
            PlaybackState.update { it.copy(isPlaying = false) }
        } else {
            if (hafalanMode) hafalanPlayer.resume() else player.resume()
            PlaybackState.update { it.copy(isPlaying = true) }
        }
        updateNotification()
    }

    private fun stopEverything() {
        player.stop()
        hafalanPlayer.stop()
        PlaybackState.reset()
        stopForegroundCompat()
        stopSelf()
    }

    override fun onDestroy() {
        player.stop()
        hafalanPlayer.stop()
        PlaybackState.reset()
        scope.cancel()
        super.onDestroy()
    }

    // ---- notifikasi ----

    private fun promoteToForeground() {
        ensureChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        if (!PlaybackState.snapshot.value.isActive) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_channel_name),
                // LOW: notifikasi kontrol pemutaran tidak boleh berbunyi/bergetar tiap ganti ayat.
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.playback_channel_description)
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(): android.app.Notification {
        val state = PlaybackState.snapshot.value
        val surahName = surahs.find { it.number == state.surah }?.transliteration
            ?: getString(R.string.quran_surah_fallback, state.surah ?: 0)

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(surahName)
            .setContentText(notificationText(state))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(state.isPlaying)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(
                0,
                getString(if (state.isPlaying) R.string.action_pause else R.string.action_play),
                servicePendingIntent(ACTION_TOGGLE, 1),
            )
            .addAction(0, getString(R.string.action_stop), servicePendingIntent(ACTION_STOP, 2))
            .build()
    }

    private fun notificationText(state: PlaybackSnapshot): String {
        val session = state.hafalan ?: return getString(R.string.quran_ayah_number_format, state.ayah ?: 1)
        val position = session.position
        return if (position.inGap) {
            getString(R.string.hafalan_notif_your_turn, position.ayah)
        } else {
            getString(
                R.string.hafalan_notif_playing,
                position.ayah,
                position.ayahRepeatIndex,
                session.plan.ayahRepeat,
            )
        }
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, AudioPlaybackService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_PLAY = "id.linuwih.sholat.PLAYBACK_PLAY"
        const val ACTION_PLAY_HAFALAN = "id.linuwih.sholat.PLAYBACK_PLAY_HAFALAN"
        const val ACTION_TOGGLE = "id.linuwih.sholat.PLAYBACK_TOGGLE"
        const val ACTION_STOP = "id.linuwih.sholat.PLAYBACK_STOP"
        private const val EXTRA_SURAH = "surah"
        private const val EXTRA_RECITER = "reciter"
        private const val EXTRA_FROM = "from"
        private const val EXTRA_TO = "to"
        private const val EXTRA_AYAH_REPEAT = "ayah_repeat"
        private const val EXTRA_RANGE_REPEAT = "range_repeat"
        private const val EXTRA_GAP = "gap"
        private const val EXTRA_SPEED = "speed"
        private const val CHANNEL_ID = "quran_playback"
        private const val NOTIFICATION_ID = 3001

        fun play(context: Context, surah: Int, reciter: Reciter) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_SURAH, surah)
                putExtra(EXTRA_RECITER, reciter.name)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun playHafalan(context: Context, plan: HafalanPlan, reciter: Reciter) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_PLAY_HAFALAN
                putExtra(EXTRA_SURAH, plan.surah)
                putExtra(EXTRA_RECITER, reciter.name)
                putExtra(EXTRA_FROM, plan.fromAyah)
                putExtra(EXTRA_TO, plan.toAyah)
                putExtra(EXTRA_AYAH_REPEAT, plan.ayahRepeat)
                putExtra(EXTRA_RANGE_REPEAT, plan.rangeRepeat)
                putExtra(EXTRA_GAP, plan.gapFactor)
                putExtra(EXTRA_SPEED, plan.speed)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun toggle(context: Context) = send(context, ACTION_TOGGLE)

        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String) {
            // Jangan pakai startForegroundService untuk perintah kontrol: kalau service sudah
            // mati, sistem menuntut startForeground() dalam 5 detik dan akan meng-crash app.
            runCatching {
                context.startService(Intent(context, AudioPlaybackService::class.java).apply { this.action = action })
            }
        }
    }
}
