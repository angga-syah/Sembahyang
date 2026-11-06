package id.linuwih.sholat.quran.audio

import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File

/** Pengaturan satu sesi hafalan: rentang ayat, jumlah ulangan, jeda menirukan, kecepatan. */
data class HafalanPlan(
    val surah: Int,
    val fromAyah: Int,
    val toAyah: Int,
    /** Berapa kali tiap ayat diputar berturut-turut. */
    val ayahRepeat: Int,
    /** Berapa kali seluruh rentang diulang; 0 = terus sampai dihentikan. */
    val rangeRepeat: Int,
    /** Jeda setelah tiap ayat = durasi ayat x faktor ini (0 = tanpa jeda). */
    val gapFactor: Float,
    val speed: Float,
)

/** Posisi pemutaran yang dilaporkan ke UI. Nomor ulangan dihitung mulai 1. */
data class HafalanPosition(
    val ayah: Int,
    val ayahRepeatIndex: Int,
    val rangeRepeatIndex: Int,
    /** true selama jeda — giliran user menirukan ayat yang barusan diputar. */
    val inGap: Boolean,
)

/**
 * Pemutar khusus hafalan: tiap ayat diulang N kali, diselingi jeda sepanjang durasi ayat itu
 * supaya user sempat menirukan, lalu seluruh rentang diulang M kali (atau terus).
 *
 * Terpisah dari [AyahAudioPlayer] karena alurnya berbeda total: AyahAudioPlayer memutar satu
 * surah lurus lalu lanjut ke surah berikutnya, sedangkan di sini satu rentang kecil diputar
 * berulang-ulang dan TIDAK pernah melompat ke surah lain.
 */
class RepeatingAyahPlayer {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var plan: HafalanPlan? = null
    private var fileFor: ((Int) -> File)? = null
    private var onPosition: ((HafalanPosition) -> Unit)? = null
    private var onFinished: (() -> Unit)? = null

    private var ayah = 0
    private var ayahRep = 1
    private var rangeRep = 1
    private var consecutiveFailures = 0

    /** Langkah berikutnya yang tertunda selama jeda — dijalankan ulang saat lanjut dari jeda. */
    private var pendingNext: Runnable? = null
    private var paused = false

    val isPlaying: Boolean
        get() = !paused && (pendingNext != null || runCatching { mediaPlayer?.isPlaying == true }.getOrDefault(false))

    fun start(
        plan: HafalanPlan,
        fileFor: (ayah: Int) -> File,
        onPosition: (HafalanPosition) -> Unit,
        onFinished: () -> Unit,
    ) {
        stop()
        this.plan = plan
        this.fileFor = fileFor
        this.onPosition = onPosition
        this.onFinished = onFinished
        ayah = plan.fromAyah
        ayahRep = 1
        rangeRep = 1
        consecutiveFailures = 0
        paused = false
        playCurrent()
    }

    fun pause() {
        paused = true
        pendingNext?.let { handler.removeCallbacks(it) }
        runCatching { mediaPlayer?.takeIf { it.isPlaying }?.pause() }
    }

    fun resume() {
        if (!paused) return
        paused = false
        val next = pendingNext
        if (next != null) {
            // Dijeda di tengah jeda menirukan: langsung lanjut ke ayat berikutnya.
            pendingNext = null
            next.run()
        } else {
            runCatching { mediaPlayer?.start() }
        }
    }

    fun stop() {
        pendingNext?.let { handler.removeCallbacks(it) }
        pendingNext = null
        runCatching {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        }
        mediaPlayer = null
        plan = null
        paused = false
    }

    private fun playCurrent() {
        val plan = plan ?: return
        val file = fileFor?.invoke(ayah) ?: return
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener { mp ->
                if (plan.speed != 1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    runCatching { mp.playbackParams = mp.playbackParams.setSpeed(plan.speed) }
                }
                if (paused) {
                    // User menekan jeda saat file sedang disiapkan — tahan, resume() akan start().
                    runCatching { if (mp.isPlaying) mp.pause() }
                } else {
                    mp.start()
                }
                consecutiveFailures = 0
                report(inGap = false)
            }
            player.setOnCompletionListener { mp ->
                val durationMs = runCatching { mp.duration }.getOrDefault(0)
                mp.release()
                mediaPlayer = null
                val gapMs = (durationMs / plan.speed * plan.gapFactor).toLong()
                if (gapMs > 0) {
                    report(inGap = true)
                    val next = Runnable {
                        pendingNext = null
                        advance()
                    }
                    pendingNext = next
                    handler.postDelayed(next, gapMs)
                } else {
                    advance()
                }
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.release()
                mediaPlayer = null
                skipAfterFailure()
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            player.release()
            mediaPlayer = null
            skipAfterFailure()
        }
    }

    /** File rusak/hilang: lewati ayat itu, tapi berhenti total kalau gagal terus-menerus. */
    private fun skipAfterFailure() {
        consecutiveFailures++
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            finish()
            return
        }
        ayahRep = plan?.ayahRepeat ?: 1
        advance()
    }

    private fun advance() {
        val plan = plan ?: return
        when {
            ayahRep < plan.ayahRepeat -> ayahRep++
            ayah < plan.toAyah -> {
                ayah++
                ayahRep = 1
            }
            plan.rangeRepeat == 0 || rangeRep < plan.rangeRepeat -> {
                rangeRep++
                ayah = plan.fromAyah
                ayahRep = 1
            }
            else -> {
                finish()
                return
            }
        }
        playCurrent()
    }

    private fun finish() {
        val callback = onFinished
        stop()
        callback?.invoke()
    }

    private fun report(inGap: Boolean) {
        onPosition?.invoke(HafalanPosition(ayah, ayahRep, rangeRep, inGap))
    }

    private companion object {
        const val MAX_CONSECUTIVE_FAILURES = 3
    }
}
