# 09: Overlay

**What to build:** Ikon kecil melayang di atas app lain yang menunjukkan status Layzee dan membuka menu "Stop" dan "Buka dashboard", mirip Assistive Touch. Muncul saat Start dan hilang saat Off. Tidak ada tombol Start di sini.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 07 (Foreground service dan Start/Stop)

**Status:** ready-for-human

- [x] Overlay muncul saat Layzee Start dan hilang saat Off, sebagai satu View kecil lewat `WindowManager`.
- [x] Warna overlay mengikuti status Idle / Tracking / Pinch.
- [x] Overlay bisa digeser dan menempel ke tepi layar.
- [x] Klik overlay membuka menu kecil dengan dua tombol: "Stop" dan "Buka dashboard".
- [x] "Stop" membuat Layzee Off sepenuhnya (kamera mati, overlay hilang); "Buka dashboard" membuka Dashboard.
- [x] Overlay tidak menampilkan pratinjau kamera dan tidak punya tombol Start.
- [x] Overlay yang menempel di tepi tidak menghalangi titik awal swipe di tengah layar.
- [ ] Diverifikasi di HP uji saat Instagram/TikTok di depan.

## Comments

- 2026-10-11: Dikerjakan agen. `Overlay` (`app/src/main/java/com/layzee/overlay/Overlay.kt`): satu View bulat 48dp lewat `WindowManager` (`TYPE_APPLICATION_OVERLAY`, tidak fokus), dibuat dan dihapus oleh `LayzeeService` (muncul saat Start, hilang saat Off), warna mengikuti status lewat `onFrame` (hanya menyentuh View saat status berubah). Geser menempel ke tepi kiri/kanan; klik membuka menu "Stop" / "Buka dashboard" (jendela kecil di sisi ikon yang menghadap tengah, jadi titik awal swipe di tengah bebas). Aturan penempatan murni di `OverlayPlacement` dengan unit test. Izin `SYSTEM_ALERT_WINDOW` ditambahkan; tombol "Overlay" sementara di Dashboard membuka halaman izinnya (diganti panduan izin di tiket 10). Tanpa izin itu Layzee tetap jalan tanpa overlay.
  - Item 1-7 dicentang berdasarkan kode: `assembleDebug` dan semua unit test lolos. Tidak ada HP terhubung saat pengerjaan, jadi perilaku di layar (geser, tempel, menu, Stop, Buka dashboard dari background, warna status) belum pernah dijalankan.
  - Item 8 (uji di HP saat Instagram/TikTok di depan) belum: itu sebabnya status `ready-for-human`. Yang perlu dicek: klik ikon saat menu terbuka menutup menu; "Buka dashboard" benar-benar membuka Dashboard dari app lain; flick tidak terhalang menu.
  - Review: diperbaiki ikon yang membuka ulang menu saat diklik ketika menu terbuka, animasi snap yang tidak dibatalkan, `BadTokenException` saat izin dicabut, dan contentDescription. Belum ditangani (di luar scope tiket ini): ukuran layar overlay tidak diperbarui saat rotasi/lipat, overlay tidak muncul kalau izin baru diberikan setelah Start (perlu Stop lalu Start), dan posisi belum memperhitungkan inset status bar/gesture.
