package id.linuwih.sholat.quran.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.linuwih.sholat.R

/**
 * Panduan kaidah tajwid -- dokumentasi rule yang sama persis dengan yang dihitung
 * TajweedEngine.kt saat membaca Qur'an. Swatch warna diambil dari tajweedColorFor() (bukan
 * ditulis ulang manual) supaya tidak bisa menyimpang dari warna sebenarnya di layar baca.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TajweedGuideScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tajwid_guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding)) {
            item {
                Text(
                    stringResource(R.string.tajwid_guide_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(TAJWEED_GUIDE_ENTRIES) { entry ->
                TajweedGuideCard(entry)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TajweedGuideCard(entry: TajweedGuideEntry) {
    val color = tajweedColorFor(entry.colorRuleName)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Swatch bulat kecil -- warnanya diambil dari tajweedColorFor() yang sama
                // dipakai layar baca, bukan ditulis ulang manual, supaya tidak bisa menyimpang.
                Spacer(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (color != Color.Unspecified) color else MaterialTheme.colorScheme.outline),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(entry.titleRes), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(entry.descriptionRes), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.tajwid_guide_huruf_format, stringResource(entry.hurufRes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
