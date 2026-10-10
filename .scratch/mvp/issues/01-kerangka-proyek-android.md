# 01: Kerangka proyek Android

**What to build:** Proyek Android Kotlin satu modul yang bisa dibangun dan dipasang di HP uji (Realme), dengan fondasi ringan sejak awal: batas versi, shrink, pemisahan APK per ABI, tanpa izin internet, dan jalur menjalankan tes JVM untuk seam gesture engine. Hasilnya app kosong yang terbuka, tetapi semua tiket berikutnya berdiri di atasnya.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [x] Proyek membangun APK debug dan rilis tanpa error.
- [ ] APK terpasang dan terbuka di HP uji (layar kosong tidak masalah).
- [x] `minSdk` 26 dan `targetSdk` di atas 29.
- [x] R8 shrink aktif di build rilis dan ABI splits menghasilkan APK per ABI.
- [x] Manifest tidak meminta izin internet (diverifikasi pada APK yang dihasilkan).
- [x] Tidak ada Firebase, Retrofit, atau library DI berat; dependensi tetap minimal.
- [x] Menjalankan tes JVM dari command line berhasil (satu tes contoh lulus).

## Comments

- 2026-10-10: Kerangka selesai kecuali verifikasi di HP uji (tidak ada perangkat terhubung via adb saat implementasi). Status tetap `ready-for-agent` sampai item "APK terpasang dan terbuka di HP uji" dicentang.
  - Gradle 9.3.1 (wrapper) + AGP 9.0.1 (Kotlin bawaan AGP), `compileSdk`/`targetSdk` 35, `minSdk` 26, paket `com.layzee`.
  - Build rilis: R8 + shrinkResources aktif, ABI splits menghasilkan 3 APK (arm64-v8a, armeabi-v7a, x86_64), masing-masing ~20 KB. Isinya masih identik karena belum ada library native; akan berbeda setelah MediaPipe masuk.
  - Build rilis memakai debug signing key supaya bisa dipasang (sideload MVP). Ganti ke keystore sendiri sebelum distribusi.
  - Manifest APK (diperiksa dengan `apkanalyzer`) tanpa `uses-permission`, jadi tanpa INTERNET.
  - Satu-satunya dependensi: `junit:junit:4.13.2` (test). Activity memakai `android.app.Activity` biasa, tanpa AppCompat.
  - Tes JVM: `./gradlew :app:testDebugUnitTest`.
