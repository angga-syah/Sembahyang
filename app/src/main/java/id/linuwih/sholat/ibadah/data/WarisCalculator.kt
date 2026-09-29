package id.linuwih.sholat.ibadah.data

/**
 * Kalkulator waris (faraid). BACA BATASAN INI SEBELUM MENGUBAH LOGIKANYA:
 *
 * Cakupan HANYA ahli waris utama: pasangan (suami/istri), anak (laki-laki/perempuan),
 * ayah, ibu. TIDAK mencakup kakek/nenek, saudara kandung/seayah/seibu, atau ahli waris
 * lain yang lebih jauh — kehadiran mereka mengubah pembagian (terutama bagian ibu, yang
 * turun dari 1/3 ke 1/6 kalau ada 2 saudara atau lebih) dan itu di luar cakupan kalkulator
 * ini. UI WAJIB menampilkan peringatan ini dengan jelas — kalkulator ini mengasumsikan
 * TIDAK ADA ahli waris lain selain yang diisi user, bukan diam-diam mengabaikan mereka.
 *
 * Rumus bagian tetap (furudh) di bawah adalah ijma' (kesepakatan) 4 mazhab Sunni untuk
 * ahli waris primer ini — tidak ada perbedaan mazhab pada level ini; beda mazhab baru
 * muncul di kasus lanjutan (kakek bersama saudara, dll.) yang tidak dicakup di sini.
 *
 * 'AUL diterapkan otomatis: kalau total bagian tetap > 1 (mis. suami + 2 anak perempuan +
 * ibu → asal masalah 12 'aul jadi 13), semua bagian diperkecil proporsional — hasil
 * (suami 3/13, anak perempuan 8/13, ibu 2/13) sudah diverifikasi manual cocok dengan
 * contoh baku kitab faraid.
 *
 * RADD (sisa harta saat TIDAK ADA ashabah dalam cakupan ini) TIDAK diotomatisasi — hasil
 * kalkulator menandainya [HasilWaris.butuhRadd] = true, bukan menebak pembagiannya. Radd
 * yang benar butuh tahu SEMUA ahli waris (termasuk yang di luar cakupan ini), yang
 * kalkulator ini tidak punya datanya.
 */
object WarisCalculator {

    data class Ahli(
        /** Jenis kelamin pewaris (orang yang meninggal) — menentukan siapa "pasangan"-nya. */
        val pewarisLakiLaki: Boolean,
        /** Relevan hanya kalau pewaris laki-laki. */
        val jumlahIstriHidup: Int = 0,
        /** Relevan hanya kalau pewaris perempuan. */
        val suamiHidup: Boolean = false,
        val anakLakiLaki: Int = 0,
        val anakPerempuan: Int = 0,
        val ayahHidup: Boolean = false,
        val ibuHidup: Boolean = false,
    )

    /** [fraksi] adalah bagian TOTAL kelompok ini (mis. gabungan semua anak perempuan), bukan
     *  per-orang — bagi dengan [jumlahJiwa] untuk bagian per-orang. */
    data class Bagian(val ahli: String, val fraksi: Double, val jumlahJiwa: Int = 1)

    data class HasilWaris(
        val bagian: List<Bagian>,
        val adaAul: Boolean,
        val butuhRadd: Boolean,
    )

