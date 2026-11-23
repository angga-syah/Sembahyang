package id.linuwih.sholat.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet cadangan untuk HP di bawah Android 12 (yang belum punya dynamic color / Material You).
 *
 * Sebelumnya hanya 3 slot yang diisi (primary, secondary, background), sehingga SEMUA slot lain
 * — primaryContainer, surfaceVariant, tertiary, dst. — tetap memakai baseline Material yang
 * berwarna ungu. Akibatnya elemen seperti lingkaran besar Tasbih (yang memakai primaryContainer)
 * tampil ungu, bukan teal, dan nuansa teal/emas praktis tidak pernah muncul utuh.
 *
 * Slot gelap juga dulu memakai TealPrimary yang sama persis dengan mode terang. Di Material 3,
 * warna primary pada tema gelap harus LEBIH TERANG dari permukaannya — teal pekat di atas latar
 * gelap menghasilkan kontras yang buruk. Karena itu varian gelap dibuat terpisah di bawah ini.
 */

// Warna merek
val TealPrimary = Color(0xFF00695C)
val TealPrimaryDark = Color(0xFF004D40)
val GoldAccent = Color(0xFFC9A227)
// Netral dibuat hangat (gading), selaras dengan halaman kertas di tab Qur'an.
val SurfaceLight = Color(0xFFFAF7F0)
val SurfaceDark = Color(0xFF141310)

// --- Mode terang ---
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFB2DFDB)
val LightOnPrimaryContainer = Color(0xFF00201B)
val LightSecondary = Color(0xFF8A6D00)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFF7E4A8)
val LightOnSecondaryContainer = Color(0xFF2A1F00)
val LightTertiary = Color(0xFF3F6375)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFC3E8FD)
val LightOnTertiaryContainer = Color(0xFF001F2A)
val LightOnBackground = Color(0xFF1E1B16)
val LightSurface = Color(0xFFFAF7F0)
val LightOnSurface = Color(0xFF1E1B16)
val LightSurfaceVariant = Color(0xFFE8E3D6)
val LightOnSurfaceVariant = Color(0xFF4A463D)
val LightOutline = Color(0xFF7B766A)
val LightOutlineVariant = Color(0xFFD5CEBE)
// Slot surfaceContainer* WAJIB diisi: kalau tidak, Material memakai baseline ungu muda
// (mis. bar navigasi bawah & menu dropdown tampak keunguan di tengah palet teal/emas).
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF5F1E8)
val LightSurfaceContainer = Color(0xFFF0EBE0)
val LightSurfaceContainerHigh = Color(0xFFEAE5D9)
val LightSurfaceContainerHighest = Color(0xFFE4DFD2)
val LightSurfaceDim = Color(0xFFDCD7CA)
val LightSurfaceBright = Color(0xFFFAF7F0)

// --- Mode gelap ---
val DarkPrimary = Color(0xFF6FDBC8)
val DarkOnPrimary = Color(0xFF00382F)
val DarkPrimaryContainer = Color(0xFF005046)
val DarkOnPrimaryContainer = Color(0xFF8BF8E4)
val DarkSecondary = Color(0xFFE6C65C)
val DarkOnSecondary = Color(0xFF3A2F00)
val DarkSecondaryContainer = Color(0xFF544400)
val DarkOnSecondaryContainer = Color(0xFFF7E4A8)
val DarkTertiary = Color(0xFFA7CCE0)
val DarkOnTertiary = Color(0xFF0A3445)
val DarkTertiaryContainer = Color(0xFF264B5D)
val DarkOnTertiaryContainer = Color(0xFFC3E8FD)
val DarkOnBackground = Color(0xFFE8E2D6)
val DarkSurface = Color(0xFF141310)
val DarkOnSurface = Color(0xFFE8E2D6)
val DarkSurfaceVariant = Color(0xFF494539)
val DarkOnSurfaceVariant = Color(0xFFCCC5B5)
val DarkOutline = Color(0xFF959080)
val DarkOutlineVariant = Color(0xFF494539)
val DarkSurfaceContainerLowest = Color(0xFF0F0E0B)
val DarkSurfaceContainerLow = Color(0xFF1C1B17)
val DarkSurfaceContainer = Color(0xFF211F1B)
val DarkSurfaceContainerHigh = Color(0xFF2B2925)
val DarkSurfaceContainerHighest = Color(0xFF36342F)
val DarkSurfaceDim = Color(0xFF141310)
val DarkSurfaceBright = Color(0xFF3B3934)
