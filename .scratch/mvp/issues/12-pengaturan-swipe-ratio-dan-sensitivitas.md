# 12: Pengaturan: swipe ratio dan sensitivitas pinch

**What to build:** Pengaturan inti di Dashboard yang tersimpan di perangkat: pinch sensitivity (menggeser sedikit hasil kalibrasi ke lebih longgar atau lebih ketat). Swipe ratio dulu ikut di tiket ini tetapi sudah dihapus (lihat komentar).

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 11 (Kalibrasi)

**Status:** resolved

- [x] Dashboard punya slider pinch sensitivity dengan nilai default yang wajar (tengah = hasil kalibrasi apa adanya). Slider swipe ratio tidak dibuat: swipe ratio sudah dihapus.
- [x] Pinch sensitivity hanya menggeser sedikit ambang hasil kalibrasi; tidak menggantikannya dan tidak memerlukan kalibrasi ulang.
- [x] Nilai sensitivitas tersimpan di perangkat dan tidak perlu diatur ulang tiap Start.
- [x] Perubahan berlaku paling lambat saat Start berikutnya.
- [x] ~~Diverifikasi di playground: swipe ratio~~ Tidak berlaku lagi (swipe ratio dihapus; pinch = satu flick tetap).
- [x] Diverifikasi di HP uji: sensitivitas lebih ketat mengurangi pinch tak sengaja, lebih longgar membuat pinch lebih mudah terbaca.
- [x] Tidak ada pengaturan "kecepatan scroll" atau "pilihan kamera" (di luar scope).

## Comments

- 2026-10-10: Swipe ratio tidak ada lagi (pinch = satu flick tetap, lihat komentar di tiket 07). Yang tersisa untuk tiket ini: sensitivitas pinch, dan bila perlu jarak/kecepatan flick (`flickDistanceFraction`, durasi di `SwipeDispatcher`) sebagai pengaturan. Tinjau ulang lingkup tiket ini sebelum dikerjakan.
- 2026-10-11: Lingkup ditinjau: hanya sensitivitas pinch. Pengaturan jarak/kecepatan flick tidak ditambah (di luar daftar "Masuk" CLAUDE.md; pertimbangkan lagi bila uji per app menunjukkan perlu).
- Diimplementasi. `Calibration.withSensitivity(level)` (level 0..10, 5 = netral) mengalikan keempat ambang dengan 1 + (level-5) x 3%, jadi maksimal +-15%: enter tetap di bawah exit, cadangan yang mati tetap mati, hasil kalibrasi tersimpan tidak diubah. Level disimpan `SensitivityStore` (SharedPreferences `layzee_settings`), dibaca `LayzeeService.gestureConfig()` tiap Start. Slider di Dashboard (di atas tombol Start); bila digeser saat Layzee berjalan muncul catatan "berlaku saat Start berikutnya". Tes unit ditambah di `CalibrationTest`; unit test dan `assembleDebug` lolos.
- Belum dicentang: verifikasi di HP uji (sensitivitas lebih ketat mengurangi pinch tak sengaja, lebih longgar membuat pinch lebih mudah terbaca). Itu sebabnya status `ready-for-human`. Besar langkah (3% per tingkat) adalah titik awal, setel lewat pemakaian nyata.
- 2026-10-11: Diverifikasi di HP uji oleh pengguna: sensitivitas aman dipakai. Tiket selesai.
