package id.linuwih.sholat.ibadah.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import id.linuwih.sholat.ibadah.data.WarisCalculator
import id.linuwih.sholat.ui.components.CurrencyTextField
import java.text.NumberFormat
import java.util.Locale

private fun formatRupiah(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }
    return formatter.format(value)
}

private fun parseAmount(text: String): Double = text.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
private fun parseCount(text: String): Int = text.filter { it.isDigit() }.toIntOrNull() ?: 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarisScreen(onBack: () -> Unit) {
    var pewarisLakiLaki by rememberSaveable { mutableStateOf(true) }
    var jumlahIstriText by rememberSaveable { mutableStateOf("1") }
    var suamiHidup by rememberSaveable { mutableStateOf(true) }
    var anakLakiLakiText by rememberSaveable { mutableStateOf("0") }
    var anakPerempuanText by rememberSaveable { mutableStateOf("0") }
    var ayahHidup by rememberSaveable { mutableStateOf(false) }
    var ibuHidup by rememberSaveable { mutableStateOf(false) }
    var hartaText by rememberSaveable { mutableStateOf("") }

    val jumlahIstri = parseCount(jumlahIstriText).coerceIn(0, 4)
    val anakLakiLaki = parseCount(anakLakiLakiText)
    val anakPerempuan = parseCount(anakPerempuanText)
    val harta = parseAmount(hartaText)

    val hasil = remember(pewarisLakiLaki, jumlahIstri, suamiHidup, anakLakiLaki, anakPerempuan, ayahHidup, ibuHidup) {
        WarisCalculator.hitung(
            WarisCalculator.Ahli(
                pewarisLakiLaki = pewarisLakiLaki,
                jumlahIstriHidup = jumlahIstri,
                suamiHidup = suamiHidup,
                anakLakiLaki = anakLakiLaki,
                anakPerempuan = anakPerempuan,
                ayahHidup = ayahHidup,
                ibuHidup = ibuHidup,
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.waris_title)) },
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
            // Peringatan cakupan ditampilkan PALING ATAS dan permanen (bukan dialog yang bisa
            // ditutup lalu terlupakan) — ini bukan sekadar disclaimer hukum, tapi batasan nyata
            // yang mengubah jawaban benar-salahnya perhitungan kalau diabaikan.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    stringResource(R.string.waris_disclaimer),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.waris_pewaris_label), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = pewarisLakiLaki,
                    onClick = { pewarisLakiLaki = true },
                    label = { Text(stringResource(R.string.waris_pewaris_lakilaki)) },
                )
                FilterChip(
                    selected = !pewarisLakiLaki,
                    onClick = { pewarisLakiLaki = false },
                    label = { Text(stringResource(R.string.waris_pewaris_perempuan)) },
                )
            }
            Spacer(Modifier.height(16.dp))

            if (pewarisLakiLaki) {
                OutlinedTextField(
                    value = jumlahIstriText,
                    onValueChange = { jumlahIstriText = it },
                    label = { Text(stringResource(R.string.waris_jumlah_istri_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = suamiHidup, onCheckedChange = { suamiHidup = it })
                    Text(stringResource(R.string.waris_suami_hidup_label))
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = anakLakiLakiText,
                onValueChange = { anakLakiLakiText = it },
                label = { Text(stringResource(R.string.waris_anak_lakilaki_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = anakPerempuanText,
                onValueChange = { anakPerempuanText = it },
                label = { Text(stringResource(R.string.waris_anak_perempuan_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = ayahHidup, onCheckedChange = { ayahHidup = it })
                Text(stringResource(R.string.waris_ayah_hidup_label))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = ibuHidup, onCheckedChange = { ibuHidup = it })
                Text(stringResource(R.string.waris_ibu_hidup_label))
            }
            Spacer(Modifier.height(16.dp))

            CurrencyTextField(
                value = hartaText,
                onValueChange = { hartaText = it },
                label = { Text(stringResource(R.string.waris_harta_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            if (hasil.bagian.isEmpty()) {
                Text(
                    stringResource(R.string.waris_no_heir),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(stringResource(R.string.waris_result_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                hasil.bagian.forEach { bagian ->
                    val perOrang = bagian.fraksi / bagian.jumlahJiwa
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(bagian.ahli, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(R.string.waris_fraksi_format, bagian.fraksi * 100),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (harta > 0) {
                                val labelJumlah = if (bagian.jumlahJiwa > 1) {
                                    stringResource(
                                        R.string.waris_amount_per_person_format,
                                        formatRupiah(perOrang * harta),
                                        bagian.jumlahJiwa,
                                        formatRupiah(bagian.fraksi * harta),
                                    )
                                } else {
                                    formatRupiah(bagian.fraksi * harta)
                                }
                                Text(
                                    labelJumlah,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (hasil.adaAul) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.waris_aul_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hasil.butuhRadd) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.waris_radd_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
