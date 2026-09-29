package id.linuwih.sholat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografi disetel ringan di atas baseline Material, bukan diganti total.
 *
 * Dua penyesuaian yang disengaja:
 * - Judul memakai bobot SemiBold dengan letterSpacing sedikit dirapatkan. Bobot Bold bawaan
 *   terasa berat untuk layar yang isinya banyak daftar, sementara rapat sedikit membuat judul
 *   terbaca sebagai satu kesatuan.
 * - Teks isi diberi lineHeight lebih lapang. Terjemahan ayat sering panjang, dan jarak baris
 *   bawaan membuatnya padat sehingga melelahkan dibaca.
 *
 * Teks Arab TIDAK diatur di sini — ukuran & jarak barisnya dihitung khusus di layar baca
 * (lihat AyahRow) karena harus ikut skala ukuran font pilihan user dan butuh rasio jarak baris
 * yang jauh lebih longgar daripada teks Latin.
 */
val SholatTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)
