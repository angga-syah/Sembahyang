# Sembahyang — Jadwal Sholat, Kiblat & Al-Qur'an

Aplikasi Android native (Kotlin + Jetpack Compose) untuk ibadah sehari-hari: jadwal sholat,
arah kiblat, Al-Qur'an dengan tajwid berwarna + tafsir, dzikir, dan beberapa kalkulator
ibadah. Dirancang **offline-first** dan ringan untuk hp lama (minSdk 21 / Android 5.0),
tanpa bergantung pada Google Play Services.

## Fitur

- **Jadwal sholat** — perhitungan lokal via [adhan-kotlin](https://github.com/batoulapps/adhan-kotlin),
  termasuk parameter Kemenag RI (ihtiyat + pembulatan), notifikasi & adzan (audio penuh, bukan
  cuma bunyi notifikasi), widget home screen
- **Arah kiblat** — kompas berbasis sensor perangkat
- **Al-Qur'an** — teks Arab, terjemahan ID/EN, transliterasi latin, **pewarnaan tajwid runtime**
  (dihitung langsung dari teks, bukan dataset pre-indexed), tafsir ringkas Kemenag per ayat,
  panduan 16 kaidah tajwid, pencarian surah/ayat, audio murottal (unduh sesuai kebutuhan)
- **Dzikir** — Al-Ma'tsurat pagi/petang
- **Amal harian** — checklist ibadah harian
- **Puasa** — pencatat puasa sunnah/wajib
- **Kalender Hijriah**
- **Tasbih** — penghitung digital dengan getar
- **Zakat** — kalkulator zakat fitrah & maal
- **Waris (faraid)** — kalkulator warisan Islam untuk ahli waris primer (pasangan, anak, ayah,
  ibu), termasuk kasus 'aul dan gharrawain — **belum mencakup kakek/nenek/saudara**, lihat
  disclaimer di dalam aplikasi

## Status

Fitur inti sudah lengkap dan teruji (lihat `app/src/test`). Belum ada: kamus Arab-Indonesia,
Hisnul Muslim — keduanya tertahan karena belum ditemukan sumber data terbuka yang lisensinya
jelas.

## Lisensi

Kode sumber repositori ini: **MIT** — lihat [LICENSE](LICENSE).

Data & aset pihak ketiga yang dibundel (teks Qur'an, tafsir, font, audio, dzikir) **tidak**
ikut lisensi MIT — masing-masing sumber punya status izin sendiri, sebagian masih dalam proses
konfirmasi resmi ke pemiliknya. **Baca [NOTICE.md](NOTICE.md) sebelum menggunakan ulang atau
mendistribusikan ulang isi `app/src/main/assets/` atau `app/src/main/res/font/`.**

## Menjalankan

Butuh Android Studio, atau JDK 17+ & Android SDK dengan `platforms;android-35` terpasang.

```bash
./gradlew assembleDebug
```

APK debug akan ada di `app/build/outputs/apk/debug/app-debug.apk`.

Menjalankan test unit:

```bash
./gradlew testDebugUnitTest
```

## Struktur

```
app/src/main/java/id/linuwih/sholat/
  prayer/   # jadwal sholat, notifikasi/adzan, widget
  qibla/    # kompas & arah kiblat
  quran/    # teks + terjemahan + tajwid + tafsir + audio
  ibadah/   # dzikir, amal harian, puasa, tasbih, zakat, waris, kalender hijriah
  ui/       # tema, navigasi bersama, komponen shared
  util/     # helper lintas fitur (getar, dll.)
app/src/main/assets/     # data Qur'an, dzikir (lihat ATTRIBUTION.md masing-masing)
app/src/test/            # unit test
```

## Ketergantungan eksternal

Semua fitur inti (jadwal sholat, kiblat, baca Qur'an + terjemahan) berjalan **100% offline,
tanpa server**. Panggilan ke pihak luar hanya dua: download audio murottal on-demand dari CDN
publik, dan pemeriksaan pembaruan aplikasi ke GitHub Releases (sekali sehari, bisa dimatikan di
layar Tentang).

## Rilis & pembaruan otomatis

Aplikasi memeriksa `releases/latest` repo ini, membandingkan `tag_name` (mis. `v0.2.0`) dengan
`versionName` terpasang, lalu mengunduh berkas `.apk` pertama di rilis itu. Supaya pembaruan
bisa dipasang: buat rilis dengan tag `vX.Y.Z` yang sama dengan `versionName`, naikkan
`versionCode`, dan lampirkan APK rilis yang ditandatangani kunci yang sama.

## Kontribusi

Repo ini masih tahap awal untuk publikasi terbuka — issue dan PR dipersilakan, tapi tolong
baca [NOTICE.md](NOTICE.md) dulu kalau kontribusinya menyentuh data/aset di `assets/` atau
`res/font/`.
