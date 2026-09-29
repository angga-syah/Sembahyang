package id.linuwih.sholat.quran.audio

import android.media.MediaPlayer
import java.io.File

/** Pemutar berurutan file lokal ayat-per-ayat (bukan streaming — hanya file yang sudah diunduh). */
class AyahAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun playSequence(files: List<File>, onAyahChanged: (index: Int) -> Unit, onFinished: () -> Unit) {
        stop()
        playAt(files, 0, onAyahChanged, onFinished)
    }

    /** true kalau ada ayat yang sedang benar-benar berbunyi (bukan sekadar player masih hidup). */
    val isPlaying: Boolean
        get() = runCatching { mediaPlayer?.isPlaying == true }.getOrDefault(false)

    fun pause() {
        runCatching { mediaPlayer?.takeIf { it.isPlaying }?.pause() }
    }

    /** Lanjut dari posisi terakhir; tidak memulai ulang ayat dari awal. */
    fun resume() {
        runCatching { mediaPlayer?.start() }
    }

    fun stop() {
        // release() (dan isPlaying) bisa lempar IllegalStateException kalau player ini sudah
        // di-release sebelumnya (mis. baru saja selesai wajar lewat setOnCompletionListener) —
        // bungkus semuanya, bukan cuma bagian isPlaying.
        runCatching {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        }
        mediaPlayer = null
    }

    private fun playAt(
        files: List<File>,
        index: Int,
        onAyahChanged: (Int) -> Unit,
        onFinished: () -> Unit,
        consecutiveFailures: Int = 0,
    ) {
        // Kalau banyak file berturut-turut gagal (mis. folder audio terhapus di tengah sesi),
        // berhenti — bukan terus merambat sampai ujung daftar.
        if (index >= files.size || consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            mediaPlayer = null
            onFinished()
            return
        }
        val player = MediaPlayer()
        mediaPlayer = player

        // Satu ayat gagal diputar (file korup/hilang) TIDAK boleh dianggap "surah selesai".
        // Sebelumnya jalur error memanggil onFinished(), yang di ViewModel berarti pemicu
        // lanjut-otomatis ke surah BERIKUTNYA — jadi file rusak di tengah surah bikin
        // pemutaran melompat surah, bukan sekadar melewati satu ayat.
        fun skipToNext() = playAt(files, index + 1, onAyahChanged, onFinished, consecutiveFailures + 1)

        try {
            player.setDataSource(files[index].absolutePath)
            player.setOnPreparedListener {
                it.start()
                onAyahChanged(index)
            }
            player.setOnCompletionListener {
                it.release()
                mediaPlayer = null
                playAt(files, index + 1, onAyahChanged, onFinished, consecutiveFailures = 0)
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.release()
                mediaPlayer = null
                skipToNext()
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            player.release()
            mediaPlayer = null
            skipToNext()
        }
    }

    private companion object {
        const val MAX_CONSECUTIVE_FAILURES = 5
    }
}
