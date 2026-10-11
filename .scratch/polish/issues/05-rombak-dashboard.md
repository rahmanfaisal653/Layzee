# 05: Rombak dashboard

**What to build:** Dashboard informatif dan rapi menggantikan tembok teks, dengan label status hidup.

**Spec:** `.scratch/polish/spec.md`

**Blocked by:** 02 (pratinjau lancar), 04 (ikon dan glyph status)

**Status:** ready-for-agent

- [ ] `colors.xml`, `themes.xml`, `values-night`: gelap-first, terang mengikuti sistem, satu warna aksen, palet status konsisten dengan overlay (Idle/Tracking/Pinch). Tanpa dependensi baru.
- [ ] Dua keadaan layar: setup belum lengkap (daftar langkah ringkas dengan ikon status berwarna dan penjelasan yang bisa dilipat) dan layar utama (Start/Stop besar, label status hidup, sensitivitas).
- [ ] Label status hidup lewat callback dari service ke dashboard, tanpa polling.
- [ ] Kartu status izin dengan tombol pintas ke pengaturan sistem (Accessibility, overlay, kamera, baterai).
- [ ] Kalibrasi menampilkan tanggal terakhir dan tombol Ulangi.
- [ ] Sensitivitas pinch dengan tanda "berlaku saat Start berikutnya".
- [ ] Bagian Tentang & privasi (diproses lokal, tidak ada izin internet, versi app, alasan Accessibility) dan cara pakai 3 baris.
- [ ] Daftar 40 band warna dihapus; pratinjau kamera dengan landmark dan tombol lain dipertahankan.
- [ ] Tanpa tampilan angka latensi.
- [ ] Ukuran APK rilis dicatat setelah perubahan.
