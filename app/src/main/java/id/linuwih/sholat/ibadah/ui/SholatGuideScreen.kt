package id.linuwih.sholat.ibadah.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import id.linuwih.sholat.ibadah.data.SholatGuideRepository
import id.linuwih.sholat.ibadah.data.SholatStep
import id.linuwih.sholat.quran.data.ArabicFont
import id.linuwih.sholat.quran.data.MushafStyle
import id.linuwih.sholat.quran.data.QuranPrefs
import id.linuwih.sholat.quran.ui.toFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SholatGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SholatGuideRepository(context.applicationContext) }
    var steps by remember { mutableStateOf<List<SholatStep>?>(null) }

    // Ikut pilihan font Arab user di layar baca Qur'an supaya tampilannya konsisten.
    // Panduan sholat memakai teks Arab bertanda lengkap gaya Indonesia, jadi font-nya
    // diselaraskan dengan gaya mushaf pilihan user di layar Qur'an -- bukan dipilih terpisah.
    val prefs = remember { QuranPrefs(context.applicationContext) }
    val arabicFont = remember { prefs.arabicFont }
    val mushafStyle = remember { prefs.mushafStyle }

    LaunchedEffect(Unit) { steps = repository.getSteps() }

    id.linuwih.sholat.ui.theme.PaperPage {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                colors = id.linuwih.sholat.ui.theme.paperTopBarColors(),
                title = { Text(stringResource(R.string.sholat_guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        val list = steps
        if (list == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Text(
                    stringResource(R.string.sholat_guide_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                HorizontalDivider()
            }

            itemsIndexed(list) { index, step ->
                StepRow(index = index + 1, step = step, arabicFont = arabicFont, mushafStyle = mushafStyle)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
    }
}

@Composable
private fun StepRow(index: Int, step: SholatStep, arabicFont: ArabicFont, mushafStyle: MushafStyle) {
    // Langkah tertutup secara default: seluruh panduan ini panjang, dan menampilkan semuanya
    // sekaligus membuat urutan sholatnya justru tenggelam di antara teks bacaan.
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$index",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                step.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(start = 64.dp, end = 20.dp, bottom = 16.dp)) {
                if (step.note.isNotBlank()) {
                    Text(
                        step.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                step.readings.forEachIndexed { i, reading ->
                    if (i > 0) Spacer(Modifier.height(20.dp))
                    if (reading.label.isNotBlank()) {
                        Text(
                            reading.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    val arabicBase = MaterialTheme.typography.headlineSmall
                    Text(
                        reading.arabic,
                        style = arabicBase.copy(
                            // Jarak baris Arab dilonggarkan seperti di layar baca Qur'an —
                            // harakat menjulang jauh di atas/bawah huruf dasarnya.
                            lineHeight = arabicBase.fontSize * 1.9f,
                            fontFamily = arabicFont.toFontFamily(mushafStyle),
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        reading.latin,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(reading.translation, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
