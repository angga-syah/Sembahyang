package id.linuwih.sholat.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Tampilan "kertas cetak" untuk layar baca (Qur'an, dzikir, tata cara sholat): latar krem
 * bertekstur serat halus seperti kertas mushaf, tinta cokelat-hitam, dan garis bingkai emas.
 *
 * Teks Arab TIDAK diubah sama sekali di sini — font, ukuran, dan jarak barisnya tetap diatur
 * di masing-masing layar. Tekstur digambar di BELAKANG teks (drawBehind), jadi tidak bisa
 * mengganggu bentuk huruf atau posisi harakat.
 */
@Immutable
data class PaperPalette(
    val paper: Color,
    val paperEdge: Color,
    val ink: Color,
    val inkSoft: Color,
    val rule: Color,
    val fleck: Color,
    val isDark: Boolean,
)

val LightPaper = PaperPalette(
    paper = Color(0xFFF7F0DE),
    paperEdge = Color(0xFFE6D7B4),
    ink = Color(0xFF221B13),
    inkSoft = Color(0xFF5A4C3B),
    rule = Color(0xFFA98535),
    fleck = Color(0xFF6E5732),
    isDark = false,
)

/** Mode gelap: kertas tetap "kertas", tapi seperti dibaca di bawah lampu redup. */
val DarkPaper = PaperPalette(
    paper = Color(0xFF1E1B15),
    paperEdge = Color(0xFF110F0B),
    ink = Color(0xFFEDE3CB),
    inkSoft = Color(0xFFBDB197),
    rule = Color(0xFFC9A55A),
    fleck = Color(0xFFFFF1D0),
    isDark = true,
)

val LocalPaper = staticCompositionLocalOf<PaperPalette?> { null }

/** Font isi bergaya buku cetak untuk terjemahan/latin di halaman kertas. */
val PrintSerif: FontFamily = FontFamily.Serif

/**
 * Tekstur serat kertas: bintik dan serat pendek beralfa sangat rendah di atas transparan,
 * diulang (tile) memenuhi layar. Deterministik (seed tetap) supaya tidak "berkedip" antar
 * recomposition, dan cukup kecil (256 px) sehingga dibuat sekali dalam hitungan milidetik.
 */
private fun buildPaperTexture(fleck: Color, dark: Boolean): ImageBitmap {
    val size = 256
    val pixels = IntArray(size * size)
    val rnd = Random(7L)
    val base = fleck.toArgb() and 0x00FFFFFF
    val maxAlpha = if (dark) 10 else 16
    for (i in pixels.indices) {
        // Butiran halus merata.
        val a = (rnd.nextGaussian() * maxAlpha / 2.5).toInt().coerceIn(0, maxAlpha)
        pixels[i] = (a shl 24) or base
    }
    // Serat pendek mendatar — ciri kertas, bukan sekadar noise.
    repeat(140) {
        val x0 = rnd.nextInt(size)
        val y0 = rnd.nextInt(size)
        val len = 6 + rnd.nextInt(18)
        val a = (maxAlpha + rnd.nextInt(maxAlpha)).coerceAtMost(40)
        val slope = (rnd.nextFloat() - 0.5f) * 0.6f
        for (k in 0 until len) {
            val x = (x0 + k) % size
            val y = ((y0 + (k * slope).toInt()) % size + size) % size
            pixels[y * size + x] = (a shl 24) or base
        }
    }
    // Sesekali noda kecil seperti serat kayu yang tercetak.
    repeat(18) {
        val cx = rnd.nextInt(size)
        val cy = rnd.nextInt(size)
        val a = maxAlpha + 8
        pixels[cy * size + cx] = (a shl 24) or base
        pixels[cy * size + (cx + 1) % size] = ((a / 2) shl 24) or base
    }
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap.asImageBitmap()
}

/**
 * Latar kertas: warna dasar + tekstur + bayangan tepi (vignette) seperti halaman cetak.
 * [framed] menambah bingkai garis ganda di kiri-kanan, meniru bingkai halaman mushaf.
 */
fun Modifier.paperBackground(palette: PaperPalette, texture: ImageBitmap, framed: Boolean = false): Modifier =
    drawWithCache {
        val grain = ShaderBrush(ImageShader(texture, TileMode.Repeated, TileMode.Repeated))
        val vignette = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent, palette.paperEdge.copy(alpha = if (palette.isDark) 0.7f else 0.55f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = maxOf(size.width, size.height) * 0.75f,
        )
        val outer = 8.dp.toPx()
        val inner = 12.dp.toPx()
        val thick = 1.6.dp.toPx()
        val thin = 0.8.dp.toPx()
        onDrawBehind {
            drawRect(palette.paper)
            drawRect(grain)
            drawRect(vignette)
            if (framed) {
                val c = palette.rule.copy(alpha = 0.75f)
                drawLine(c, Offset(outer, 0f), Offset(outer, size.height), thick)
                drawLine(c, Offset(inner, 0f), Offset(inner, size.height), thin)
                drawLine(c, Offset(size.width - outer, 0f), Offset(size.width - outer, size.height), thick)
                drawLine(c, Offset(size.width - inner, 0f), Offset(size.width - inner, size.height), thin)
            }
        }
    }

/**
 * Bungkus halaman berlatar kertas. Di dalamnya, warna Material (surface, onSurface, dst.)
 * diarahkan ke palet kertas, sehingga Scaffold/TopAppBar/Text yang sudah ada otomatis ikut
 * memakai tinta & kertas tanpa perlu diubah satu per satu. Layar yang ingin teksturnya
 * terlihat cukup memakai `containerColor = Color.Transparent` pada Scaffold-nya.
 */
@Composable
fun PaperPage(modifier: Modifier = Modifier, framed: Boolean = false, content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val palette = if (base.background.luminance() < 0.5f) DarkPaper else LightPaper
    val texture = remember(palette) { buildPaperTexture(palette.fleck, palette.isDark) }
    val scheme = remember(base, palette) {
        base.copy(
            background = palette.paper,
            onBackground = palette.ink,
            surface = palette.paper,
            onSurface = palette.ink,
            onSurfaceVariant = palette.inkSoft,
            surfaceVariant = palette.paperEdge,
            outlineVariant = palette.rule.copy(alpha = 0.45f),
            surfaceContainerLowest = palette.paper,
            surfaceContainerLow = palette.paper,
            surfaceContainer = palette.paper,
            surfaceContainerHigh = palette.paperEdge.copy(alpha = 0.6f).compositeOn(palette.paper),
            surfaceContainerHighest = palette.paperEdge,
            // Rona elevasi (bar bawah, dialog) ikut emas kertas; dengan teal bawaan, permukaan
            // yang terangkat tampak kehijauan di atas kertas krem.
            surfaceTint = palette.rule,
        )
    }
    Box(modifier.fillMaxSize().paperBackground(palette, texture, framed)) {
        CompositionLocalProvider(LocalPaper provides palette) {
            MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
        }
    }
}

/** TopAppBar tembus pandang supaya tekstur kertas terlihat sampai ke atas layar. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun paperTopBarColors() = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
)

private fun Color.compositeOn(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f,
    )
}

/**
 * Medali nomor ayat bergaya mushaf: bintang segi-delapan bergaris emas dengan nomor di
 * tengah. Digambar dengan Canvas (bukan glyph U+06DD) supaya tampil sama di semua font dan
 * tidak bergantung dukungan tanda penutup ayat di font yang dipilih user.
 */
@Composable
fun AyahMedallion(number: Int, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val rule = LocalPaper.current?.rule ?: MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = min(this.size.width, this.size.height) / 2f
            val c = center
            val star = Path()
            for (i in 0 until 16) {
                val radius = if (i % 2 == 0) r * 0.98f else r * 0.80f
                val ang = (i * PI / 8 - PI / 2).toFloat()
                val p = Offset(c.x + radius * cos(ang), c.y + radius * sin(ang))
                if (i == 0) star.moveTo(p.x, p.y) else star.lineTo(p.x, p.y)
            }
            star.close()
            drawPath(star, rule.copy(alpha = 0.10f))
            drawPath(star, rule, style = Stroke(width = 1.2.dp.toPx()))
            drawCircle(rule.copy(alpha = 0.8f), radius = r * 0.62f, center = c, style = Stroke(width = 0.8.dp.toPx()))
        }
        Text(
            "$number",
            color = ink,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PrintSerif, fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
    }
}

