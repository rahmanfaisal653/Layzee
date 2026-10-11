# 05: Rombak dashboard

**What to build:** Dashboard informatif dan rapi menggantikan tembok teks, dengan label status hidup.

**Spec:** `.scratch/polish/spec.md`

**Blocked by:** 02 (pratinjau lancar), 04 (ikon dan glyph status)

**Status:** resolved

- [x] `colors.xml`, `themes.xml`, `values-night`: gelap-first, terang mengikuti sistem, satu warna aksen, palet status konsisten dengan overlay (Idle/Tracking/Pinch). Tanpa dependensi baru.
- [x] Dua keadaan layar: setup belum lengkap (daftar langkah ringkas dengan ikon status berwarna dan penjelasan yang bisa dilipat) dan layar utama (Start/Stop besar, label status hidup, sensitivitas).
- [x] Label status hidup lewat callback dari service ke dashboard, tanpa polling.
- [x] Kartu status izin dengan tombol pintas ke pengaturan sistem (Accessibility, overlay, kamera, baterai).
- [x] ~~Kalibrasi menampilkan tanggal terakhir dan tombol Ulangi.~~ Tidak berlaku: kalibrasi dihapus (commit 009ac6c), diganti Pinch test.
- [x] ~~Sensitivitas pinch dengan tanda "berlaku saat Start berikutnya".~~ Tidak berlaku: pengaturan sensitivitas dihapus, ambang tetap di `GestureConfig`.
- [x] Bagian Tentang & privasi (diproses lokal, tidak ada izin internet, versi app, alasan Accessibility) dan cara pakai 3 baris.
- [x] Daftar 40 band warna dihapus; pratinjau kamera dengan landmark dan tombol lain dipertahankan. (Daftar uji pinch tetap ada karena `CLAUDE.md` memintanya, tapi kini baris bernomor dengan warna tema, bukan band warna.)
- [x] Tanpa tampilan angka latensi.
- [x] Ukuran APK rilis dicatat setelah perubahan: arm64-v8a 20 MB, armeabi-v7a 16 MB, x86_64 23 MB (didominasi MediaPipe; tidak ada baseline sebelum perubahan, tidak ada dependensi baru).

## Comments

- 2026-10-11: Ditutup atas keputusan pengguna ("sudah cukup"). Kode selesai; uji di HP Realme tidak dicatat sebagai verifikasi terpisah.
