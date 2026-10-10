# 07: Foreground service dan Start/Stop

**What to build:** Status Off ↔ Idle yang sungguhan. Pengguna menekan Start di Dashboard, kamera menyala lewat foreground service dan tetap aktif saat app lain di depan, sampai pengguna menekan Stop. Tidak ada auto-Off. Pipeline dari kamera sampai dispatcher berjalan di dalam service.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 06 (Evaluasi spike di HP)

**Status:** ready-for-human

- [x] Tombol Start di Dashboard menyalakan foreground service bertipe kamera (dengan notifikasi yang diwajibkan Android); status berpindah dari Off ke Idle.
- [ ] Kamera dan pipeline tetap berjalan saat app lain (misalnya Instagram) ada di depan, dan gesture menggulir app itu.
- [x] Stop dari Dashboard mematikan service dan kamera sepenuhnya (Off).
- [x] Tidak ada auto-Off: Idle yang lama tetap menyala sampai pengguna menghentikan.
- [x] Tombol debug dari tiket 05 digantikan oleh Start/Stop.
- [x] Jika service dihentikan sistem di tengah Pinch, jari virtual terangkat dan tidak ada goresan menggantung.
- [x] Hanya satu tempat yang memulai pipeline (service); Dashboard tidak menjalankan kamera sendiri untuk pemakaian sehari-hari.

## Comments

- 2026-10-10: Dikerjakan agen. `LayzeeService` (`app/src/main/java/com/layzee/LayzeeService.kt`) sekarang satu-satunya pemilik pipeline: foreground service bertipe kamera, kamera diikat ke lifecycle service sendiri, `START_NOT_STICKY`, tanpa auto-Off. Dashboard hanya Start/Stop (dan meminta izin kamera + notifikasi sebelum Start) dan menampilkan frame lewat `LayzeeService.preview` selama terbuka; tombol debug dan `onStop()` yang mematikan pipeline dihapus. `onDestroy` melepas jari virtual (`dispatcher.release()`) sebelum pipeline ditutup.
  - Diverifikasi di HP (Realme RMX3241): service `isForeground=true`, dan log `LayzeeTiming` tetap jalan (~15 fps) saat Firefox di depan. Unit test dan `assembleDebug` lolos.
  - Belum dicentang: item 2 ("gesture menggulir app itu"). Pipeline terbukti jalan di latar, tetapi uji pinch menggulir Instagram dilakukan manusia (sekaligus membuka sisa tiket 06). Itu sebabnya status `ready-for-human`.
  - Tidak diuji di perangkat: Stop dari Dashboard (jalur `stopService`) dan service dimatikan sistem di tengah Pinch; keduanya baru diverifikasi lewat pembacaan kode.
  - Catatan: tanpa izin notifikasi (Android 13+), notifikasi service hanya terlihat di Task Manager foreground service; service tetap jalan.