/** Pemisah antar ayat/bacaan: garis tipis emas dengan belah ketupat kecil di tengah. */
@Composable
fun OrnamentDivider(modifier: Modifier = Modifier) {
    val rule = LocalPaper.current?.rule ?: MaterialTheme.colorScheme.outlineVariant
    Row(modifier.fillMaxWidth().height(12.dp).padding(horizontal = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2f
            val mid = size.width / 2f
            val d = 4.dp.toPx()
            val gap = 10.dp.toPx()
            val line = Brush.horizontalGradient(
                0f to Color.Transparent,
                1f to rule.copy(alpha = 0.6f),
                startX = 0f,
                endX = mid - gap,
            )
            val line2 = Brush.horizontalGradient(
                0f to rule.copy(alpha = 0.6f),
                1f to Color.Transparent,
                startX = mid + gap,
                endX = size.width,
            )
            drawLine(line, Offset(0f, y), Offset(mid - gap, y), 0.8.dp.toPx())
            drawLine(line2, Offset(mid + gap, y), Offset(size.width, y), 0.8.dp.toPx())
            val diamond = Path().apply {
                moveTo(mid, y - d); lineTo(mid + d, y); lineTo(mid, y + d); lineTo(mid - d, y); close()
            }
            drawPath(diamond, rule.copy(alpha = 0.8f))
        }
    }
}