    fun hitung(ahli: Ahli): HasilWaris {
        require(ahli.jumlahIstriHidup in 0..4) { "Jumlah istri harus 0-4" }
        require(ahli.anakLakiLaki >= 0 && ahli.anakPerempuan >= 0) { "Jumlah anak tidak boleh negatif" }

        val punyaKeturunan = ahli.anakLakiLaki > 0 || ahli.anakPerempuan > 0
        val pasanganAda = (ahli.pewarisLakiLaki && ahli.jumlahIstriHidup > 0) ||
            (!ahli.pewarisLakiLaki && ahli.suamiHidup)

        val bagian = mutableListOf<Bagian>()
        var totalFurudh = 0.0

        // --- Pasangan ---
        if (ahli.pewarisLakiLaki && ahli.jumlahIstriHidup > 0) {
            val fraksi = if (punyaKeturunan) 1.0 / 8 else 1.0 / 4
            bagian += Bagian("Istri", fraksi, ahli.jumlahIstriHidup)
            totalFurudh += fraksi
        } else if (!ahli.pewarisLakiLaki && ahli.suamiHidup) {
            val fraksi = if (punyaKeturunan) 1.0 / 4 else 1.0 / 2
            bagian += Bagian("Suami", fraksi)
            totalFurudh += fraksi
        }

        // --- Anak perempuan: bagian tetap HANYA kalau tidak ada anak laki-laki (kalau ada,
        // anak perempuan jadi ashabah bil ghair bersama anak laki-laki — ditangani di bagian
        // residu di bawah, rasio 2:1 laki:perempuan). ---
        val anakPerempuanJadiAshabah = ahli.anakLakiLaki > 0 && ahli.anakPerempuan > 0
        if (ahli.anakLakiLaki == 0 && ahli.anakPerempuan > 0) {
            val fraksi = if (ahli.anakPerempuan == 1) 1.0 / 2 else 2.0 / 3
            bagian += Bagian("Anak perempuan", fraksi, ahli.anakPerempuan)
            totalFurudh += fraksi
        }

        // --- Ayah: bagian tetap 1/6 kalau ada keturunan, ashabah (dapat sisa) kalau tidak
        // ada anak laki-laki -- baik sebagai TAMBAHAN atas 1/6 (kalau ada anak perempuan saja)
        // maupun SATU-SATUNYA bagian (kalau tidak ada keturunan sama sekali). ---
        if (ahli.ayahHidup && punyaKeturunan) {
            bagian += Bagian("Ayah (bagian tetap)", 1.0 / 6)
            totalFurudh += 1.0 / 6
        }
        val ayahJadiAshabah = ahli.ayahHidup && ahli.anakLakiLaki == 0

        // --- Ibu: kasus gharrawain/'umariyyatan (pasangan + kedua orang tua, TANPA keturunan)
        // -- ibu dapat 1/3 dari SISA setelah bagian pasangan, bukan 1/3 dari total harta. Ini
        // pengecualian yang diputuskan Umar bin Khattab dan diikuti jumhur ulama. ---
        if (ahli.ibuHidup) {
            val gharrawain = !punyaKeturunan && pasanganAda && ahli.ayahHidup
            val fraksi = when {
                gharrawain -> (1.0 - totalFurudh) / 3.0
                punyaKeturunan -> 1.0 / 6
                // Berasumsi TIDAK ADA 2 saudara atau lebih — lihat batasan cakupan di atas.
                else -> 1.0 / 3
            }
            bagian += Bagian("Ibu", fraksi)
            totalFurudh += fraksi
        }

        // --- 'Aul: total bagian tetap melebihi 1 -> semua diperkecil proporsional, tidak ada residu. ---
        val adaAul = totalFurudh > 1.0 + 1e-9
        val faktorAul = if (adaAul) 1.0 / totalFurudh else 1.0
        val hasilAkhir = bagian.map { it.copy(fraksi = it.fraksi * faktorAul) }.toMutableList()
        val totalSetelahAul = totalFurudh * faktorAul

        var sisa = (1.0 - totalSetelahAul).coerceAtLeast(0.0)
        var adaAshabah = false

        if (!adaAul && sisa > 1e-9) {
            if (ahli.anakLakiLaki > 0) {
                adaAshabah = true
                val unitAnakPerempuan = if (anakPerempuanJadiAshabah) 1 else 0
                val totalUnit = ahli.anakLakiLaki * 2 + ahli.anakPerempuan * unitAnakPerempuan
                if (totalUnit > 0) {
                    hasilAkhir += Bagian("Anak laki-laki (ashabah)", sisa * ahli.anakLakiLaki * 2 / totalUnit, ahli.anakLakiLaki)
                    if (anakPerempuanJadiAshabah) {
                        hasilAkhir += Bagian(
                            "Anak perempuan (ashabah)",
                            sisa * ahli.anakPerempuan * unitAnakPerempuan / totalUnit,
                            ahli.anakPerempuan,
                        )
                    }
                }
                sisa = 0.0
            } else if (ayahJadiAshabah) {
                adaAshabah = true
                hasilAkhir += Bagian("Ayah (ashabah)", sisa)
                sisa = 0.0
            }
        }

        val butuhRadd = !adaAul && sisa > 1e-9 && !adaAshabah
        return HasilWaris(bagian = hasilAkhir, adaAul = adaAul, butuhRadd = butuhRadd)
    }
}
