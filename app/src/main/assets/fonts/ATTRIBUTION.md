# Sumber font

Font DIPASANGKAN dengan gaya mushaf, bukan dipilih bebas — lihat dokumentasi `ArabicFont` dan
`ArabicFontCoverageTest`. Tidak ada satu font pun yang meng-cover kedua gaya sekaligus:
teks Kemenag/MSI memakai 5 karakter khas Indonesia yang tidak ada di Amiri (a.l. U+08D6
ARABIC SMALL HIGH AIN, 560 kemunculan), dan teks Utsmani memakai U+0671 ALEF WASLA
(13.483 kemunculan) yang tidak ada di LPMQ.

## LPMQ Isep Misbah — dipakai untuk mushaf Indonesia (Kemenag/MSI)

`res/font/lpmq_isep_misbah.ttf` — versi 1.00 (April 2018)
Copyright (c) 2018, **Lajnah Pentashihan Mushaf Al-Qur'an** <lajnah@kemenag.go.id>
Perancang: M Zamroni Ahbab, dari khat tulisan tangan kaligrafer H. Isep Misbah, MA.
https://lajnah.kemenag.go.id/

Font resmi Kemenag/LPMQ, dirilis 28 Maret 2018 khusus untuk penulisan Mushaf Standar
Indonesia — berisi penyesuaian dan penambahan karakter khas yang hanya ada di MSI. Inilah
sebabnya ia dipakai untuk teks Kemenag: cakupannya 100%, sementara font rasm Utsmani mana pun
akan bolong.

Berkas diambil dari https://github.com/utrodus/flutter-ayatnesia-quran-search (lisensi MIT),
aplikasi Qur'an Indonesia yang membundelnya di `assets/fonts/LPMQ-IsepMisbah.ttf`.
SHA-256: `b0927593ebd07550b879c31e36085d1bb44f7a066b9824fad2b62822b7887e21`
Keaslian dicek lewat metadata font: nama, copyright LPMQ, dan tanda tangan digital (tabel DSIG).

**Catatan jujur**: font ini TIDAK menyertakan record lisensi di dalam berkasnya (name ID 13/14
kosong), jadi tidak ada teks lisensi formal seperti OFL. Yang ada adalah pernyataan publik
Kemenag bahwa font ini "dapat digunakan oleh seluruh pengembang aplikasi Al-Qur'an secara
gratis, dan juga dapat digunakan oleh masyarakat umum"
(https://www.kemenag.go.id/berita/read/507457/kemenag-rilis-font-mushaf-standar-indonesia-untuk-penulisan-alquran).
Sama seperti data equran.id dan Kemenag di `assets/quran/ATTRIBUTION.md`: **sebelum publikasi
luas, konfirmasi dulu ke LPMQ** apakah pembundelan di dalam APK termasuk yang diizinkan.

## Amiri Quran — dipakai untuk mushaf internasional (Utsmani)

`res/font/amiri_quran.ttf` — https://github.com/aliftype/amiri (rilis 1.003)
Lisensi **SIL Open Font License 1.1** — https://openfontlicense.org/
Copyright 2010-2022 The Amiri Project Authors.

Cakupannya 100% terhadap teks Utsmani. Jangan dipakai untuk teks Kemenag.
