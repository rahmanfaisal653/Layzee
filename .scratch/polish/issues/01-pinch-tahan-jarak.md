# 01: Pinch tahan jarak dan tracking halus

**What to build:** Perketat deteksi pinch supaya tangan jauh tidak memicu swipe palsu, dan haluskan rasio pinch.

**Spec:** `.scratch/polish/spec.md`

**Blocked by:** none

**Status:** resolved

- [x] Batas minimum hand size dalam piksel: di bawahnya status tetap Tracking dan pinch tidak dihitung. Nilai awal dapat disetel lewat satu konstanta di `GestureConfig`.
- [x] Pemicu cadangan menjadi AND dengan jarak ujung (bukan OR) di `GestureEngine`.
- [x] Debounce masuk 3 frame (`enterFrames`).
- [x] One Euro filter pada rasio pinch (`PinchMeasure`/`GestureEngine`), selang waktu dari timestamp nyata, tanpa alokasi per frame.
- [x] Tes JVM baru: frame dengan jitter pada tangan kecil tidak memicu pinch; pinch nyata tetap terdeteksi; tangan di bawah batas minimum tidak memicu.
- [x] Tes yang ada tetap lulus; `StageTimer` di build debug mencatat waktu tahap logika.
- [x] Panel kalibrasi memakai ambang pengguna, bukan `GestureConfig()` default (`DashboardActivity.kt` sekitar baris 286).
- [x] Dot landmark di `LandmarkView` diperkecil dan radiusnya ikut ukuran tangan.

## Comments

Dikerjakan 2026-10-11.

- `GestureConfig`: `minHandSizePx` (default 30 px pada analisis 320x240, titik awal untuk disetel di HP), `enterFrames` 3, `smoothingMinCutoffHz` 2 dan `smoothingBeta` 1 (titik awal).
- Tangan di bawah batas: status Tracking (pinch yang sedang berjalan berakhir), bukan Idle. Kalibrasi juga membuang frame di bawah batas yang sama.
- Pemicu cadangan AND dengan jarak ujung. Jika kalibrasi mematikan cadangan (0/0), cadangan dilewati supaya pinch tetap bisa mulai.
- One Euro filter dipasang pada dua rasio (ujung dan cadangan), masing-masing satu instance, tanpa alokasi. Menahan pinch (hold) tetap memakai aturan "keduanya terbuka".
- Panel kalibrasi memakai `Calibration.toGestureConfig` (ambang pengguna + sensitivitas), sama seperti service.
- Dot landmark: radius 3% hand size, dibatasi 1-2,5 dp.
- `StageTimer.LOGIC` sudah membungkus `engine.process`, jadi filter ikut tercatat.
- Belum diuji di HP: nilai `minHandSizePx`, cutoff, dan beta perlu disetel di perangkat (latensi pinch ≤ 150 ms).
