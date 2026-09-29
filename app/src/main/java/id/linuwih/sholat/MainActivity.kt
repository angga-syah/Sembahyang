package id.linuwih.sholat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import id.linuwih.sholat.ui.navigation.SholatNavHost
import id.linuwih.sholat.ui.theme.SholatTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Statusnya ditampilkan di layar Setelan Notifikasi, tidak perlu ditindaklanjuti di sini. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            SholatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SholatNavHost()
                }
            }
        }
    }

    /**
     * Sejak Android 13 izin notifikasi WAJIB diminta saat aplikasi berjalan — mendeklarasikannya
     * di manifest saja tidak cukup. Selama ini izin itu hanya diperiksa sebelum menampilkan
     * notifikasi tapi tidak pernah diminta, sehingga tidak pernah diberikan dan seluruh notifikasi
     * adzan berhenti diam-diam tanpa jejak apa pun di layar.
     *
     * Diminta di sini (saat aplikasi dibuka) supaya user yang tidak pernah membuka layar Setelan
     * pun tetap ditanya — adzan adalah fitur utama aplikasi ini, bukan pelengkap.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
