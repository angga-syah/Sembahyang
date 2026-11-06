package id.linuwih.sholat.quran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Star as FilledStar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Star as OutlinedStar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.linuwih.sholat.R
import id.linuwih.sholat.quran.audio.Reciter
import id.linuwih.sholat.quran.data.Ayah
import id.linuwih.sholat.quran.data.ArabicFont
import id.linuwih.sholat.quran.data.LatinStyle
import id.linuwih.sholat.quran.data.MushafStyle
import id.linuwih.sholat.quran.data.ReadingFontSize
import id.linuwih.sholat.quran.data.Surah
import id.linuwih.sholat.quran.data.TranslationLanguage
import id.linuwih.sholat.quran.hafalan.HafalanViewModel

@Composable
fun QuranScreen(
    modifier: Modifier = Modifier,
    viewModel: QuranViewModel = viewModel(),
    hafalanViewModel: HafalanViewModel = viewModel(),
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list", modifier = modifier) {
        composable("list") {
            SurahListScreen(
                viewModel = viewModel,
                onOpenSurah = { number -> navController.navigate("surah/$number") },
                onOpenAyah = { surah, ayah -> navController.navigate("surah/$surah?ayah=$ayah") },
                onOpenAudioManager = { navController.navigate("audio") },
                onOpenSearch = { navController.navigate("search") },
                onOpenHafalan = { navController.navigate("hafalan") },
            )
        }
        composable("hafalan") {
            HafalanHomeScreen(
                quranViewModel = viewModel,
                viewModel = hafalanViewModel,
                onBack = { navController.popBackStack() },
                onOpenSurah = { number, review -> navController.navigate("hafalan/$number?review=$review") },
            )
        }
        composable(
            route = "hafalan/{number}?review={review}",
            arguments = listOf(
                navArgument("number") { type = NavType.IntType },
                navArgument("review") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { backStackEntry ->
            HafalanSessionScreen(
                surahNumber = backStackEntry.arguments?.getInt("number") ?: 1,
                review = backStackEntry.arguments?.getBoolean("review") ?: false,
                quranViewModel = viewModel,
                viewModel = hafalanViewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable("audio") {
            AudioDownloadScreen(onBack = { navController.popBackStack() })
        }
        composable("search") {
            QuranSearchScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenAyah = { surah, ayah -> navController.navigate("surah/$surah?ayah=$ayah") },
                onOpenSurah = { surah -> navController.navigate("surah/$surah") },
            )
        }
        composable(
            route = "surah/{number}?ayah={ayah}",
            arguments = listOf(
                navArgument("number") { type = NavType.IntType },
                // -1 = buka dari atas (tidak melompat ke ayat tertentu).
                navArgument("ayah") { type = NavType.IntType; defaultValue = -1 },
            ),
        ) { backStackEntry ->
            val number = backStackEntry.arguments?.getInt("number") ?: 1
            val targetAyah = backStackEntry.arguments?.getInt("ayah")?.takeIf { it > 0 }
            SurahDetailScreen(
                surahNumber = number,
                targetAyah = targetAyah,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToSurah = { target ->
                    // Ganti entry saat ini (bukan numpuk) — supaya tombol back & lompat otomatis
                    // saat audio lanjut ke surah berikut tetap kembali ke daftar surah, bukan
                    // menumpuk riwayat tiap surah yang dilewati.
                    navController.navigate("surah/$target") {
                        popUpTo("surah/{number}?ayah={ayah}") { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahListScreen(
    viewModel: QuranViewModel,
    onOpenSurah: (Int) -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onOpenAudioManager: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHafalan: () -> Unit,
) {
    val surahs by viewModel.surahs.collectAsState()
    val lastRead by viewModel.lastRead.collectAsState()
    var juzAmmaOnly by remember { mutableStateOf(false) }

    // Juz Amma = Juz 30, selalu mulai dari surah 78 (An-Naba).
    val filtered = remember(surahs, juzAmmaOnly) {
        surahs.filter { !juzAmmaOnly || it.number >= 78 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quran_title)) },
                actions = {
                    IconButton(onClick = onOpenHafalan) {
                        Icon(
                            Icons.Filled.School,
                            contentDescription = stringResource(R.string.hafalan_title),
                        )
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.quran_search_title),
                        )
                    }
                    IconButton(onClick = onOpenAudioManager) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = stringResource(R.string.audio_manager_description),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (surahs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            lastRead?.let { lr ->
                val surahName = surahs.find { it.number == lr.surahNumber }?.transliteration
                    ?: stringResource(R.string.quran_surah_fallback, lr.surahNumber)
                // Permukaan tonal tipis, bukan Card ber-bayangan dengan padding ganda:
                // ini penanda cepat, bukan elemen yang perlu menonjol setebal itu.
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        // Lanjut baca langsung melompat ke ayat terakhir yang dibaca.
                        .clickable { onOpenAyah(lr.surahNumber, lr.ayahNumber) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.quran_continue_reading),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.quran_last_read_format, surahName, lr.ayahNumber),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Hanya SATU pencarian di tab ini. Dulu ada dua: kolom di sini (hanya menyaring
            // nama surah) dan ikon kaca pembesar di app bar (mencari isi ayat) — dua kotak cari
            // untuk hal berbeda di satu layar jelas membingungkan. Sekarang keduanya disatukan
            // di layar pencarian, yang mencari nama surah sekaligus isi ayat.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.FilterChip(
                    selected = juzAmmaOnly,
                    onClick = { juzAmmaOnly = !juzAmmaOnly },
                    label = { Text(stringResource(R.string.quran_juz_amma_chip)) },
                )
            }

            LazyColumn {
                items(filtered) { surah ->
                    SurahRow(surah = surah, onClick = { onOpenSurah(surah.number) })
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SurahRow(surah: Surah, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Nomor tanpa lingkaran berwarna — 114 lingkaran solid membuat daftar terasa ramai.
        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "${surah.number}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(surah.transliteration, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.quran_surah_meta_format,
                    surah.translationName,
                    surah.totalVerses,
                    stringResource(if (surah.type == "meccan") R.string.quran_type_meccan else R.string.quran_type_medinan),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            surah.arabicName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahDetailScreen(
    surahNumber: Int,
    targetAyah: Int?,
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onNavigateToSurah: (Int) -> Unit,
) {
    val surahs by viewModel.surahs.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    var ayahs by remember(surahNumber) { mutableStateOf<List<Ayah>?>(null) }
    val listState = rememberLazyListState()

    val audioState by viewModel.audioState.collectAsState()
    val latinStyle by viewModel.latinStyle.collectAsState()
    val translationLanguage by viewModel.translationLanguage.collectAsState()
    val arabicFont by viewModel.arabicFont.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val tajweedEnabled by viewModel.tajweedEnabled.collectAsState()
    val mushafStyle by viewModel.mushafStyle.collectAsState()
    val surah = surahs.find { it.number == surahNumber }

    // reloadTrigger ikut jadi key: ganti gaya mushaf harus memuat ulang teks ayat yang tampil,
    // bukan menunggu user keluar-masuk surah dulu.
    val reloadTrigger by viewModel.ayahReloadTrigger.collectAsState()
    LaunchedEffect(surahNumber, reloadTrigger) {
        ayahs = viewModel.ayahsFor(surahNumber)
    }

    LaunchedEffect(surahNumber, surah?.totalVerses) {
        surah?.let { viewModel.checkAudioStatus(surahNumber, it.totalVerses) }
    }

    // Lompat ke ayat target — dipakai hasil pencarian & kartu "Lanjutkan membaca". Ditunggu
    // sampai daftar ayat selesai dimuat, karena sebelum itu tidak ada item untuk di-scroll.
    // +1 pada indeks: item ke-0 di LazyColumn adalah kartu kontrol audio, bukan ayat.
    LaunchedEffect(targetAyah, ayahs) {
        val list = ayahs ?: return@LaunchedEffect
        val target = targetAyah ?: return@LaunchedEffect
        val index = list.indexOfFirst { it.number == target }
        if (index >= 0) listState.scrollToItem(index + 1)
    }

    // Ikuti perpindahan surah otomatis HANYA kalau layar ini memang sedang menampilkan surah
    // yang barusan diputar. Sejak audio berlanjut di background, user bisa saja sengaja membuka
    // surah lain sambil mendengarkan — tanpa syarat ini dia akan "ditarik" pindah layar.
    var lastSeenPlaying by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(audioState.playingSurah) {
        val playing = audioState.playingSurah
        if (playing != null && playing != surahNumber && lastSeenPlaying == surahNumber) {
            onNavigateToSurah(playing)
        }
        lastSeenPlaying = playing
    }

    // Audio TIDAK lagi dihentikan saat layar ini ditutup — pemutaran dipegang foreground
    // service supaya murottal terus berbunyi walau user pindah tab atau layar dimatikan.
    // Menghentikannya hanya lewat tombol Berhenti di layar ini atau di notifikasi.

    // -1 karena item ke-0 adalah kartu kontrol audio, bukan ayat. Tanpa koreksi ini posisi
    // "terakhir dibaca" yang tersimpan selalu meleset satu ayat ke depan.
    val currentAyahNumber = rememberUpdatedState(
        ayahs?.getOrNull((listState.firstVisibleItemIndex - 1).coerceAtLeast(0))?.number ?: 1,
    )
    DisposableEffect(surahNumber) {
        onDispose { viewModel.markLastRead(surahNumber, currentAyahNumber.value) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surah?.transliteration ?: stringResource(R.string.quran_surah_fallback, surahNumber)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToSurah(surahNumber - 1) },
                        enabled = surahNumber > 1,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = stringResource(R.string.action_previous_surah))
                    }
                    IconButton(
                        onClick = { onNavigateToSurah(surahNumber + 1) },
                        enabled = surahNumber < 114,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = stringResource(R.string.action_next_surah))
                    }
                    DisplaySettingsMenu(
                        mushafStyle = mushafStyle,
                        onMushafStyleSelected = viewModel::setMushafStyle,
                        latinStyle = latinStyle,
                        translationLanguage = translationLanguage,
                        arabicFont = arabicFont,
                        fontSize = fontSize,
                        tajweedEnabled = tajweedEnabled,
                        onLatinStyleSelected = viewModel::setLatinStyle,
                        onTranslationLanguageSelected = viewModel::setTranslationLanguage,
                        onArabicFontSelected = viewModel::setArabicFont,
                        onFontSizeSelected = viewModel::setFontSize,
                        onTajweedToggled = viewModel::setTajweedEnabled,
                    )
                },
            )
        },
    ) { padding ->
        val list = ayahs
        if (list == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                AudioControlBar(
                    state = audioState,
                    onReciterSelected = { r -> surah?.let { viewModel.setReciter(r, surahNumber, it.totalVerses) } },
                    onDownload = { surah?.let { viewModel.downloadAudio(surahNumber, it.totalVerses) } },
                    onPlay = { surah?.let { viewModel.playAudio(surahNumber, it.totalVerses) } },
                    onStop = { viewModel.stopAudio() },
                    onTogglePause = { viewModel.togglePlayPause() },
                    onDelete = { viewModel.deleteAudio(surahNumber) },
                )
                HorizontalDivider()
            }
            items(list) { ayah ->
                val isBookmarked = bookmarks.any { it.surahNumber == surahNumber && it.ayahNumber == ayah.number }
                AyahRow(
                    ayah = ayah,
                    latinStyle = latinStyle,
                    translationLanguage = translationLanguage,
                    arabicFont = arabicFont,
                    mushafStyle = mushafStyle,
                    fontSize = fontSize,
                    tajweedEnabled = tajweedEnabled,
                    isBookmarked = isBookmarked,
                    onToggleBookmark = { viewModel.toggleBookmark(surahNumber, ayah.number, isBookmarked) },
                    onLoadTafsir = { viewModel.tafsirFor(surahNumber, ayah.number) },
                )
                // Garis tipis menjorok, bukan Divider setebal penuh selebar layar: deretan garis
                // penuh antar ayat membuat layar baca terlihat seperti tangga.
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun AudioControlBar(
    state: AudioUiState,
    onReciterSelected: (Reciter) -> Unit,
    onDownload: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit,
) {
    // Dulu ini Card tebal ber-padding ganda dengan OutlinedTextField penuh untuk memilih qori,
    // menghabiskan ruang besar di atas SETIAP surah padahal jarang disentuh. Sekarang satu baris
    // tipis: kontrol utama di kiri, nama qori yang bisa diketuk di tengah, aksi sekunder di kanan.
    var reciterMenu by remember { mutableStateOf(false) }
    val sessionActive = state.playingSurah != null

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                state.isDownloading -> Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = { state.downloadProgress },
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                }
                !state.isDownloaded -> IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = stringResource(R.string.action_download_audio),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                else -> IconButton(onClick = if (sessionActive) onTogglePause else onPlay) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (state.isPlaying) R.string.action_pause else R.string.action_play,
                        ),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.clickable { reciterMenu = true }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.reciter.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.quran_reciter_label),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = reciterMenu, onDismissRequest = { reciterMenu = false }) {
                    Reciter.entries.forEach { reciter ->
                        DropdownMenuItem(
                            text = { Text(reciter.displayName) },
                            onClick = {
                                onReciterSelected(reciter)
                                reciterMenu = false
                            },
                        )
                    }
                }
            }

            // Nomor ayat yang sedang dibaca — informasi sekilas tanpa menambah baris baru.
            if (sessionActive) {
                state.playingAyah?.let {
                    Text(
                        stringResource(R.string.quran_ayah_number_format, it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.action_stop))
                }
            }
            if (state.isDownloaded && !sessionActive) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete_audio),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.isDownloading) {
            LinearProgressIndicator(
                progress = { state.downloadProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun AyahRow(
    ayah: Ayah,
    latinStyle: LatinStyle,
    translationLanguage: TranslationLanguage,
    arabicFont: ArabicFont,
    mushafStyle: MushafStyle,
    fontSize: ReadingFontSize,
    tajweedEnabled: Boolean,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onLoadTafsir: suspend () -> String?,
) {
    val scale = fontSize.scale
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Lencana nomor ringkas menggantikan teks "Ayat N" — nomornya tetap terbaca tapi
            // tidak lagi bersaing perhatian dengan teks Arabnya.
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    "${ayah.number}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.defaultMinSize(minWidth = 26.dp).padding(horizontal = 8.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center,
                )
            }
            IconButton(onClick = onToggleBookmark, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.FilledStar else Icons.Outlined.OutlinedStar,
                    contentDescription = stringResource(if (isBookmarked) R.string.action_remove_bookmark else R.string.action_add_bookmark),
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        val arabicBase = MaterialTheme.typography.headlineSmall
        val arabicFontSize = arabicBase.fontSize * scale
        val arabicStyle = arabicBase.copy(
            fontSize = arabicFontSize,
            // Rasio lineHeight/fontSize bawaan headlineSmall (~1.33x) dibuat untuk teks Latin.
            // Harakat Arab (terutama font Naskh Qur'ani seperti Amiri Quran) menjulang jauh di
            // atas/bawah huruf dasarnya — begitu ayat wrap ke 2 baris atau lebih, harakat baris
            // bawah bertabrakan dengan harakat baris atas kalau rasio ini dipertahankan. Rasio
            // 1.9x di sini dipasang tetap (bukan ikut rasio Latin), lalu diskalakan bareng
            // fontSize supaya proporsinya konsisten di semua ukuran teks.
            lineHeight = arabicFontSize * 1.9f,
            fontFamily = arabicFont.toFontFamily(mushafStyle),
        )
        // Kedua mode memasang span dengan JUMLAH DAN BATAS YANG SAMA PERSIS; yang berbeda
        // hanya nilai warnanya. Lihat buildTajweedAnnotatedString soal kenapa jumlah span
        // berpengaruh pada posisi glyph.
        val plainColor = LocalContentColor.current
        val arabicText = remember(ayah.number, ayah.arabicTextTajweed, tajweedEnabled, plainColor) {
            buildTajweedAnnotatedString(
                text = ayah.arabicTextTajweed,
                rules = TajweedEngine.compute(ayah.arabicTextTajweed),
                tajweedEnabled = tajweedEnabled,
                plainColor = plainColor,
            )
        }
        Text(
            arabicText,
            style = arabicStyle,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        ayah.latinText(latinStyle)?.let {
            Spacer(Modifier.height(10.dp))
            val latinBase = MaterialTheme.typography.bodyMedium
            Text(
                it,
                style = latinBase.copy(fontSize = latinBase.fontSize * scale, lineHeight = latinBase.lineHeight * scale),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        if (translationLanguage != TranslationLanguage.INGGRIS) {
            Spacer(Modifier.height(10.dp))
            val idBase = MaterialTheme.typography.bodyMedium
            Text(
                ayah.translationId,
                style = idBase.copy(fontSize = idBase.fontSize * scale, lineHeight = idBase.lineHeight * scale),
            )
        }
        if (translationLanguage != TranslationLanguage.INDONESIA) {
            Spacer(Modifier.height(10.dp))
            val enBase = MaterialTheme.typography.bodySmall
            Text(
                ayah.translationEn,
                style = enBase.copy(fontSize = enBase.fontSize * scale, lineHeight = enBase.lineHeight * scale),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        TafsirSection(onLoadTafsir = onLoadTafsir, scale = scale)
    }
}

/**
 * Tafsir dimuat MALAS -- baru dipanggil begitu user membuka bagiannya, bukan ikut termuat
 * bersama ayat (lihat TafsirRepository: berkasnya 3+ MB, kebanyakan user tidak membuka tafsir
 * di setiap ayat yang dibaca).
 */
@Composable
private fun TafsirSection(onLoadTafsir: suspend () -> String?, scale: Float) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var tafsir by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    TextButton(onClick = { expanded = !expanded }) {
        Text(stringResource(if (expanded) R.string.quran_tafsir_hide else R.string.quran_tafsir_show))
    }

    if (expanded) {
        LaunchedEffect(Unit) {
            if (!loaded) {
                tafsir = onLoadTafsir()
                loaded = true
            }
        }
        when {
            !loaded -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            tafsir.isNullOrBlank() -> Text(
                stringResource(R.string.quran_tafsir_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                val base = MaterialTheme.typography.bodyMedium
                Text(
                    tafsir.orEmpty(),
                    style = base.copy(fontSize = base.fontSize * scale, lineHeight = base.lineHeight * scale),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DisplaySettingsMenu(
    mushafStyle: MushafStyle,
    onMushafStyleSelected: (MushafStyle) -> Unit,
    latinStyle: LatinStyle,
    translationLanguage: TranslationLanguage,
    arabicFont: ArabicFont,
    fontSize: ReadingFontSize,
    tajweedEnabled: Boolean,
    onLatinStyleSelected: (LatinStyle) -> Unit,
    onTranslationLanguageSelected: (TranslationLanguage) -> Unit,
    onArabicFontSelected: (ArabicFont) -> Unit,
    onFontSizeSelected: (ReadingFontSize) -> Unit,
    onTajweedToggled: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.TextFields, contentDescription = stringResource(R.string.quran_display_settings_description))
    }

    if (!expanded) return

    // Tiap pilihan dulu satu baris penuh berisi teks + ikon centang, sehingga 5 bagian menjadi
    // ~14 baris tinggi yang harus digulir dan dibaca satu per satu. Sekarang tiap bagian jadi
    // sebaris chip: seluruh pilihan terlihat sekaligus, mana yang aktif langsung kelihatan dari
    // chip yang terisi, dan panelnya muat tanpa digulir.
    Dialog(onDismissRequest = { expanded = false }) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    stringResource(R.string.quran_display_settings_description),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))

                ChipGroup(stringResource(R.string.quran_section_mushaf)) {
                    MushafStyle.entries.forEach { style ->
                        ChoiceChip(mushafStyleLabel(style), style == mushafStyle) { onMushafStyleSelected(style) }
                    }
                }
                ChipGroup(stringResource(R.string.quran_section_font_size)) {
                    ReadingFontSize.entries.forEach { size ->
                        ChoiceChip(fontSizeLabel(size), size == fontSize) { onFontSizeSelected(size) }
                    }
                }
                ChipGroup(stringResource(R.string.quran_section_arabic_font)) {
                    ArabicFont.entries.forEach { font ->
                        ChoiceChip(arabicFontLabel(font), font == arabicFont) { onArabicFontSelected(font) }
                    }
                }
                ChipGroup(stringResource(R.string.quran_section_latin)) {
                    LatinStyle.entries.forEach { style ->
                        ChoiceChip(latinStyleLabel(style), style == latinStyle) { onLatinStyleSelected(style) }
                    }
                }
                ChipGroup(stringResource(R.string.quran_section_translation_lang)) {
                    TranslationLanguage.entries.forEach { lang ->
                        ChoiceChip(translationLanguageLabel(lang), lang == translationLanguage) {
                            onTranslationLanguageSelected(lang)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTajweedToggled(!tajweedEnabled) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.quran_tajweed_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.Switch(checked = tajweedEnabled, onCheckedChange = null)
                }

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { expanded = false },
                    modifier = Modifier.align(Alignment.End),
                ) { Text(stringResource(R.string.action_close)) }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(title: String, content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        content = content,
    )
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
    )
}


@Composable
private fun mushafStyleLabel(style: MushafStyle): String = stringResource(
    when (style) {
        MushafStyle.KEMENAG -> R.string.mushaf_style_kemenag
        MushafStyle.UTSMANI -> R.string.mushaf_style_utsmani
    },
)

@Composable
private fun latinStyleLabel(style: LatinStyle): String = stringResource(
    when (style) {
        LatinStyle.INDONESIA -> R.string.latin_style_indonesia
        LatinStyle.INGGRIS -> R.string.latin_style_inggris
        LatinStyle.TIDAK_TAMPIL -> R.string.latin_style_hidden
    },
)

@Composable
private fun translationLanguageLabel(lang: TranslationLanguage): String = stringResource(
    when (lang) {
        TranslationLanguage.INDONESIA -> R.string.translation_lang_id_only
        TranslationLanguage.INGGRIS -> R.string.translation_lang_en_only
        TranslationLanguage.KEDUANYA -> R.string.translation_lang_both
    },
)

@Composable
private fun arabicFontLabel(font: ArabicFont): String = when (font) {
    ArabicFont.OTOMATIS -> stringResource(R.string.arabic_font_auto)
    ArabicFont.SYSTEM -> stringResource(R.string.arabic_font_standard)
    ArabicFont.AMIRI_QURAN -> "Amiri Quran"
    ArabicFont.LPMQ_ISEP_MISBAH -> "LPMQ Isep Misbah"
}

@Composable
private fun fontSizeLabel(size: ReadingFontSize): String = stringResource(
    when (size) {
        ReadingFontSize.KECIL -> R.string.font_size_small
        ReadingFontSize.NORMAL -> R.string.font_size_normal
        ReadingFontSize.BESAR -> R.string.font_size_large
        ReadingFontSize.SANGAT_BESAR -> R.string.font_size_extra_large
    },
)

/**
 * Keadaan kosong yang seragam: ikon redup + satu kalimat penjelas. Sebelumnya daftar yang kosong
 * hanya menampilkan area putih tanpa keterangan, sehingga user tidak tahu apakah datanya memang
 * tidak ada atau aplikasinya gagal memuat.
 */
@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
