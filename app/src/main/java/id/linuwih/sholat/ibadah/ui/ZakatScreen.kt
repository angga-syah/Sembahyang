package id.linuwih.sholat.ibadah.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.linuwih.sholat.R
import id.linuwih.sholat.ibadah.data.ZakatCalculator
import id.linuwih.sholat.ui.components.CurrencyTextField
import java.text.NumberFormat
import java.util.Locale

private enum class ZakatTab { FITRAH, MAAL }

/** Format Rupiah tanpa desimal — angka zakat tidak pernah butuh presisi sen. */
private fun formatRupiah(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }
    return formatter.format(value)
}

/** Parse input angka user (boleh kosong/karakter aneh saat mengetik) — jangan pernah crash. */
private fun parseAmount(text: String): Double = text.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

private fun parseCount(text: String): Int = text.filter { it.isDigit() }.toIntOrNull() ?: 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatScreen(onBack: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(ZakatTab.FITRAH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.zakat_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tab == ZakatTab.FITRAH,
                    onClick = { tab = ZakatTab.FITRAH },
                    label = { Text(stringResource(R.string.zakat_tab_fitrah)) },
                )
                FilterChip(
                    selected = tab == ZakatTab.MAAL,
                    onClick = { tab = ZakatTab.MAAL },
                    label = { Text(stringResource(R.string.zakat_tab_maal)) },
                )
            }
            Spacer(Modifier.height(16.dp))

            when (tab) {
                ZakatTab.FITRAH -> ZakatFitrahForm()
                ZakatTab.MAAL -> ZakatMaalForm()
            }
        }
    }
}

@Composable
private fun ZakatFitrahForm() {
    var jiwaText by rememberSaveable { mutableStateOf("1") }
    var hargaText by rememberSaveable { mutableStateOf("") }

    val jiwa = parseCount(jiwaText)
    val harga = parseAmount(hargaText)
    val hasil = ZakatCalculator.hitungFitrah(jiwa, harga)

    Text(
        stringResource(R.string.zakat_fitrah_explainer, ZakatCalculator.FITRAH_KG_PER_JIWA),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = jiwaText,
        onValueChange = { jiwaText = it },
        label = { Text(stringResource(R.string.zakat_fitrah_jiwa_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    CurrencyTextField(
        value = hargaText,
        onValueChange = { hargaText = it },
        label = { Text(stringResource(R.string.zakat_harga_beras_label)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(20.dp))

    ResultCard(
        title = stringResource(R.string.zakat_fitrah_result_title),
        amount = formatRupiah(hasil),
    )
}

@Composable
private fun ZakatMaalForm() {
    var hartaText by rememberSaveable { mutableStateOf("") }
    var emasText by rememberSaveable { mutableStateOf("") }
    var sudahHaul by rememberSaveable { mutableStateOf(true) }

    val harta = parseAmount(hartaText)
    val emas = parseAmount(emasText)
    val hasil = ZakatCalculator.hitungMaal(harta, emas, sudahHaul)

    Text(
        stringResource(R.string.zakat_maal_explainer, ZakatCalculator.MAAL_NISAB_GRAM_EMAS.toInt()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))

    CurrencyTextField(
        value = hartaText,
        onValueChange = { hartaText = it },
        label = { Text(stringResource(R.string.zakat_maal_harta_label)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    CurrencyTextField(
        value = emasText,
        onValueChange = { emasText = it },
        label = { Text(stringResource(R.string.zakat_harga_emas_label)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(checked = sudahHaul, onCheckedChange = { sudahHaul = it })
        Text(stringResource(R.string.zakat_maal_haul_label), style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(20.dp))

    if (harta > 0 || emas > 0) {
        Text(
            stringResource(R.string.zakat_maal_nisab_format, formatRupiah(hasil.nisab)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (hasil.wajibZakat) {
        ResultCard(
            title = stringResource(R.string.zakat_maal_result_title),
            amount = formatRupiah(hasil.jumlahZakat),
        )
    } else {
        Text(
            stringResource(R.string.zakat_maal_not_obligated),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ResultCard(title: String, amount: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                amount,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
