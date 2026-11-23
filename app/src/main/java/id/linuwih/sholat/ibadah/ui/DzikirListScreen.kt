package id.linuwih.sholat.ibadah.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.linuwih.sholat.R
import id.linuwih.sholat.ibadah.data.DzikirItem
import id.linuwih.sholat.ibadah.data.DzikirRepository
import id.linuwih.sholat.ibadah.data.DzikirSet
import id.linuwih.sholat.ui.components.CompactSelector
import id.linuwih.sholat.quran.data.ArabicFont
import id.linuwih.sholat.quran.data.QuranPrefs
import id.linuwih.sholat.quran.ui.amiriQuranFontFamily
import id.linuwih.sholat.quran.ui.lpmqIsepMisbahFontFamily
import id.linuwih.sholat.ui.theme.OrnamentDivider
import id.linuwih.sholat.ui.theme.PaperPage
import id.linuwih.sholat.ui.theme.PrintSerif
import id.linuwih.sholat.ui.theme.paperTopBarColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DzikirListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { DzikirRepository(context.applicationContext) }

    var selectedSet by remember { mutableStateOf(DzikirSet.SUGRO_PAGI) }
    var items by remember(selectedSet) { mutableStateOf<List<DzikirItem>?>(null) }

    LaunchedEffect(selectedSet) {
        items = repository.getItems(selectedSet)
    }

    // Font Arab dzikir dipilih PER BACAAN (lihat dzikirArabicFont), kecuali user memilih font
    // sistem di layar Qur'an.
    val prefs = remember { QuranPrefs(context.applicationContext) }
    val useSystemFont = remember { prefs.arabicFont.resolve(prefs.mushafStyle) == ArabicFont.SYSTEM }

    PaperPage {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = paperTopBarColors(),
                title = { Text(stringResource(R.string.dzikir_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SetDropdown(
                selected = selectedSet,
                onSelected = { selectedSet = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            val list = items
            if (list == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(list) { item ->
                        if (item.isHeader) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            )
                        } else {
                            DzikirRow(item, if (useSystemFont) null else dzikirArabicFont(item.arabic))
                            OrnamentDivider()
                        }
                    }
                }
            }
        }
    }
    }
}

/**
 * Teks Al-Ma'tsurat bercampur dua gaya penulisan, dan tidak ada satu font bawaan yang
 * meng-cover keduanya (dicek dari cmap font terhadap seluruh berkas dzikir):
 * - Gaya Kemenag (mayoritas): LPMQ lengkap. Sebagian bacaan memakai U+06D5 sebagai pembawa
 *   tanda waqaf ("ەۙ") yang hanya ditangani benar oleh LPMQ; di Amiri karakter itu jatuh ke
 *   font lain dan tampil sebagai huruf liar.
 * - Gaya Utsmani: memakai alif wasla (U+0671) yang tidak ada di LPMQ, jadi pakai Amiri Quran.
 * Kalau huruf jatuh ke font lain, sambungan huruf & posisi harakat ikut berantakan.
 */
private fun dzikirArabicFont(arabic: String): FontFamily =
    if (arabic.contains('ٱ')) amiriQuranFontFamily else lpmqIsepMisbahFontFamily

/**
 * Teks gaya Utsmani di berkas dzikir (turunan Tanzil) menulis tanwin dhammah terbuka sebagai
 * U+065E, karakter yang tidak ada di Amiri Quran -- akibatnya huruf + tanwinnya digambar font
 * sistem dan tampak lain sendiri (mis. "شَرِيكٞ" di Al-Isra 111). Titik kode yang tepat untuk
 * tanda itu adalah U+08F1 (ARABIC OPEN DAMMATAN), yang dimiliki Amiri. Hanya untuk tampilan;
 * berkas data tidak diubah.
 */
private fun normalizeForAmiri(arabic: String): String = arabic.replace('ٞ', 'ࣱ')

@Composable
private fun DzikirRow(item: DzikirItem, arabicFamily: FontFamily?) {
    // 24dp kiri-kanan: harakat & tanda waqaf di ujung baris Arab menjorok keluar batas glyph.
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(item.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.repeat > 1) {
                Text(
                    stringResource(R.string.dzikir_repeat_format, item.repeat),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        val arabicBase = MaterialTheme.typography.headlineSmall
        Text(
            if (arabicFamily === amiriQuranFontFamily) normalizeForAmiri(item.arabic) else item.arabic,
            // Jarak baris 1.9x seperti layar baca Qur'an: rasio Latin bawaan (~1.33x) membuat
            // harakat baris bawah menabrak harakat baris atas begitu bacaan panjang wrap.
            style = arabicBase.copy(lineHeight = arabicBase.fontSize * 1.9f, fontFamily = arabicFamily),
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        val body = MaterialTheme.typography.bodyMedium.copy(fontFamily = PrintSerif)
        Text(item.latin, style = body, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(10.dp))
        Text(item.translation, style = body)
    }
}

@Composable
private fun dzikirSetLabel(set: DzikirSet): String = stringResource(
    when (set) {
        DzikirSet.SUGRO_PAGI -> R.string.dzikir_set_sugro_pagi
        DzikirSet.SUGRO_PETANG -> R.string.dzikir_set_sugro_petang
        DzikirSet.KUBRO_PAGI -> R.string.dzikir_set_kubro_pagi
        DzikirSet.KUBRO_PETANG -> R.string.dzikir_set_kubro_petang
    },
)

@Composable
private fun SetDropdown(selected: DzikirSet, onSelected: (DzikirSet) -> Unit, modifier: Modifier = Modifier) {
    CompactSelector(
        label = stringResource(R.string.dzikir_set_label),
        selected = selected,
        options = DzikirSet.entries,
        optionLabel = { dzikirSetLabel(it) },
        onSelected = onSelected,
        modifier = modifier,
    )
}
