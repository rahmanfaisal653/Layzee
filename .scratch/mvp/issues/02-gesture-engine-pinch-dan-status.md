# 02: Gesture engine: pinch dan status

**What to build:** Logika murni (gesture engine, satu-satunya seam tes) yang membaca urutan frame tangan lalu menentukan apakah pengguna sedang pinch, dan menghasilkan status Idle / Tracking / Pinch. Belum ada swipe. Diverifikasi seluruhnya lewat tes JVM dengan urutan frame buatan.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 01 (Kerangka proyek Android)

**Status:** resolved

- [x] Engine menerima per frame: 21 titik tangan dengan skor kepercayaan, waktu frame, ukuran frame sebenarnya, dan konfigurasi ambang pinch.
- [x] Jarak pinch = jarak ujung jempol ke ujung telunjuk dibagi **hand size**, dengan koordinat dikalikan ukuran frame sebenarnya sebelum jarak dihitung. Tes: tangan yang sama di jarak kamera berbeda menghasilkan keputusan pinch yang sama.
- [x] Debounce: pinch baru dihitung setelah bertahan 2-3 frame; pinch sekilas tidak mengubah status.
- [x] Hysteresis: ambang masuk dan keluar pinch berbeda; jarak yang berkedip di sekitar ambang tidak membuat status berkedip.
- [x] Transisi status: Idle → Tracking saat tangan terlihat; Tracking → Pinch saat pinch bertahan; Pinch → Tracking saat pinch dilepas; Pinch → Idle saat tangan hilang.
- [x] Skor kepercayaan jatuh saat Pinch → status keluar dari Pinch (jari virtual dianggap diangkat), tanpa masa tenggang.
- [x] Saat ujung telunjuk sebagian tertutup, pinch tetap terbaca lewat fitur cadangan (jarak ujung jempol ke sendi tengah telunjuk).
- [x] Jalur frame tanpa alokasi objek: hasil ditulis ke wadah keluaran yang dipakai ulang, bukan membuat `List`/`Pair` baru per frame.
- [x] Semua perilaku di atas punya tes JVM yang hanya memeriksa status keluar untuk urutan frame masukan (bukan detail internal).

## Comments

- 2026-10-10: Selesai. Kode di `app/src/main/java/com/layzee/gesture/` (`GestureEngine`, `GestureConfig`, `HandFrame`, `GestureOutput`, `GestureState`), tes di `app/src/test/java/com/layzee/gesture/GestureEngineTest.kt` (21 tes JVM, lulus via `./gradlew :app:testDebugUnitTest`).
  - Antarmuka: `engine.process(frame, config, out)`; `HandFrame` dan `GestureOutput` dibuat sekali lalu diisi ulang tiap frame. `timestampMs` sudah diterima tapi belum dipakai (untuk kecepatan di tiket 03). "Layar disentuh" belum ada, masuk di tiket 03.
  - Debounce dihitung per frame: `enterFrames` dan `releaseFrames` (default 2/2).
  - Skor kepercayaan di bawah `minConfidence` diperlakukan sama dengan tangan hilang, jadi status ke Idle (bukan Tracking), tanpa masa tenggang.
  - Fitur cadangan memakai aturan OR: pinch dimulai jika jarak ujung-ujung ATAU jempol-ke-PIP telunjuk di bawah ambang masuk, dan dilepas hanya jika keduanya di atas ambang keluar. Risiko: kepalan atau jari menekuk bisa memicu pinch palsu lewat fitur cadangan. Ambang cadangan (default 0.30/0.50 hand size) hanyalah titik mulai; harus ikut dikalibrasi (tiket 11) dan diuji di HP.
  - Tangan dengan hand size ~0 diperlakukan sebagai tangan hilang.
  - Review: "tanpa alokasi" (item 18) hanya diverifikasi lewat pembacaan kode, bukan tes (tidak bisa diperiksa lewat status keluaran). Menunggu data HP: `debounce` dihitung per frame, jadi pada fps Idle ~5 fps dua frame bisa ~400 ms; aturan lama berbasis waktu bisa dipertimbangkan di tiket 03/08.
