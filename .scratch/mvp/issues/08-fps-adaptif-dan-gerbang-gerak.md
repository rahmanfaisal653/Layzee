# 08: FPS adaptif dan gerbang gerak

**What to build:** Penghematan beban saat Layzee menyala: fps naik-turun mengikuti status, dan model dilewati saat Idle kalau tidak ada pergerakan. Tujuannya kamera yang menyala terus tidak membebani baterai dan CPU, tanpa membuat pinch terlambat dikenali.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 07 (Foreground service dan Start/Stop)

**Status:** resolved

- [x] Sekitar 5 fps saat Idle, 15-20 fps saat Tracking, dan tertinggi saat Pinch.
- [x] Setelah tangan hilang, fps tetap tinggi beberapa detik lalu turun ke fps Idle; lama jeda ini bisa disetel di satu tempat.
- [x] Gerbang gerak saat Idle: frame abu-abu sangat kecil (sekitar 64x48) dibandingkan antar frame, dan model dilewati bila selisihnya kecil.
- [x] Rentang fps kamera dikunci (`CONTROL_AE_TARGET_FPS_RANGE`) sehingga fps tidak turun di cahaya redup.
- [x] Tangan yang baru muncul di depan kamera dikenali sebagai Tracking dalam waktu wajar (tidak terlewat karena gerbang gerak); waktunya terlihat di log debug.
- [x] Waktu per tahap di log debug menunjukkan inferensi dilewati saat gerbang gerak menahan.
- [x] Tidak membangun profil performa otomatis, pemantauan suhu, GPU delegate, atau ROI crop (di luar scope).

## Comments

- 2026-10-10: Dikerjakan agen. Semua penyetelan ada di `PacingConfig` (`pipeline/FramePacer.kt`): Idle 5 fps, Tracking 20, Pinch 30, jeda `holdMs` 3 detik setelah tangan hilang, jalan paksa model tiap 2 detik saat Idle, ambang gerbang gerak (piksel berubah > 24 level pada >= 2% dari grid 64x48).
  - `FramePacer` (murni, diuji unit: 10 tes) memutuskan per frame kamera: lewati karena laju (`SKIP_RATE`), lewati karena gerbang gerak (`SKIP_MOTION`), atau teruskan. Frame dibuang di `CameraPipeline.analyze` sebelum konversi dan inferensi. Gerbang membandingkan dengan frame terakhir yang dikirim ke model (bukan frame sebelumnya), jadi gerak pelan menumpuk.
  - Rentang fps kamera dikunci lewat `Camera2Interop` (`CONTROL_AE_TARGET_FPS_RANGE`), dipilih oleh `FpsRanges.choose`: puncak tertinggi <= 30, batas bawah terendah >= 15 (di HP uji umumnya [15,30]).
  - Log debug: `LayzeeTiming` kini ditutup tiap 60 frame atau 5 detik dan menampilkan `skipped(rate=N motion=M)`; `LayzeePacing` mencatat berapa ms dari gerak pertama yang membuka gerbang sampai hasil non-Idle pertama.
  - Diverifikasi di HP (Realme RMX3241) lewat `adb logcat`: saat Idle ada jendela dengan `modelFps=1,0 ... skipped(rate=75 motion=20)` (inferensi dilewati), dan `hand seen 417 ms after the motion gate opened` saat tangan muncul. Angka baterai/CPU belum diukur (tiket 13).
  - Batasan: kamera sendiri tetap berjalan di rentang terkunci (>= 15 fps); yang dihemat adalah konversi + inferensi. Pratinjau Dashboard ikut turun ke ~5 fps saat Idle (sengaja tidak dilewati; tiket 11 perlu memutuskan apakah kalibrasi butuh pacing dimatikan). Ambang gerbang gerak (24 level, 2%) baru titik mulai; belum disetel di berbagai cahaya.
