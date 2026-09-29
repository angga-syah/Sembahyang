package id.linuwih.sholat.qibla.ui

data class QiblaUiState(
    val loading: Boolean = true,
    val permissionDenied: Boolean = false,
    val hasCompassSensor: Boolean = true,
    val qiblaBearing: Float? = null,
    val needleRotation: Float = 0f,
    /**
     * Rotasi yang harus diterapkan ke piringan mata angin supaya huruf U (utara) menunjuk
     * ke utara sebenarnya. Dipisah dari [needleRotation] karena jarum kiblat dan piringan
     * berputar dengan acuan berbeda: jarum relatif ke arah kiblat, piringan relatif ke utara.
     */
    val northRotation: Float = 0f,
    /**
     * Selisih absolut ke arah kiblat dalam derajat (0..180). Ditampilkan apa adanya supaya user
     * tahu SEBERAPA meleset — penanda hijau/tidak saja menyembunyikan informasi ini.
     */
    val offByDegrees: Float = 0f,
    val accuracyLow: Boolean = false,
    val locationLabel: String? = null,
    val errorMessage: String? = null,
    /**
     * Dihitung di QiblaViewModel dengan histeresis (masuk di <=3 derajat, baru lepas di >6
     * derajat) — bukan ambang tunggal, karena ambang tunggal membuat status kedip-kedip
     * (dan getar berulang) tiap kali pembacaan sensor bergetar di sekitar batas.
     */
    val isAligned: Boolean = false,
)
