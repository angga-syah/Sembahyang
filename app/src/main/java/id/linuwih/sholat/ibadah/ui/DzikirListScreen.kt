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

    Scaffold(
        topBar = {
            TopAppBar(
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
                            DzikirRow(item)
                            // Menjorok & tipis, seperti antar ayat di layar baca — garis penuh
                            // antar bacaan membuat daftar terlihat seperti tangga.
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DzikirRow(item: DzikirItem) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
        Text(
            item.arabic,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(item.latin, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(10.dp))
        Text(item.translation, style = MaterialTheme.typography.bodyMedium)
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
