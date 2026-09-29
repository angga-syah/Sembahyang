package id.linuwih.sholat.prayer.data

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.model.Rounding

/**
 * Metode kalkulasi yang bisa dipilih user (lihat plan: "bisa pilih metode lain juga").
 * KEMENAG_RI bukan bawaan library adhan2, jadi dibuat manual dengan sudut resmi
 * Kemenag (Fajr 20°, Isya 18°) — dikonfirmasi dari falakmu.id, lihat plan doc.
 */
enum class PrayerCalculationMethod {
    KEMENAG_RI,
    MUSLIM_WORLD_LEAGUE,
    EGYPTIAN,
    KARACHI,
    UMM_AL_QURA,
    NORTH_AMERICA,
    SINGAPORE,
    TURKEY,
    DUBAI,
    KUWAIT,
    QATAR,
    MOON_SIGHTING_COMMITTEE;

    fun toCalculationParameters(): CalculationParameters = when (this) {
        KEMENAG_RI -> CalculationParameters(
            fajrAngle = 20.0,
            ishaAngle = 18.0,
            method = CalculationMethod.OTHER,
            madhab = Madhab.SHAFI,
            // Kemenag/NU membulatkan KE ATAS sebagai kehati-hatian (dikonfirmasi dari FAQ
            // resmi NU Online soal kenapa antar-app bisa beda beberapa menit), bukan ke
            // terdekat seperti default library ini. Tanpa ini, hasil kita masih bisa
            // terpaut ~1 menit lagi dari jadwal resmi bahkan setelah ihtiyat diterapkan --
            // sisi yang salah pula (lebih cepat, bukan lebih lambat).
            rounding = Rounding.UP,
        )
        MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        EGYPTIAN -> CalculationMethod.EGYPTIAN.parameters
        KARACHI -> CalculationMethod.KARACHI.parameters
        UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA.parameters
        NORTH_AMERICA -> CalculationMethod.NORTH_AMERICA.parameters
        SINGAPORE -> CalculationMethod.SINGAPORE.parameters
        TURKEY -> CalculationMethod.TURKEY.parameters
        DUBAI -> CalculationMethod.DUBAI.parameters
        KUWAIT -> CalculationMethod.KUWAIT.parameters
        QATAR -> CalculationMethod.QATAR.parameters
        MOON_SIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
    }

    /**
     * Ihtiyat (waktu pengaman) BAKU milik metode ini sendiri — bukan koreksi pribadi user
     * (lihat PrayerPrefs.offsetMinutes, yang untuk selisih ke jadwal masjid setempat).
     *
     * Kemenag RI resmi menambahkan ihtiyat di atas hasil hisab murni: Subuh/Zuhur/Ashar/
     * Maghrib/Isya digenapkan MAJU 2 menit (lebih aman terlambat sedikit masuk waktu
     * daripada terlalu cepat), sementara Terbit digenapkan MUNDUR 2 menit (lebih aman
     * dianggap belum terbit). Tanpa ini, hisab kita cocok dengan kalkulasi astronomi murni
     * (mis. Google) tapi berselisih ~2 menit dari jadwal resmi Kemenag dan kebanyakan situs
     * Indonesia yang mengikutinya.
     *
     * Metode lain (MWL, Mesir, dst.) sengaja TIDAK diberi ihtiyat di sini — itu konvensi
     * khas Indonesia, bukan bagian standar internasional metode-metode itu.
     */
    fun defaultIhtiyat(): PrayerAdjustments = when (this) {
        KEMENAG_RI -> PrayerAdjustments(fajr = 2, sunrise = -2, dhuhr = 2, asr = 2, maghrib = 2, isha = 2)
        else -> PrayerAdjustments()
    }

    companion object {
        /**
         * Deteksi region 100% offline lewat bounding-box koordinat (lihat plan:
         * tidak pakai reverse-geocoding API supaya tetap offline).
         */
        fun defaultFor(latitude: Double, longitude: Double): PrayerCalculationMethod {
            val isIndonesia = latitude in -11.5..6.5 && longitude in 94.5..141.5
            return if (isIndonesia) KEMENAG_RI else MUSLIM_WORLD_LEAGUE
        }
    }
}
