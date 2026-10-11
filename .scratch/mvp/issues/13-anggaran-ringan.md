# 13: Anggaran "ringan"

**What to build:** Pengukuran manual oleh manusia terhadap produk yang sudah lengkap, lalu penetapan anggaran "ringan" yang terukur. Juga memeriksa apakah kamera yang menyala terus (tanpa auto-Off) masih masuk anggaran baterai.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 08 (FPS adaptif dan gerbang gerak), 09 (Overlay), 12 (Pengaturan: swipe ratio dan sensitivitas pinch)

**Status:** ready-for-human

- [ ] Ukuran APK per ABI, CPU rata-rata saat Tracking, persen baterai per jam, dan suhu HP setelah 15 menit diukur di HP uji.
- [ ] Latensi (pinch sampai layar bergerak) dan waktu gerak berhenti setelah pinch dilepas diukur dari log debug dan dibandingkan dengan batas awal.
- [ ] Hasil dibandingkan dengan batas awal sementara di spec; batas direvisi bila perlu.
- [ ] Dicek apakah kamera yang menyala terus tanpa auto-Off melewati anggaran baterai; bila ya, diskusi auto-Off dibuka lagi.
- [ ] Layzee bertahan di latar belakang di HP uji setelah panduan baterai/autostart dijalankan, dan tidak terbunuh sistem dalam pemakaian normal.
- [x] APK rilis diperiksa: tidak ada izin internet.
- [ ] Anggaran terukur ditulis di bagian Anggaran "Ringan" pada `CLAUDE.md`.

## Comments

- 2026-10-11: Bagian yang bisa diukur tanpa pemakaian manusia dikerjakan; status tetap `ready-for-human` karena sisanya butuh pengukuran di HP uji (Realme RMX3241, arm64-v8a) oleh pengguna.
  - Ukuran APK rilis (`./gradlew assembleRelease`, R8 + shrinkResources, ABI splits): arm64-v8a 20.919.874 B (~20,9 MB), armeabi-v7a 16.674.988 B (~16,7 MB), x86_64 24.180.297 B (~24,2 MB). HP uji memakai arm64-v8a. Belum dicentang karena item itu juga menyebut CPU, baterai, dan suhu.
  - Izin APK rilis (`aapt2 dump permissions`, ketiga ABI): CAMERA, FOREGROUND_SERVICE, FOREGROUND_SERVICE_CAMERA, POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW. Tidak ada INTERNET maupun ACCESS_NETWORK_STATE. Item dicentang.
  - Masih menunggu pengguna: CPU rata-rata saat Tracking, persen baterai per jam, suhu setelah 15 menit, latensi (perlu cara ukur selain log `dispatch`, lihat `spike-findings.md`), uji bertahan di latar belakang, revisi batas awal, keputusan auto-Off, dan penulisan angka ke `CLAUDE.md`.
