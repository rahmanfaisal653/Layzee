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
- [ ] APK rilis diperiksa: tidak ada izin internet.
- [ ] Anggaran terukur ditulis di bagian Anggaran "Ringan" pada `CLAUDE.md`.
