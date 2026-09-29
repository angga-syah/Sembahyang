package id.linuwih.sholat.ibadah.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.linuwih.sholat.R
import id.linuwih.sholat.ibadah.data.AmalHarianItem

private fun labelRes(item: AmalHarianItem): Int = when (item) {
    AmalHarianItem.SUBUH -> R.string.amal_item_subuh
    AmalHarianItem.DZUHUR -> R.string.amal_item_dzuhur
    AmalHarianItem.ASHAR -> R.string.amal_item_ashar
    AmalHarianItem.MAGHRIB -> R.string.amal_item_maghrib
    AmalHarianItem.ISYA -> R.string.amal_item_isya
    AmalHarianItem.DZIKIR_PAGI -> R.string.amal_item_dzikir_pagi
    AmalHarianItem.DZIKIR_PETANG -> R.string.amal_item_dzikir_petang
    AmalHarianItem.TILAWAH -> R.string.amal_item_tilawah
    AmalHarianItem.SEDEKAH -> R.string.amal_item_sedekah
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmalHarianScreen(onBack: () -> Unit) {
    val viewModel: AmalHarianViewModel = viewModel()
    val checked by viewModel.checkedToday.collectAsState()
    val items = remember { AmalHarianItem.entries }
    val progress = if (items.isEmpty()) 0f else checked.size.toFloat() / items.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.amal_harian_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        // Tanpa verticalScroll, 9 item checklist + judul + progress bar berisiko overflow di
        // layar kecil atau saat ukuran font sistem diperbesar (aksesibilitas) -- ditemukan
        // saat audit scroll/kontras layar-layar baru sesi ini, sama seperti bug serupa yang
        // sempat ada di IbadahMenuScreen.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.amal_harian_progress_format, checked.size, items.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(Modifier.height(16.dp))

            items.forEach { item ->
                val isChecked = item in checked
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggle(item, !isChecked) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = isChecked, onCheckedChange = { viewModel.toggle(item, it) })
                    Text(stringResource(labelRes(item)), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.amal_harian_reset_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
