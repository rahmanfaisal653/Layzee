# 02: Pratinjau kamera lancar

**What to build:** Selama pratinjau dashboard/kalibrasi terbuka, pacer dan gerbang gerak dimatikan supaya gambar tidak patah saat tangan tidak terlihat.

**Spec:** `.scratch/polish/spec.md`

**Blocked by:** none

**Status:** resolved

- [x] `CameraPipeline` menerima mode pratinjau yang melewati `FramePacer.admit` (semua frame diteruskan).
- [x] Mode berjalan di service tidak berubah.
- [x] Pratinjau hanya aktif saat layar itu terbuka dan Layzee Off (seperti sekarang).
- [ ] Diverifikasi di HP uji: gambar tidak patah saat tangan keluar frame.

Catatan implementasi: `CameraPipeline(preview = true)` dipakai `DashboardActivity.openCalibration`; `analyze()` melewati `pacer.admit` (semua frame diteruskan, STRATEGY_KEEP_ONLY_LATEST tetap membuang frame lama). `LayzeeService` tidak memakai parameter ini. Build dan unit test lolos; tidak ada unit test baru karena perubahan ada di lapisan CameraX.

## Comments

- 2026-10-11: Ditutup atas keputusan pengguna ("sudah cukup"). Kode selesai; uji di HP Realme tidak dicatat sebagai verifikasi terpisah.
