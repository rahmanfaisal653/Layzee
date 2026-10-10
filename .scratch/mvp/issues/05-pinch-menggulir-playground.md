# 05: Pinch menggulir playground

**What to build:** Jalur tipis dari ujung ke ujung: pinch + gerak vertikal menggulir daftar playground di dalam app lewat Accessibility Service, dengan goresan berlanjut. Dimulai dari tombol debug sementara (belum ada service foreground, overlay, atau kalibrasi). Ini inti spike: jawaban apakah pinch dan goresan Accessibility terasa mulus di HP sungguhan.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 03 (Gesture engine: swipe), 04 (Kamera + landmark: status di layar)

**Status:** resolved

- [x] Accessibility Service dengan izin melakukan gesture bisa diaktifkan manual di pengaturan sistem.
- [x] Playground: daftar yang bisa di-scroll di dalam app, tanpa tampilan angka.
- [x] Tombol debug sementara memulai dan menghentikan pipeline (akan digantikan tiket 07).
- [x] Pinch + gerak vertikal menggulir daftar playground; melepas pinch mengangkat jari virtual dengan kecepatan gerak terakhir.
- [x] SwipeDispatcher memakai goresan berlanjut: titik awal tiap segmen sama persis dengan titik akhir segmen sebelumnya, dan mengangkat jari = segmen terakhir tidak berlanjut.
- [x] Sentuhan layar sungguhan diteruskan ke gesture engine sebagai kejadian "layar disentuh" dan memutus swipe.
- [x] Pipeline dihentikan di tengah Pinch → jari virtual terangkat; tidak ada goresan menggantung ("jari tertekan").
- [x] `targetSdk` di atas 29 sehingga sampling gerakan halus, bukan 100 ms patah-patah.
- [x] Di build debug, waktu dispatch tercatat ke log berdampingan dengan tahap sebelumnya.
- [x] Diverifikasi di HP uji: daftar playground bergerak mengikuti pinch dan berhenti saat pinch dilepas.

## Comments

- 2026-10-10: Kode selesai, tinggal verifikasi di HP (item terakhir sengaja belum dicentang; butuh pinch sungguhan, jadi status `ready-for-human`). Build debug dan rilis lolos, 51 tes JVM lulus, APK debug sudah terpasang di Realme RMX3241. Layout playground terlihat benar di layar (titik awal swipe, tengah x dan 65% tinggi, jatuh di atas daftar). Gesture belum diuji.
  - Kode: `app/src/main/java/com/layzee/swipe/` (`SwipeDispatcher`, `LayzeeAccessibilityService`), config di `res/xml/accessibility_service_config.xml`, `DashboardActivity` memuat playground + tombol debug Start/Stop + tombol ke pengaturan Aksesibilitas (banner kuning muncul selama service belum aktif). `HandLandmarkerSource` mengirim perintah ke dispatcher; `StageTimer` bertambah tahap `dispatch`.
  - Cara uji: aktifkan "Layzee" di Pengaturan > Aksesibilitas, buka app, tekan "Mulai (debug)", pinch lalu gerakkan tangan naik/turun, lepas pinch. Lihat `adb logcat -s LayzeeTiming` untuk waktu per tahap.
  - Desain dispatcher: satu segmen `continueStroke` dalam penerbangan sekaligus; `move` hanya mencatat target terbaru dan segmen berikutnya (dari titik akhir sebelumnya, lamanya = waktu sejak segmen sebelumnya, 8-100 ms) keluar saat segmen sebelumnya selesai. Saat lift, jari menyusul dulu ke titik lift lewat gerak biasa, lalu segmen terakhir `willContinue=false` sepanjang 16 ms dengan kecepatan terakhir (ekstrapolasi hanya 16 ms). Sentuhan sungguhan terdeteksi dari `onCancelled` segmen yang masih kita lanjutkan, dibaca engine sekali per frame lewat `consumeTouched()`.
  - Hasil review (dua sub-agen, standar + spec) yang sudah diperbaiki: start tertunda yang bisa menekan jari setelah pinch sudah lepas; lift yang membuang target terakhir dan membuat kecepatan lift palsu; `release()` yang menimpa lift yang menunggu; config Accessibility yang mendaftar event notifikasi padahal tidak dipakai; label "Berhenti" (istilah `_Avoid_`).
  - Diketahui, belum ditangani (cek saat uji di HP):
    - Sentuhan sungguhan baru terdeteksi saat segmen berikutnya dibatalkan, jadi tertunda sampai satu frame (~80 ms); dan `onCancelled` karena sebab lain (interupsi) terbaca sebagai sentuhan.
    - Balapan sempit saat Stop: hasil MediaPipe yang sudah lewat pemeriksaan `closed` bisa mengirim START setelah `release()` dan meninggalkan goresan menggantung. Ada pemeriksaan `closed` kedua tepat sebelum dispatch, tapi tidak menutup sepenuhnya.
    - `dispatchGesture` dipanggil sambil memegang kunci; callback di thread utama bisa menunggu sebentar.
    - Waktu `dispatch` di log hanya durasi pemanggilan `dispatchGesture` pada frame yang membawa perintah, bukan waktu sampai layar bergerak.
    - Di fps rendah, durasi segmen dibatasi 100 ms sehingga jari tertinggal dari tangan.
    - Di luar tiket: banner/tombol Aksesibilitas, `LandmarkView.clear()`, dan `onStop` yang menghentikan pipeline (menjamin jari terangkat saat app keluar dari layar).
    - Tidak ada tes JVM baru: logika dispatcher terikat tipe Android (`Path`, `GestureDescription`).
- 2026-10-10 (verifikasi): Pengguna menguji di HP uji: fungsi pinch menggulir playground aman (daftar bergerak mengikuti pinch dan berhenti saat dilepas). Tiket ditutup.
  - Setelah uji: percobaan membalik arah `ImageProcessingOptions.rotationDegrees` tidak menyelesaikan titik landmark yang meleset (dugaan dari bytecode ternyata tidak cukup). Penyelesaiannya ada di komentar tiket 04: bitmap diputar tegak sendiri sebelum inferensi, tanpa opsi rotasi MediaPipe. Karena landmark sebelumnya berada di ruang yang salah (wrist di atas ujung telunjuk), **arah swipe perlu dicek ulang di HP**: tangan naik harus membuat konten naik. Isi playground diganti dari kata yang mirip nama status menjadi pita warna tanpa teks.
  - Titik landmark yang meleset (tiket 04) sudah diperbaiki; lihat tiket 04.
