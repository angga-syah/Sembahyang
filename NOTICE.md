# Lisensi & Atribusi

**Kode sumber** repositori ini dilisensikan **MIT** — lihat [LICENSE](LICENSE).

**Data & aset pihak ketiga yang dibundel di dalam aplikasi TIDAK ikut lisensi MIT
di atas.** Masing-masing punya lisensi/status izinnya sendiri, dirinci di berkas
ATTRIBUTION.md per-folder aset:

- [`app/src/main/assets/quran/ATTRIBUTION.md`](app/src/main/assets/quran/ATTRIBUTION.md)
  — teks Al-Qur'an, terjemahan, tajwid, tafsir
- [`app/src/main/assets/fonts/ATTRIBUTION.md`](app/src/main/assets/fonts/ATTRIBUTION.md)
  — font Arab (LPMQ Isep Misbah, Amiri Quran)
- [`app/src/main/assets/dzikir/ATTRIBUTION.md`](app/src/main/assets/dzikir/ATTRIBUTION.md)
  — teks dzikir Al-Ma'tsurat

Audio murottal & rekaman adzan diambil dari CDN pihak ketiga saat dipakai (tidak
dibundel di repo ini), dirinci di layar "Tentang" aplikasi.

## Ringkasan status izin (per commit ini — cek tanggal commit untuk update terbaru)

| Data | Lisensi/status | Aman dipakai ulang? |
|---|---|---|
| Teks Arab Utsmani + terjemahan (risan/quran-json) | CC BY-SA 4.0 | ✅ Ya, dengan atribusi + share-alike |
| Teks & aturan tajwid Utsmani (semarketir/quranjson) | MIT | ✅ Ya |
| Font Amiri Quran | SIL OFL 1.1 | ✅ Ya |
| Dzikir Al-Ma'tsurat (trianbasofi/almatsurat-json) | MIT | ✅ Ya |
| Library adhan-kotlin (jadwal sholat) | MIT | ✅ Ya |
| Rekaman adzan (Wikimedia Commons) | CC0 1.0 | ✅ Ya, domain publik |
| everyayah.com (audio cadangan) | Wajib tautan balik | ✅ Sudah dipenuhi (lihat layar Tentang) |
| Transliterasi Latin Indonesia (equran.id) | ToS izinkan pakai API untuk app | ⚠️ Cukup jelas, belum 100% eksplisit soal bundling offline permanen |
| **Teks Arab Kemenag + Tafsir Ringkas Kemenag** | Diambil pihak ketiga, BUKAN API resmi LPMQ | ❌ **BELUM ada izin tertulis — jangan asumsikan aman** |
| **Font LPMQ Isep Misbah** | Diunduh dari situs resmi LPMQ, tapi tanpa teks lisensi formal | ⚠️ Perlu konfirmasi redistribusi |
| **Audio murottal Quran.com** | "Personal non-commercial", retensi maks 7 hari tanpa izin tertulis; app ini simpan permanen | ❌ **Berpotensi melanggar — perlu izin tertulis** |

**Item bertanda ❌/⚠️ WAJIB dikonfirmasi ke pemiliknya sebelum repo ini
dipublikasikan secara terbuka atau aplikasi didistribusikan luas.** Surat/email
permohonan sudah disiapkan (LPMQ: `lajnah@kemenag.go.id`, cc `quran.kemenag@gmail.com`;
Quran.com: `support@quran.com`) — cek riwayat proyek untuk draftnya.

**Kalau memfork/menyalin repo ini**: jangan asumsikan seluruh isi folder
`app/src/main/assets/` dan `app/src/main/res/font/` bebas dipakai ulang hanya
karena repo berlisensi MIT. Baca tabel di atas dan ATTRIBUTION.md terkait dulu.
