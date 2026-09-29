package id.linuwih.sholat.quran.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.linuwih.sholat.quran.audio.AudioPlaybackService
import id.linuwih.sholat.quran.audio.AudioRepository
import id.linuwih.sholat.quran.audio.PlaybackState
import id.linuwih.sholat.quran.audio.Reciter
import id.linuwih.sholat.quran.data.Ayah
import id.linuwih.sholat.quran.data.AyahSearchResult
import id.linuwih.sholat.quran.data.BookmarkEntity
import id.linuwih.sholat.quran.data.LastReadEntity
import id.linuwih.sholat.quran.data.ArabicFont
import id.linuwih.sholat.quran.data.LatinStyle
import id.linuwih.sholat.quran.data.MushafStyle
import id.linuwih.sholat.quran.data.ReadingFontSize
import id.linuwih.sholat.quran.data.QuranDatabase
import id.linuwih.sholat.quran.data.QuranPrefs
import id.linuwih.sholat.quran.data.QuranRepository
import id.linuwih.sholat.quran.data.Surah
import id.linuwih.sholat.quran.data.TafsirRepository
import id.linuwih.sholat.quran.data.TranslationLanguage
import id.linuwih.sholat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AudioUiState(
    val reciter: Reciter = Reciter.ALAFASY,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val isPlaying: Boolean = false,
    val playingSurah: Int? = null,
    val playingAyah: Int? = null,
    val error: String? = null,
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranRepository(application)
    private val tafsirRepository = TafsirRepository(application)
    private val dao = QuranDatabase.getInstance(application).quranDao()
    private val audioRepository = AudioRepository(application)
    private val quranPrefs = QuranPrefs(application)

    private val _audioState = MutableStateFlow(AudioUiState())
    val audioState: StateFlow<AudioUiState> = _audioState.asStateFlow()

    init {
        // Status pemutaran sekarang dimiliki service, bukan ViewModel — cukup diikuti supaya UI
        // tetap sinkron walau pemutaran berlanjut saat layar surah sempat ditutup.
        viewModelScope.launch {
            PlaybackState.snapshot.collect { snapshot ->
                _audioState.value = _audioState.value.copy(
                    isPlaying = snapshot.isPlaying,
                    playingSurah = snapshot.surah,
                    playingAyah = snapshot.ayah,
                )
            }
        }
    }

    private val _latinStyle = MutableStateFlow(quranPrefs.latinStyle)
    val latinStyle: StateFlow<LatinStyle> = _latinStyle.asStateFlow()

    fun setLatinStyle(style: LatinStyle) {
        quranPrefs.latinStyle = style
        _latinStyle.value = style
    }

    private val _translationLanguage = MutableStateFlow(quranPrefs.translationLanguage)
    val translationLanguage: StateFlow<TranslationLanguage> = _translationLanguage.asStateFlow()

    fun setTranslationLanguage(language: TranslationLanguage) {
        quranPrefs.translationLanguage = language
        _translationLanguage.value = language
    }

    private val _arabicFont = MutableStateFlow(quranPrefs.arabicFont)
    val arabicFont: StateFlow<ArabicFont> = _arabicFont.asStateFlow()

    fun setArabicFont(font: ArabicFont) {
        quranPrefs.arabicFont = font
        _arabicFont.value = font
    }

    private val _fontSize = MutableStateFlow(quranPrefs.fontSize)
    val fontSize: StateFlow<ReadingFontSize> = _fontSize.asStateFlow()

    fun setFontSize(size: ReadingFontSize) {
        quranPrefs.fontSize = size
        _fontSize.value = size
    }

    private val _mushafStyle = MutableStateFlow(quranPrefs.mushafStyle)
    val mushafStyle: StateFlow<MushafStyle> = _mushafStyle.asStateFlow()

    fun setMushafStyle(style: MushafStyle) {
        if (style == _mushafStyle.value) return
        quranPrefs.mushafStyle = style
        _mushafStyle.value = style
        viewModelScope.launch {
            // Dua lapis cache harus dibuang: milik repository DAN milik ViewModel ini —
            // kalau salah satu tertinggal, layar surah yang sudah pernah dibuka akan tetap
            // menampilkan teks gaya lama walau setelan sudah berganti.
            ayahCache.clear()
            repository.invalidateAyahs()
            _ayahReloadTrigger.value = _ayahReloadTrigger.value + 1
        }
    }

    /** Dinaikkan setiap gaya mushaf berganti supaya layar surah memuat ulang ayatnya. */
    private val _ayahReloadTrigger = MutableStateFlow(0)
    val ayahReloadTrigger: StateFlow<Int> = _ayahReloadTrigger.asStateFlow()

    private val _tajweedEnabled = MutableStateFlow(quranPrefs.tajweedEnabled)
    val tajweedEnabled: StateFlow<Boolean> = _tajweedEnabled.asStateFlow()

    fun setTajweedEnabled(enabled: Boolean) {
        quranPrefs.tajweedEnabled = enabled
        _tajweedEnabled.value = enabled
    }

    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    val surahs: StateFlow<List<Surah>> = _surahs.asStateFlow()

    val lastRead: StateFlow<LastReadEntity?> = dao.observeLastRead()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val bookmarks: StateFlow<List<BookmarkEntity>> = dao.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val ayahCache = mutableMapOf<Int, List<Ayah>>()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AyahSearchResult>>(emptyList())
    val searchResults: StateFlow<List<AyahSearchResult>> = _searchResults.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private var searchJob: Job? = null

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.trim().length < QuranRepository.MIN_SEARCH_LENGTH) {
            _searchResults.value = emptyList()
            _searching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            // Debounce: tunggu user berhenti mengetik sejenak sebelum memindai 6.236 ayat,
            // supaya tiap ketukan huruf tidak memicu pencarian penuh.
            delay(250)
            _searching.value = true
            _searchResults.value = repository.searchAyahs(query)
            _searching.value = false
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searching.value = false
    }

    init {
        viewModelScope.launch { _surahs.value = repository.getSurahs() }
    }

    suspend fun ayahsFor(surahNumber: Int): List<Ayah> {
        ayahCache[surahNumber]?.let { return it }
        val ayahs = repository.getAyahs(surahNumber)
        ayahCache[surahNumber] = ayahs
        return ayahs
    }

    /** Tafsir Ringkas Kemenag — sama untuk kedua gaya mushaf, tidak perlu invalidasi saat
     *  gaya mushaf berganti (beda dari ayahsFor, lihat TafsirRepository). */
    suspend fun tafsirFor(surahNumber: Int, ayahNumber: Int): String? =
        tafsirRepository.getTafsir(surahNumber, ayahNumber)

    fun markLastRead(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            dao.setLastRead(
                LastReadEntity(surahNumber = surahNumber, ayahNumber = ayahNumber, updatedAt = System.currentTimeMillis()),
            )
        }
    }

    fun toggleBookmark(surahNumber: Int, ayahNumber: Int, currentlyBookmarked: Boolean) {
        viewModelScope.launch {
            if (currentlyBookmarked) {
                dao.removeBookmark(BookmarkEntity(surahNumber, ayahNumber, 0))
            } else {
                dao.addBookmark(BookmarkEntity(surahNumber, ayahNumber, System.currentTimeMillis()))
            }
        }
    }

    fun checkAudioStatus(surahNumber: Int, totalAyahs: Int) {
        _audioState.value = _audioState.value.copy(
            isDownloaded = audioRepository.isSurahDownloaded(_audioState.value.reciter, surahNumber, totalAyahs),
        )
    }

    fun setReciter(reciter: Reciter, surahNumber: Int, totalAyahs: Int) {
        AudioPlaybackService.stop(getApplication())
        _audioState.value = _audioState.value.copy(reciter = reciter)
        checkAudioStatus(surahNumber, totalAyahs)
    }

    fun downloadAudio(surahNumber: Int, totalAyahs: Int) {
        viewModelScope.launch {
            _audioState.value = _audioState.value.copy(isDownloading = true, downloadProgress = 0f, error = null)
            val reciter = _audioState.value.reciter
            val success = audioRepository.downloadSurah(reciter, surahNumber, totalAyahs) { done, total ->
                _audioState.value = _audioState.value.copy(downloadProgress = done.toFloat() / total)
            }
            _audioState.value = _audioState.value.copy(
                isDownloading = false,
                isDownloaded = success,
                error = if (!success) getApplication<Application>().getString(R.string.quran_download_error) else null,
            )
        }
    }

    /**
     * Pemutaran didelegasikan ke AudioPlaybackService (foreground service) supaya murottal
     * tetap jalan saat layar surah ditutup atau layar HP dimatikan. ViewModel tidak lagi
     * memegang MediaPlayer — cuma mengirim perintah dan mengamati PlaybackState.
     */
    fun playAudio(surahNumber: Int, totalAyahs: Int) {
        AudioPlaybackService.play(getApplication(), surahNumber, _audioState.value.reciter)
    }

    fun togglePlayPause() {
        AudioPlaybackService.toggle(getApplication())
    }

    fun stopAudio() {
        AudioPlaybackService.stop(getApplication())
    }

    fun deleteAudio(surahNumber: Int) {
        AudioPlaybackService.stop(getApplication())
        audioRepository.deleteSurah(_audioState.value.reciter, surahNumber)
        _audioState.value = _audioState.value.copy(isDownloaded = false)
    }
}
