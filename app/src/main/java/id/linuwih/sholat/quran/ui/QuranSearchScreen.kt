package id.linuwih.sholat.quran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.linuwih.sholat.R
import id.linuwih.sholat.quran.data.QuranRepository
import id.linuwih.sholat.quran.data.Surah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSearchScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onOpenSurah: (Int) -> Unit,
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val searching by viewModel.searching.collectAsState()
    val surahs by viewModel.surahs.collectAsState()

    // Pencarian ini menggantikan DUA kotak cari yang dulu terpisah, jadi harus mencocokkan nama
    // surah juga — bukan cuma isi ayat. Cocokkan nama lebih longgar: 2 huruf sudah cukup, karena
    // mencari nama surah tidak menghasilkan ledakan hasil seperti mencari kata di 6.236 ayat.
    val surahMatches = remember(surahs, query) {
        val q = query.trim()
        if (q.length < 2) {
            emptyList<Surah>()
        } else {
            surahs.filter {
                it.transliteration.contains(q, ignoreCase = true) ||
                    it.translationName.contains(q, ignoreCase = true) ||
                    it.number.toString() == q
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quran_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text(stringResource(R.string.quran_search_field_hint)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear_search))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            when {
                searching -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                query.trim().length < QuranRepository.MIN_SEARCH_LENGTH -> Hint(
                    stringResource(R.string.quran_search_hint_min, QuranRepository.MIN_SEARCH_LENGTH),
                )

                results.isEmpty() && surahMatches.isEmpty() ->
                    Hint(stringResource(R.string.quran_search_empty, query))

                else -> {
                    LazyColumn {
                        if (surahMatches.isNotEmpty()) {
                            item {
                                SectionLabel(stringResource(R.string.quran_search_section_surah))
                            }
                            items(surahMatches, key = { "surah-" + it.number }) { surah ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenSurah(surah.number) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        surah.number.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(32.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(surah.transliteration, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            surah.translationName,
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
                        }

                        if (results.isNotEmpty()) {
                            item {
                                SectionLabel(
                                    stringResource(R.string.quran_search_section_ayah, results.size),
                                )
                            }
                        }
                        items(results) { result ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenAyah(result.surahNumber, result.ayahNumber) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    "${result.surahName} : ${result.ayahNumber}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    result.snippet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}
