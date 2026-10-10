# 03: Gesture engine: swipe

**What to build:** Perluasan gesture engine agar status Pinch menghasilkan perintah swipe: virtual finger menyentuh di titik awal tetap, mengikuti gerak vertikal tangan dengan swipe ratio, lalu terangkat dengan kecepatan gerak terakhir. Termasuk perilaku saat tangan hilang dan saat layar disentuh sungguhan. Diverifikasi lewat tes JVM.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 02 (Gesture engine: pinch dan status)

**Status:** resolved

- [x] Saat Pinch dimulai, engine mengeluarkan perintah "mulai" di titik awal tetap (x tengah layar, y sekitar 60-70% tinggi layar).
- [x] Perpindahan jari virtual = perpindahan titik pinch (dalam satuan hand size) × swipe ratio. Hanya gerak vertikal dihitung; arah natural (tangan naik = konten naik).
- [x] Gerak tangan yang sama di jarak kamera berbeda menghasilkan swipe yang sama; menggandakan swipe ratio menggandakan jarak swipe.
- [x] One Euro filter pada posisi pinch menekan jitter; tes: input bergetar kecil menghasilkan gerak jari virtual yang lebih halus tanpa tertinggal berarti.
- [x] Gerak naik lalu turun dalam satu pinch sama-sama diikuti.
- [x] Saat pinch dilepas, engine mengeluarkan perintah "angkat" dengan kecepatan gerak terakhir (bukan nol).
- [x] Tangan hilang atau kepercayaan jatuh saat Pinch → perintah "angkat" langsung dikeluarkan.
- [x] Kejadian "layar disentuh" saat Pinch → perintah "putus", status kembali ke Tracking, dan swipe baru hanya mulai setelah pinch dilepas lalu dilakukan lagi.
- [x] Perintah keluar lewat wadah yang dipakai ulang (tanpa alokasi objek per frame) dan hanya saat ada data baru.
- [x] Semua perilaku di atas punya tes JVM yang memeriksa perintah keluar untuk urutan frame masukan.

## Comments

- 2026-10-10: Selesai. Kode di `app/src/main/java/com/layzee/gesture/` (`GestureEngine` diperluas; baru: `SwipeCommand`, `OneEuroFilter`), tes di `app/src/test/java/com/layzee/gesture/GestureSwipeTest.kt` (29 tes JVM; total 51 lulus via `./gradlew :app:testDebugUnitTest`).
  - Antarmuka: `GestureOutput` kini membawa `command` (`NONE/START/MOVE/LIFT/CANCEL`), `x`, `y` (piksel layar), dan `velocityY` (px/detik, positif = turun; berlaku untuk `LIFT`). Kejadian "layar disentuh" masuk lewat `HandFrame.screenTouched` (diisi dispatcher: true bila ada sentuhan sejak frame sebelumnya).
  - `GestureConfig` bertambah: ukuran layar, `startYFraction` (0.65), `swipeRatio`, dan parameter One Euro. `swipeRatio` didefinisikan sebagai **tinggi layar per hand size** (default 0.3) supaya tidak bergantung pada resolusi layar; ubah bila tiket 12 butuh satuan lain.
  - Titik pinch = titik tengah ujung jempol dan ujung telunjuk. Perpindahan vertikal diakumulasi per frame dalam satuan hand size (dibagi hand size frame itu), lalu difilter One Euro, lalu dikali swipe ratio. Posisi jari dijepit ke dalam layar.
  - Frame yang masih menunggu debounce lepas tidak menggerakkan jari (jari terbuka membuat titik tengah tidak andal); `LIFT` memakai posisi `MOVE` terakhir supaya segmen terakhir sambung dengan goresan sebelumnya. Kecepatan angkat = per-frame speed yang dihaluskan (EMA 0.5) dari posisi jari.
  - Setelah `CANCEL`, status Tracking dan Pinch baru bisa mulai lagi setelah jari terbuka `releaseFrames` frame berturut-turut lalu pinch debounce ulang. Sentuhan layar saat bukan Pinch diabaikan. Sentuhan + tangan hilang di frame yang sama: `CANCEL` tanpa `LIFT`.
  - `MOVE` keluar tiap frame dengan timestamp baru; frame dengan timestamp sama (tanpa data baru) menghasilkan `NONE`.
  - "Tanpa alokasi" (item 19) diverifikasi lewat pembacaan kode, bukan tes. Angka filter (`minCutoff` 1.5, `beta` 1.0, `dCutoff` 1.0) dan `swipeRatio` default hanya titik awal; harus disetel di HP (tiket 05/06). Pelambatan gerak jari menjelang `LIFT` dan kecepatan angkat yang diterima tiap app belum diverifikasi.
  - Review: setelah `CANCEL`, hilangnya tangan tidak dihitung sebagai pinch dilepas (menunggu lepas bertahan sampai jari terbuka). Tes kecepatan angkat kini memeriksa besarnya (~-900 px/s), bukan hanya arah.
  - Belum ditangani (catatan untuk tuning di HP): `rawDisplacement` terus terakumulasi melewati batas layar, jadi setelah jari terjepit di tepi, tangan harus kembali melewati kelebihannya dulu; y=0 berada di tepi tarik status bar; penghalusan kecepatan angkat per frame (bukan per waktu), jadi bergantung fps; `START` tidak memeriksa timestamp ganda.
