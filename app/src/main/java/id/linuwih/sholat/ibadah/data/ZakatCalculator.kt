package id.linuwih.sholat.ibadah.data

/**
 * Kalkulator zakat murni matematika — tidak butuh dataset/konten kurasi apa pun, rumusnya baku
 * dan dipublikasikan luas oleh lembaga zakat (Baznas dkk.), jadi aman dihitung langsung di kode
 * tanpa risiko "mengarang" teks agama.
 *
 * Harga beras & emas SENGAJA tidak di-hardcode: keduanya berubah tiap waktu dan berbeda per
 * daerah, sementara app ini 100% offline (lihat plan doc) dan tidak bisa menarik harga terkini
 * sendiri. User mengisi harga saat itu sendiri — hasil kalkulasi selalu benar relatif terhadap
 * angka yang diisi, walau angkanya sendiri harus dicek user dari sumber terkini (mis. HET beras
 * Bulog, harga emas Antam hari itu).
 */
object ZakatCalculator {

    /**
     * 2.5 kg beras per jiwa — konversi paling umum dipakai lembaga zakat Indonesia dari takaran
     * asli 1 sha' (~3 liter/2,5-3 kg tergantung jenis makanan pokok). Ada pendapat lain (2,7 kg,
     * atau 3,5 liter) tapi 2,5 kg yang paling luas dipakai (termasuk Baznas), jadi jadi default.
     */
    const val FITRAH_KG_PER_JIWA = 2.5

    /** Nisab zakat maal: setara 85 gram emas — nilai yang paling luas disepakati ulama kontemporer. */
    const val MAAL_NISAB_GRAM_EMAS = 85.0

    /** Kadar zakat maal: 2,5% dari total harta yang sudah mencapai nisab dan haul. */
    const val MAAL_RATE = 0.025

    fun hitungFitrah(jumlahJiwa: Int, hargaBerasPerKg: Double): Double {
        require(jumlahJiwa >= 0) { "Jumlah jiwa tidak boleh negatif" }
        require(hargaBerasPerKg >= 0) { "Harga beras tidak boleh negatif" }
        return jumlahJiwa * FITRAH_KG_PER_JIWA * hargaBerasPerKg
    }

    data class HasilMaal(
        val nisab: Double,
        /** Wajib hanya kalau harta sudah mencapai nisab DAN sudah dimiliki 1 tahun penuh (haul). */
        val wajibZakat: Boolean,
        val jumlahZakat: Double,
    )

    fun hitungMaal(totalHarta: Double, hargaEmasPerGram: Double, sudahHaul: Boolean): HasilMaal {
        require(totalHarta >= 0) { "Total harta tidak boleh negatif" }
        require(hargaEmasPerGram >= 0) { "Harga emas tidak boleh negatif" }
        val nisab = MAAL_NISAB_GRAM_EMAS * hargaEmasPerGram
        val wajib = sudahHaul && totalHarta >= nisab
        val zakat = if (wajib) totalHarta * MAAL_RATE else 0.0
        return HasilMaal(nisab = nisab, wajibZakat = wajib, jumlahZakat = zakat)
    }
}
