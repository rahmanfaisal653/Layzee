# 07: Foreground service dan Start/Stop

**What to build:** Status Off ↔ Idle yang sungguhan. Pengguna menekan Start di Dashboard, kamera menyala lewat foreground service dan tetap aktif saat app lain di depan, sampai pengguna menekan Stop. Tidak ada auto-Off. Pipeline dari kamera sampai dispatcher berjalan di dalam service.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 06 (Evaluasi spike di HP)

**Status:** resolved

- [x] Tombol Start di Dashboard menyalakan foreground service bertipe kamera (dengan notifikasi yang diwajibkan Android); status berpindah dari Off ke Idle.
- [x] Kamera dan pipeline tetap berjalan saat app lain (misalnya Instagram) ada di depan, dan gesture menggulir app itu.
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
- 2026-10-10 (uji pengguna di Instagram): pinch tanpa gerak mem-pause video (tap) dan pinch ditahan menghentikan video (tekan-tahan). Perbaikan: jari virtual baru menyentuh layar setelah tangan bergerak melewati `touchSlop` (0,1 hand size, filter One Euro dulu); pinch yang diam tidak mengirim apa pun, dan lepasnya tidak mengirim LIFT. Status Pinch tetap berlaku sejak pinch masuk. Tes engine diperbarui dan ditambah. Belum diuji ulang di Instagram.
- 2026-10-10 (revisi): menunggu gerak (`touchSlop`) terasa "tidak ada reaksi" dan menambah tahap di luar Pinch, jadi dibatalkan. Sekarang pinch langsung START, dan segmen pertama sudah bergeser `touchJumpFraction` (sekarang 5% tinggi layar, ~120 px, dinaikkan dari 1,25% atas permintaan pengguna agar terasa seperti swipe asli) ke atas, sehingga sentuhan terbaca drag, bukan tap/tekan-tahan. Konsekuensi: konten bergeser sedikit tiap pinch. Belum diuji di Instagram.
- 2026-10-10 (perubahan desain atas permintaan pengguna): **pinch = swipe**. Jari virtual (sentuhan yang menempel, mengikuti gerak tangan, lift dengan kecepatan terakhir) dihapus, karena sentuhan diam terbaca Reels sebagai tap/tekan-tahan (pause). Sekarang satu pinch (setelah debounce) = satu flick ke atas yang cepat (~120 ms, 45% tinggi layar dari titik awal tetap) lewat satu `dispatchGesture`; arah selalu ke atas; menahan pinch tidak mengulang, pinch berikutnya butuh jari sempat terbuka. Dihapus: One Euro filter, swipe ratio, touch slop, goresan berlanjut, deteksi sentuhan layar. Akibatnya item "jari virtual terangkat saat service dihentikan" tidak berlaku lagi (tidak ada goresan menggantung). Belum diuji di Instagram. `CLAUDE.md` dan `CONTEXT.md` ikut diperbarui.
- 2026-10-10: Pengguna menguji di Instagram dan menyatakan hasilnya baik (pinch = swipe mengganti page). Item gulir di app lain dicentang, status `resolved`. Stop dari Dashboard dan service dimatikan sistem tetap hanya diverifikasi lewat kode.
