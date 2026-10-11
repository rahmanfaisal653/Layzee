# Polish: dashboard, ikon, pinch lebih tahan jarak, hemat baterai

Hasil sesi grilling 2026-10-11. Tujuan pengguna: **ringan, tidak boros baterai, frame lancar, tracking halus**. Keputusan lengkap ada di `CLAUDE.md` dan `CONTEXT.md`; rencana yang ditunda ada di `deferred.md`.

## Keputusan

- Desain visual dan ikon masuk MVP. Tidak ada pemilih tema; gelap-first, terang mengikuti sistem. Tanpa dependensi UI baru.
- Dashboard dirombak jadi informatif dan rapi: dua keadaan layar (setup belum lengkap, dan layar utama), label status hidup lewat callback dari service (tanpa polling), tombol/section: status izin berikon dengan tombol pintas, kalibrasi (tanggal terakhir + Ulangi), sensitivitas pinch, Tentang & privasi, cara pakai 3 baris.
- Daftar 40 band warna dihapus. Pratinjau kamera dengan landmark dan tombol lain dipertahankan.
- Pinch palsu saat tangan jauh: dot landmark diperkecil dan skalanya ikut hand size; panel kalibrasi memakai ambang pengguna (bug: sekarang `GestureConfig()` default); batas minimum hand size; pemicu cadangan AND; debounce 3 frame; One Euro filter pada rasio.
- Kamera dilepas saat layar mati, dipasang lagi saat layar menyala. Tanpa status baru.
- Pratinjau dashboard/kalibrasi bebas dari pacer dan gerbang gerak. Mode berjalan di app lain tidak berubah (Instagram dilaporkan lancar). Rentang fps kamera 15-30 tidak diubah.
- Ditunda: pengukuran baseline, world landmarks dibanding 2D (`deferred.md`).

## Di luar scope

Statistik, pemilih tema, tutorial panjang, reset posisi overlay, GPU delegate, ROI crop, rentang fps kamera per status.
