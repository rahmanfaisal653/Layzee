# 01: Pinch tahan jarak dan tracking halus

**What to build:** Perketat deteksi pinch supaya tangan jauh tidak memicu swipe palsu, dan haluskan rasio pinch.

**Spec:** `.scratch/polish/spec.md`

**Blocked by:** none

**Status:** ready-for-agent

- [ ] Batas minimum hand size dalam piksel: di bawahnya status tetap Tracking dan pinch tidak dihitung. Nilai awal dapat disetel lewat satu konstanta di `GestureConfig`.
- [ ] Pemicu cadangan menjadi AND dengan jarak ujung (bukan OR) di `GestureEngine`.
- [ ] Debounce masuk 3 frame (`enterFrames`).
- [ ] One Euro filter pada rasio pinch (`PinchMeasure`/`GestureEngine`), selang waktu dari timestamp nyata, tanpa alokasi per frame.
- [ ] Tes JVM baru: frame dengan jitter pada tangan kecil tidak memicu pinch; pinch nyata tetap terdeteksi; tangan di bawah batas minimum tidak memicu.
- [ ] Tes yang ada tetap lulus; `StageTimer` di build debug mencatat waktu tahap logika.
- [ ] Panel kalibrasi memakai ambang pengguna, bukan `GestureConfig()` default (`DashboardActivity.kt` sekitar baris 286).
- [ ] Dot landmark di `LandmarkView` diperkecil dan radiusnya ikut ukuran tangan.
