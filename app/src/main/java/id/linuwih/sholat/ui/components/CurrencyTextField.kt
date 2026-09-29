package id.linuwih.sholat.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * Sisipkan titik pemisah ribuan gaya Indonesia ("15000" -> "15.000") supaya angka besar
 * (harga beras, harta, harga emas) tidak sulit dibaca saat diketik — tanpa ini "150000000"
 * gampang salah hitung nol-nya sendiri oleh user. Nol di depan dibuang ("0015" -> "15"),
 * kecuali satu-satunya digit memang nol.
 */
fun formatThousands(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    val trimmed = digits.trimStart('0').ifEmpty { "0" }
    return trimmed.reversed().chunked(3).joinToString(".").reversed()
}

/** Buang titik pemisah — kebalikan [formatThousands], dipakai saat parsing ke Double/Int. */
fun stripThousands(formatted: String): String = formatted.filter { it.isDigit() }

/**
 * OutlinedTextField angka dengan pemisah ribuan otomatis saat mengetik. State yang disimpan
 * pemanggil SUDAH berisi titik ("15.000"), bukan angka mentah -- parsing tetap pakai
 * [stripThousands] atau filter digit seperti biasa, titiknya otomatis diabaikan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(formatThousands(it)) },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
