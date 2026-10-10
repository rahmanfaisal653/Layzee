# 08: FPS adaptif dan gerbang gerak

**What to build:** Penghematan beban saat Layzee menyala: fps naik-turun mengikuti status, dan model dilewati saat Idle kalau tidak ada pergerakan. Tujuannya kamera yang menyala terus tidak membebani baterai dan CPU, tanpa membuat pinch terlambat dikenali.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 07 (Foreground service dan Start/Stop)

**Status:** ready-for-agent

- [ ] Sekitar 5 fps saat Idle, 15-20 fps saat Tracking, dan tertinggi saat Pinch.
- [ ] Setelah tangan hilang, fps tetap tinggi beberapa detik lalu turun ke fps Idle; lama jeda ini bisa disetel di satu tempat.
- [ ] Gerbang gerak saat Idle: frame abu-abu sangat kecil (sekitar 64x48) dibandingkan antar frame, dan model dilewati bila selisihnya kecil.
- [ ] Rentang fps kamera dikunci (`CONTROL_AE_TARGET_FPS_RANGE`) sehingga fps tidak turun di cahaya redup.
- [ ] Tangan yang baru muncul di depan kamera dikenali sebagai Tracking dalam waktu wajar (tidak terlewat karena gerbang gerak); waktunya terlihat di log debug.
- [ ] Waktu per tahap di log debug menunjukkan inferensi dilewati saat gerbang gerak menahan.
- [ ] Tidak membangun profil performa otomatis, pemantauan suhu, GPU delegate, atau ROI crop (di luar scope).
