# 04: Kamera + landmark: status tampil di layar

**What to build:** Jalur nyata pertama di HP: kamera depan membaca frame, MediaPipe Hand Landmarker menemukan satu tangan, gesture engine menentukan status, dan hasilnya terlihat langsung di layar uji (pratinjau kamera, titik landmark, status Idle/Tracking/Pinch). Pencatatan waktu per tahap ada di build debug. Belum ada swipe.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 01 (Kerangka proyek Android), 02 (Gesture engine: pinch dan status)

**Status:** resolved

- [x] Izin kamera diminta dan layar uji menampilkan pratinjau kamera depan dengan titik landmark dan status engine secara langsung.
- [x] CameraX ImageAnalysis dengan `STRATEGY_KEEP_ONLY_LATEST`, output RGBA_8888 (tanpa konversi YUV manual), resolusi analisis rendah (mulai sekitar 320x240 atau 480x360), dan hanya kamera depan.
- [x] Hand Landmarker mode LIVE_STREAM, asinkron, satu tangan, di satu thread inferensi khusus sehingga UI tidak tersendat.
- [x] Model disertakan di dalam APK; app tetap tanpa izin internet.
- [x] Buffer dan objek di jalur frame dipakai ulang (tidak ada `List`/`Pair` baru per frame).
- [x] Di build debug, waktu tiap tahap (kamera, inferensi, logika) tercatat ke log; di build rilis kode pencatatan tidak ada.
- [x] Diverifikasi di HP uji: status berubah ke Tracking saat tangan muncul, Pinch saat pinch bertahan, dan kembali saat pinch dilepas atau tangan hilang.

## Comments

- 2026-10-10: Selesai, dengan satu masalah terbuka (di bawah). Kode di `app/src/main/java/com/layzee/pipeline/` (`CameraPipeline`, `HandLandmarkerSource`, `StageTimer`), `LandmarkView`, dan `DashboardActivity` sebagai layar uji.
  - Terverifikasi di Realme RMX3241 (Android 13): status berubah Idle → Tracking (tangan muncul) → Pinch (pinch bertahan) → Tracking (pinch dilepas) → Idle (tangan hilang), diambil dari screenshot saat tangan digerakkan. Log debug: ~10-15 fps, kamera ~73 ms (cap waktu sensor ke analyzer), konversi ~0,3 ms, inferensi ~75-120 ms (CPU, 320x240), logika ~0,1 ms. Inferensi 75-120 ms perlu dipantau untuk anggaran latensi (tiket 06/13).
  - **Masalah terbuka: titik landmark di pratinjau salah tempat.** Kerangka titik bergeser (umumnya ke atas) dari tangan sebenarnya, tidak hanya karena lag; titik ujung jempol/telunjuk (merah) sering mendekati posisi pinch asli tapi sisa kerangka tidak pas. Status Idle/Tracking/Pinch tetap benar karena dihitung dari jarak relatif. Belum diselidiki; ditunda atas permintaan pengguna. Kandidat penyebab: arah rotasi (`ImageProcessingOptions.rotationDegrees` dianggap berlawanan arah jarum jam: `(360 - rotationDegrees) % 360`; belum dipastikan), pratinjau dan analisis tanpa `ViewPort` bersama (FOV/crop bisa beda), asumsi FIT_CENTER di `LandmarkView`, dan cermin `1 - x`. Cara paling cepat menyelidiki: simpan bitmap yang masuk ke MediaPipe + koordinat landmark dan bandingkan. Item 1 dicentang karena pratinjau, titik, dan status tampil; posisi titik belum akurat.
  - Model `hand_landmarker.task` (float16, 7,5 MB, dari storage.googleapis.com/mediapipe-models) ada di `app/src/main/assets/`. APK rilis: arm64-v8a 20 MB, armeabi-v7a 16 MB, x86_64 23 MB.
  - MediaPipe/CameraX menyuntik `INTERNET` dan `ACCESS_NETWORK_STATE` lewat manifest merge; dihapus dengan `tools:node="remove"`. Diperiksa pada APK rilis dengan `aapt2 dump permissions`: hanya `CAMERA`. `StageTimer`/`LayzeeTiming` tidak ada di dex rilis.
  - Perubahan di luar tiket: `compileSdk` 35 → 36 (diwajibkan `camera-view` 1.6.2; `targetSdk` tetap 35), layar dikunci portrait, dependensi baru `androidx.activity` (untuk `ComponentActivity` sebagai LifecycleOwner), CameraX (`core`, `camera2`, `lifecycle`, `view`) dan `tasks-vision` 1.1.0.
  - Pembuatan dan penutupan landmarker dilakukan di thread analisis (bukan thread UI) dan hasil yang datang setelah `close()` diabaikan, hasil review.
  - Belum ada (sengaja, tiket 08): kunci rentang fps `CONTROL_AE_TARGET_FPS_RANGE`, fps adaptif, gerbang gerak. Resolusi pratinjau memakai selector yang sama dengan analisis (320x240), jadi pratinjau beresolusi rendah.
  - Catatan debug: `startNs` timing inferensi dikunci per timestamp (8 slot) dan tidak disinkronkan antar thread; angka inferensi cukup untuk gambaran, bukan pengukuran presisi.
- 2026-10-10 (perbaikan landmark, menunggu verifikasi pengguna): `PreviewView`/use case `Preview` dibuang. `LandmarkView` kini menggambar frame analisis yang menghasilkan landmark itu (disalin per frame ke 4 slot di `HandLandmarkerSource`, dicocokkan lewat timestamp) dan titiknya di atasnya, diputar tegak dan dicermin. Dengan begitu tidak ada lag antara gambar dan titik, tidak ada asumsi crop/FIT_CENTER pratinjau, satu stream kamera berkurang, dan dependensi `camera-view` dihapus. Bersama perbaikan arah rotasi di tiket 05. Terverifikasi: frame tampil tegak dan tercermin; belum diverifikasi: titik menempel di jari. Kalau titik masih meleset di atas frame yang sama, penyebabnya bukan geometri pratinjau (curigai model/resolusi 320x240).
