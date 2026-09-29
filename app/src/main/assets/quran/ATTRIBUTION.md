# Sumber data Al-Qur'an

**Teks Arab, transliterasi Latin (gaya Inggris), terjemahan Indonesia & Inggris, metadata surah**
— risan/quran-json — https://github.com/risan/quran-json
Oleh Risan Bagja Pradana — https://risanb.com
Dilisensikan **CC BY-SA 4.0**: https://creativecommons.org/licenses/by-sa/4.0/

**Transliterasi Latin (gaya Indonesia, `quran_latin_id.json`)**
— Diambil dari API publik **equran.id** (https://equran.id/apidev), diunduh sekali per-surah lalu
dibundel offline (bukan dipanggil live, supaya baca Qur'an tetap 100% offline — lihat plan).
**Catatan (diperbarui setelah membaca Terms of Service lengkap di equran.id/terms)**: ToS mereka
secara eksplisit mengizinkan "Menggunakan API untuk pengembangan aplikasi" (poin 3) — jadi
pemakaian ini SUDAH berdasar izin tertulis mereka, bukan area abu-abu seperti dugaan awal.

Yang masih perlu diperhatikan: poin 7 ToS menyatakan "Teks Al-Qur'an adalah milik umat Islam dan
bebas digunakan. Namun, **terjemahan, tafsir, audio, dan fitur aplikasi yang dikembangkan
EQuran.id dilindungi hak cipta**." Transliterasi Latin yang dipakai di sini kemungkinan termasuk
kategori "fitur yang dikembangkan EQuran.id" (bukan teks Qur'an murni), jadi tetap hak cipta
mereka — tapi poin 3 di atas sudah mengizinkan pemakaiannya untuk pengembangan aplikasi.

Satu hal yang TIDAK disebut eksplisit di ToS: apakah boleh diambil SEKALI lalu dibundel offline
permanen (seperti dilakukan di sini), atau harus dipanggil live tiap kali. Kalau ingin
memastikan, kontak: **admin@equran.id**.

**Tajwid berwarna (teks Arab + anotasi aturan, `quran_ar_tajweed.json` & `quran_tajweed_rules.json`)**
— semarketir/quranjson — https://github.com/semarketir/quranjson
Dilisensikan **MIT License**. Teks Arab di file ini adalah varian Utsmani tersendiri (bukan
risan/quran-json) — dipakai khusus untuk mode tajwid berwarna supaya index karakter aturan
tajwid selaras persis dengan teksnya (sudah diverifikasi manual: total 6236 ayat, batas index
tiap aturan tidak melebihi panjang teks). Skema warna tajwid mengikuti konvensi umum yang
didokumentasikan di alquran.cloud/tajweed-guide (madd = biru, qalqalah = merah, ikhfa = ungu,
idgham = hijau, ghunnah = oranye, huruf/tanda diam = abu-abu).

**Teks Arab gaya Indonesia (Mushaf Standar Indonesia) + anotasi tajwid
(`quran_ar_kemenag.json` & `quran_tajweed_rules_kemenag.json`)**
— kodepandai/colorful-quran — https://github.com/kodepandai/colorful-quran
Dilisensikan **MIT License**; data Qur'an-nya berasal dari Kemenag RI.

Dipakai untuk gaya mushaf "Indonesia (Kemenag/MSI)" yang bisa dipilih user di layar baca surah.
Berbeda dari rasm Utsmani internasional, MSI menuliskan tanda baca secara eksplisit — mis.
hamzah washal diberi harakat (`اِهْدِنَا`, bukan `ٱهْدِنَا`) — supaya bisa dibaca benar tanpa
harus menguasai kaidah Arab lebih dulu.

Anotasi tajwid di repo itu sudah tersedia jadi (bukan dihitung saat runtime) dalam bentuk
`{class, start, end}`, dan sudah diverifikasi manual saat dikonversi: **6.236 ayat, 0 index di
luar batas panjang teks**, jumlah ayat tiap surah cocok dengan metadata surah yang sudah ada.
4 rentang nol-lebar (start == end) dibuang saat konversi karena tidak menandai apa pun.
13 nama aturan tajwidnya dipetakan ke warna yang sama dengan skema Utsmani supaya arti warna
tetap konsisten saat user berganti gaya mushaf.

**Catatan jujur**: MIT License repo `kodepandai/colorful-quran` berlaku untuk KODE-nya, BUKAN
untuk data Qur'an Kemenag di dalamnya — MIT tidak bisa melisensikan ulang konten yang bukan
milik pemegang repo. **Ada API resmi Qur'an Kemenag** yang dikelola LPMQ, di
https://quran-api.lpmqkemenag.id — aksesnya membutuhkan **surat permohonan tertulis** ke Kepala
Lajnah Pentashihan Mushaf Al-Qur'an, dikirim ke lajnah@kemenag.go.id (cc quran.kemenag@gmail.com).
Data yang dipakai di sini diambil pihak ketiga (`kodepandai/colorful-quran`), BUKAN lewat jalur
resmi itu. Surat permohonan itu belum diajukan. Sebelum app ini dipublikasikan luas, izin
pembundelan wajib dikonfirmasi lewat jalur resmi tsb, bukan diasumsikan aman karena kodenya MIT.

**Tafsir Ringkas Kemenag (`tafsir_ringkas_kemenag.json`)**
— renomureza/quran-api-id — https://github.com/renomureza/quran-api-id
Dilisensikan **MIT License**. Datanya sendiri (`src/data/tmp/tafsir-kemenag.json`) di-scrape
langsung dari situs resmi quran.kemenag.go.id lewat `tafsirKemenagScraper.js` di repo tersebut.

Repo sumber menyediakan DUA varian: "Tafsir Ringkas Kemenag" (field `short`, satu-dua paragraf
per ayat) dan "Tafsir Kemenag" lengkap (field `long`, bisa berhalaman-halaman per ayat). App ini
HANYA memakai varian ringkas — supaya ukuran berkas & UI tetap cocok untuk mobile, bukan karena
raguan validitas versi lengkapnya.

Dikonversi dari struktur asli (array datar `{short:{...}, long:{...}}` per ayat, dikunci
`aya_name` mis. "1:1") ke skema yang sama dengan `quran_id.json` (`{chapter, verse, text}` per
surah) supaya konsisten dengan loader yang sudah ada. Markup HTML (`<p>`, `<em>`, entity `&nbsp;`
dkk. — sisa dari hasil scraping halaman web) dibersihkan jadi teks polos saat konversi.
Diverifikasi: 6.236 ayat, 114 surah, cocok dengan jumlah ayat metadata surah yang sudah ada.

**Catatan jujur yang sama seperti sumber Kemenag lain di atas**: MIT License repo
`renomureza/quran-api-id` berlaku untuk KODE-nya (server API, script build), bukan untuk konten
tafsir yang di-scrape dari quran.kemenag.go.id di dalamnya. Sama seperti Teks Arab Kemenag di
atas — jalur resmi untuk data ini adalah API LPMQ (quran-api.lpmqkemenag.id, butuh surat
permohonan ke Kepala LPMQ), BUKAN scraping pihak ketiga. Surat permohonan belum diajukan.
Sebelum app ini dipublikasikan luas, izin pembundelan wajib dikonfirmasi lewat jalur resmi itu.

**Ringkasan status izin (per commit ini)**:

- **Teks Arab Kemenag & Tafsir Ringkas Kemenag** — diambil lewat pihak ketiga (`kodepandai/colorful-quran`, `renomureza/quran-api-id`), BUKAN lewat API resmi LPMQ. Status hukumnya BELUM dikonfirmasi resmi — WAJIB kirim surat permohonan (lajnah@kemenag.go.id, cc quran.kemenag@gmail.com) sebelum publikasi luas.
- **Transliterasi Latin Indonesia (equran.id)** — BEDA status dari dua di atas: ToS equran.id (dibaca lengkap, bukan cuma ringkasan pencarian) secara eksplisit mengizinkan "menggunakan API untuk pengembangan aplikasi". Ini sudah berdasar izin tertulis mereka sendiri, meski soal bundling-offline-permanen spesifiknya tidak disebut jelas — bisa dikonfirmasi ke admin@equran.id kalau ingin benar-benar pasti, tapi risikonya jauh lebih rendah dari dua butir Kemenag di atas.

Ini bukan berarti data Kemenag di atas pasti bermasalah — banyak proyek open-source Qur'an
Indonesia memakai pola yang sama tanpa masalah dilaporkan — tapi status hukumnya belum
dikonfirmasi resmi untuk dua butir pertama.

Semua sumber sudah dicantumkan di layar "Tentang" aplikasi.
