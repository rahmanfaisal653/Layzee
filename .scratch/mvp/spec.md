# Spec: Layzee MVP

Status: ready-for-agent

Istilah mengikuti `CONTEXT.md` (Pinch, Virtual finger, Swipe, Swipe ratio, Hand size, Off/Idle/Tracking/Pinch, Overlay, Dashboard, Calibration, Pinch sensitivity).

## Problem Statement

Pengguna yang menaruh HP di stand ingin menggulir feed pendek (Reels, TikTok, Instagram) tanpa menyentuh layar: saat tangan sibuk, kotor, atau terlalu jauh. Solusi yang ada tidak presisi (gerak kasar, sering salah baca) atau berat (menguras baterai, membuat HP panas, terutama di HP low-end dan mid-range). Pengguna butuh kontrol tangan yang akurat, mulus, dan ringan.

## Solution

Layzee membaca satu tangan lewat kamera depan. Pinch (jempol dan telunjuk bertemu) diperlakukan sebagai virtual finger: pinch menyentuh layar, gerak tangan vertikal menggeser jari virtual, melepas pinch mengangkatnya. Satu rangkaian itu adalah satu swipe yang bisa sampai mengganti page di app paging. Semua pemrosesan terjadi di perangkat, tanpa jaringan.

Pengguna menyiapkan app sekali di Dashboard (izin, kalibrasi, pengaturan), menekan Start, lalu bekerja lewat Overlay melayang yang menunjukkan status dan menyediakan Stop. Kamera tetap menyala sampai pengguna menghentikan. App dibangun ringan: resolusi analisis rendah, fps adaptif, gerbang gerak saat Idle, CPU saja.

## User Stories

### Gesture dan swipe
1. As a pengguna, I want pinch dikenali sebagai jari yang menyentuh layar, so that saya bisa menggulir tanpa menyentuh HP.
2. As a pengguna, I want menggerakkan tangan ke atas membuat konten bergerak ke atas, so that rasanya sama seperti menggeser dengan jari.
3. As a pengguna, I want melepas pinch mengangkat jari virtual dengan kecepatan gerak terakhir, so that swipe cepat bisa langsung ganti video di Reels/TikTok.
4. As a pengguna, I want swipe pelan dan pendek tidak mengganti page kalau tidak melewati ambang app, so that saya bisa mengintip tanpa pindah video.
5. As a pengguna, I want swipe yang dimulai dari titik tetap di tengah layar, so that swipe selalu mengenai area konten dan bukan tepi atau tombol.
6. As a pengguna, I want gerak horizontal tangan diabaikan, so that swipe tidak melenceng ke samping (misalnya membuka kamera atau Stories).
7. As a pengguna, I want jarak swipe dihitung dari gerak tangan relatif terhadap hand size, so that swipe terasa sama saat tangan dekat atau jauh dari kamera.
8. As a pengguna, I want mengatur swipe ratio di Dashboard, so that saya menentukan seberapa jauh gerak tangan yang dibutuhkan untuk satu swipe.
9. As a pengguna, I want gerak tangan di tepi frame bisa dilanjutkan dengan melepas pinch, menggeser tangan balik, dan pinch lagi, so that saya tidak terbatas oleh lebar frame (ala mengangkat jari di trackpad).
10. As a pengguna, I want gerak jari virtual halus tanpa jitter, so that swipe tidak bergetar dan tidak terasa tertinggal.
11. As a pengguna, I want gerak ke bawah dan ke atas dalam satu pinch sama-sama diikuti, so that saya bisa mengoreksi arah sebelum melepas.

### Keandalan pinch
12. As a pengguna, I want pinch baru dihitung setelah bertahan beberapa frame, so that gerakan jari sekilas tidak memicu swipe tak sengaja.
13. As a pengguna, I want ambang masuk dan keluar pinch berbeda (hysteresis), so that pinch tidak berkedip aktif-mati saat jari hampir menyentuh.
14. As a pengguna, I want pinch tetap terbaca saat ujung jari sebagian tertutup, so that gesture tidak putus karena sudut tangan.
15. As a pengguna, I want jari virtual langsung diangkat saat tangan hilang dari kamera atau skor kepercayaan jatuh, so that tidak ada swipe liar tanpa tangan.
16. As a pengguna, I want swipe berhenti dan tidak ada goresan menggantung kalau sesuatu gagal, so that layar tidak terkunci dalam kondisi "jari tertekan".
17. As a pengguna, I want menyentuh layar sungguhan saat Pinch memutus swipe virtual tanpa mematikan Layzee, so that saya bisa mengambil alih kapan saja.
18. As a pengguna, I want setelah sentuhan memutus swipe, swipe baru hanya mulai setelah pinch dilepas dan dilakukan lagi, so that tidak ada swipe tak terduga langsung setelah sentuhan.
19. As a pengguna, I want pinch palsu sangat jarang saat tangan hanya terlihat (Tracking), so that saya bisa bergerak di depan kamera tanpa menggulir feed.

### Status dan siklus hidup
20. As a pengguna, I want Start hanya dari Dashboard, so that Layzee tidak menyala tanpa sengaja.
21. As a pengguna, I want Layzee tetap berjalan (Idle) saat tidak ada tangan, so that gesture langsung bekerja saat tangan muncul.
22. As a pengguna, I want Layzee tidak mati sendiri, so that perilakunya mudah diprediksi seperti Assistive Touch.
23. As a pengguna, I want Stop dari Overlay atau Dashboard membuat Layzee Off sepenuhnya (kamera mati, Overlay hilang), so that saya yakin kamera tidak lagi dipakai.
24. As a pengguna, I want status Idle, Tracking, dan Pinch terlihat jelas lewat warna Overlay, so that saya tahu apakah tangan saya sedang terbaca.
25. As a pengguna, I want Layzee berfungsi di app apa pun yang ada di depan, so that saya tidak perlu mengatur daftar app.
26. As a pengguna, I want kamera tetap aktif saat app lain di depan, so that gesture bekerja di Instagram/TikTok.

### Overlay
27. As a pengguna, I want Overlay kecil yang bisa digeser ke tepi layar mana pun dan menempel di sana, so that tidak menutupi konten.
28. As a pengguna, I want mengklik Overlay membuka menu kecil dengan "Stop" dan "Buka dashboard", so that Stop tidak terpencet tidak sengaja.
29. As a pengguna, I want Overlay tidak menampilkan pratinjau kamera, so that app tetap ringan dan privat.

### Dashboard: izin dan setup
30. As a pengguna baru, I want panduan izin langkah demi langkah dengan status centang, so that saya tahu apa yang masih kurang.
31. As a pengguna baru, I want penjelasan jujur kenapa Accessibility dibutuhkan dan bahwa gambar diproses lokal, so that saya bisa memberi izin dengan sadar.
32. As a pengguna, I want status Accessibility, overlay, dan kamera dicek otomatis, so that saya tidak perlu menebak apakah izin sudah aktif.
33. As a pengguna, I want app membuka halaman pengaturan baterai/autostart dan membiarkan saya mencentangnya sendiri, so that Layzee tidak dimatikan sistem di Realme/Xiaomi/Oppo.
34. As a pengguna, I want diarahkan kembali ke langkah baterai kalau Layzee terbunuh sistem, so that saya bisa memperbaikinya.
35. As a pengguna, I want Layzee tidak meminta izin internet, so that saya yakin tidak ada data yang keluar.

### Dashboard: kalibrasi dan pengaturan
36. As a pengguna baru, I want kalibrasi wajib sebelum Start pertama, so that pinch sesuai tangan saya sejak awal.
37. As a pengguna, I want kalibrasi singkat (sekitar 5 pinch dan 5 tangan terbuka) dengan pratinjau kamera dan landmark, so that saya melihat sendiri bahwa tangan terbaca.
38. As a pengguna, I want mengulang kalibrasi kapan saja, so that saya bisa memperbaikinya setelah ganti stand atau pencahayaan.
39. As a pengguna, I want slider sensitivitas pinch menggeser sedikit hasil kalibrasi, so that saya bisa menyetel lebih longgar atau lebih ketat tanpa kalibrasi ulang.
40. As a pengguna, I want pengaturan tersimpan di perangkat, so that tidak perlu diatur ulang tiap Start.
41. As a pengguna, I want pratinjau kamera hanya aktif saat Dashboard terbuka, so that app tidak boros di pemakaian sehari-hari.

### Playground dan pengujian
42. As a pengguna, I want area uji berupa daftar yang bisa di-scroll di dalam app, so that saya mencoba gesture tanpa risiko mengacaukan app lain.
43. As a developer, I want pencatatan waktu tiap tahap (kamera, inferensi, logika, dispatch) di build debug, so that saya tahu tahap mana yang lambat.
44. As a developer, I want pencatatan waktu tidak ada di build rilis, so that tidak ada beban untuk pengguna.
45. As a developer, I want gesture engine bisa diuji dengan urutan frame buatan tanpa HP, so that perilaku pinch tervalidasi cepat dan berulang.

### Ringan dan kompatibel
46. As a pengguna HP low-end, I want Layzee tetap mulus di HP lemah, so that fitur ini bukan hanya untuk HP mahal.
47. As a pengguna, I want baterai tidak terkuras cepat saat Idle, so that kamera yang menyala terus tidak jadi beban.
48. As a pengguna, I want HP tidak panas setelah dipakai 15 menit, so that pemakaian tetap nyaman.
49. As a pengguna, I want APK kecil, so that mengunduh dan memasangnya tidak berat.

## Implementation Decisions

Modul dan antarmuka (tingkat konsep, tanpa path atau kode):

- **Gesture engine** (logika murni, satu-satunya seam tes). Menggabungkan PinchDetector, GestureStateMachine, filter posisi, dan pemetaan swipe.
  - Masuk per frame: 21 titik tangan dengan skor kepercayaan, waktu frame, ukuran frame sebenarnya, kejadian "layar disentuh", dan konfigurasi (ambang hasil kalibrasi, sensitivitas pinch, swipe ratio).
  - Keluar: status (Idle / Tracking / Pinch) dan perintah swipe (mulai di titik awal, geser ke posisi baru, angkat dengan kecepatan terakhir, putus).
  - Jalur frame tanpa alokasi objek: hasil ditulis ke wadah keluaran yang dipakai ulang, bukan mengembalikan objek atau `List` baru.
- **Hand landmarker adapter**. MediaPipe Hand Landmarker mode LIVE_STREAM, asinkron, satu tangan, satu thread inferensi khusus. Hasilnya diteruskan ke gesture engine. Model disertakan di dalam APK karena app tidak punya izin internet.
- **Camera pipeline**. CameraX ImageAnalysis dengan `STRATEGY_KEEP_ONLY_LATEST` dan output RGBA_8888 (tanpa konversi YUV manual). Rentang fps kamera dikunci. Resolusi analisis mulai sekitar 320x240 atau 480x360, dinaikkan hanya jika pinch meleset. Hanya kamera depan, tanpa pemilih.
  - **FPS adaptif**: sekitar 5 fps saat tidak ada tangan (Idle), 15-20 fps saat tangan terlihat, tertinggi saat Pinch. Setelah tangan hilang, fps tetap tinggi beberapa detik sebelum turun ke fps Idle (lamanya disetel saat spike).
  - **Gerbang gerak** saat Idle: bandingkan frame abu-abu sangat kecil (sekitar 64x48) dan lewati model jika selisih kecil.
- **SwipeDispatcher** (Accessibility Service dengan izin melakukan gesture). Menerjemahkan perintah swipe dari gesture engine menjadi goresan berlanjut: `dispatchGesture` dengan goresan `willContinue` dan `continueStroke`.
  - Titik awal tiap segmen harus sama persis dengan titik akhir segmen sebelumnya, kalau tidak goresan dibatalkan.
  - Mengangkat jari = segmen terakhir tidak berlanjut. Tidak ada API batal terpisah: gesture baru yang bukan kelanjutan membatalkan yang berjalan.
  - Sentuhan layar sungguhan membatalkan goresan virtual dan sebaliknya; ini dilaporkan ke gesture engine sebagai kejadian "layar disentuh".
  - Kecepatan saat jari diangkat dihitung app target dari titik gerakan, jadi harus diuji per app, bukan diasumsikan.
  - `targetSdk` di atas 29 (di bawah itu sampling gerakan terkunci 100 ms dan patah-patah). `minSdk` 26.
- **Tracking service** (foreground service bertipe kamera). Menjaga kamera aktif saat app lain di depan dan berjalan sampai pengguna menghentikan (Off). Memegang pipeline dari kamera sampai dispatcher.
- **Overlay**. Satu View kecil lewat `WindowManager`: bisa digeser dan menempel ke tepi, warna mengikuti status, klik membuka menu "Stop" dan "Buka dashboard". Terlihat selama Layzee tidak Off. Tidak ada pratinjau kamera dan tidak ada tombol Start.
- **Dashboard**. Satu Activity dengan View klasik (XML), tanpa Compose, berisi:
  - Panduan izin dengan status centang: Accessibility, overlay, dan kamera dicek otomatis; baterai/autostart dicentang manual.
  - Kalibrasi dengan pratinjau kamera dan landmark (pratinjau hanya aktif saat Dashboard terbuka). Wajib sebelum Start pertama.
  - Pengaturan: swipe ratio, pinch sensitivity.
  - Playground: daftar yang bisa di-scroll, tanpa tampilan angka.
  - Tombol Start (satu-satunya jalan ke Start).
- **Penyimpanan pengaturan**. Penyimpanan lokal sederhana untuk hasil kalibrasi, swipe ratio, dan pinch sensitivity. Tidak ada jaringan atau akun.
- **Pencatatan waktu debug**. Tahap kamera, inferensi, logika, dan dispatch dicatat ke log hanya di build debug, dan tidak dikompilasi ke build rilis.

Keputusan perilaku yang berlaku:
- Pemetaan **relatif**: perpindahan jari virtual = perpindahan titik pinch (dalam satuan hand size) × swipe ratio, dari titik awal tetap. Hanya vertikal, arah natural.
- Normalisasi jarak pinch: jarak ujung jempol ke ujung telunjuk dibagi hand size, dengan koordinat dikalikan ukuran frame sebenarnya sebelum jarak dihitung.
- Hysteresis dan debounce 2-3 frame; angka awal hanya titik mulai dan disetel lewat Calibration.
- Fitur cadangan saat terhalang (misalnya jarak ujung jempol ke sendi tengah telunjuk). Saat tangan hilang atau kepercayaan jatuh selama Pinch, jari virtual langsung diangkat dengan aturan lepas yang sama, tanpa masa tenggang.
- Filter posisi: One Euro filter pada posisi pinch.
- Kirim perintah hanya saat ada data baru atau haluskan perpindahan secara linear; tanpa ekstrapolasi jauh.
- Transisi status: Off → Idle (Start dari Dashboard setelah Calibration); Idle → Tracking saat tangan terlihat; Tracking → Pinch saat pinch bertahan melewati debounce; Pinch → Tracking saat pinch dilepas; Pinch → Idle saat tangan hilang. Hanya Stop yang membawa ke Off.
- Setelah sentuhan layar sungguhan memutus Pinch, status kembali ke Tracking dan swipe baru hanya bisa dimulai setelah pinch dilepas lalu dilakukan lagi.
- Berlaku di semua app di depan, tanpa deteksi app target.

Izin dan distribusi:
- Tanpa izin internet. Izin yang dipakai: kamera, tampil di atas app lain (overlay), foreground service kamera (dengan notifikasi yang diwajibkan Android), dan Accessibility Service.
- Distribusi MVP lewat APK langsung (sideload). Alur pengungkapan dan persetujuan Accessibility dibangun di dalam app sesuai syarat Google Play supaya jalur Play terbuka nanti. Jangan menandai app sebagai alat aksesibilitas (`isAccessibilityTool`).
- R8 shrink dan ABI splits aktif.

## Testing Decisions

- **Apa yang membuat tes baik**: hanya menguji perilaku yang terlihat dari luar (status dan perintah swipe yang keluar untuk urutan frame tertentu), bukan detail internal seperti nama fungsi atau struktur data.
- **Satu seam tes: gesture engine.** Tes JVM murni dengan urutan frame buatan, tanpa HP, kamera, atau Android. Perilaku yang wajib tercakup:
  - debounce (pinch sekilas tidak memicu swipe),
  - hysteresis (tidak berkedip di sekitar ambang),
  - tangan hilang saat Pinch mengangkat jari virtual,
  - kepercayaan rendah saat Pinch mengangkat jari virtual,
  - layar disentuh saat Pinch memutus swipe, status ke Tracking, dan swipe baru butuh pinch dilepas dulu,
  - gerak yang sama di jarak tangan berbeda menghasilkan swipe yang sama (normalisasi hand size),
  - swipe ratio mengubah jarak swipe secara proporsional,
  - hanya gerak vertikal yang dihitung,
  - transisi status sesuai tabel (Idle, Tracking, Pinch).
- **Tidak diuji lewat seam**, dan diverifikasi manual di HP uji (Realme) dengan log debug: kamera dan fps adaptif, gerbang gerak, inferensi MediaPipe, `dispatchGesture` di Instagram/TikTok, Overlay, Dashboard, dan alur izin. Perhitungan ambang dari sampel kalibrasi juga diverifikasi lewat pemakaian nyata, bukan tes unit.
- **Prior art**: tidak ada. Repo belum punya kode atau tes, jadi gaya tes pertama ditetapkan oleh implementasi gesture engine.

## Out of Scope

- Model AI buatan sendiri, banyak gesture, dan deteksi gerakan 3D penuh.
- Akun, cloud, integrasi API Instagram/TikTok, dan dukungan iOS.
- Statistik pemakaian, tema, tutorial panjang.
- Fitur Assistive Touch lain (home, back, screenshot).
- Tombol Start di Overlay, auto-Off, pemilih kamera, daftar putih app.
- Tampilan angka latensi di app.
- Profil performa otomatis, pemantauan suhu, GPU delegate, dan ROI crop; dibangun hanya jika tes di perangkat menunjukkan perlu.
- Handheld mode (HP dipegang); target pertama adalah mode penyangga.
- Distribusi lewat Google Play (jalurnya tetap dibuka lewat alur persetujuan, tapi pengajuan deklarasi bukan bagian MVP).

## Further Notes

- **Batas awal sementara untuk spike** (boleh direvisi setelah ada data): pinch terdeteksi benar ≥ 95% di cahaya ruangan normal pada jarak tangan 30-60 cm; pinch sampai layar bergerak ≤ 150 ms; pinch dilepas sampai gerak berhenti ≤ 100 ms; pinch palsu paling banyak 1 kali per 5 menit saat Tracking; HP tidak melambat atau panas mengganggu setelah 15 menit.
- **Risiko**: hanya ada satu HP uji (Realme), tanpa pembanding low-end vs mid-range. Catat model, RAM, dan versi Android-nya. Realme termasuk merek dengan manajemen baterai agresif.
- **Risiko**: kamera yang menyala terus tanpa auto-Off bisa melewati anggaran baterai. Kalau terukur begitu, pertimbangkan lagi auto-Off.
- **Risiko**: kebijakan Play soal Accessibility ambigu untuk kasus ini; belum ada kepastian reviewer menerima "kontrol scroll tanpa sentuh" sebagai fitur inti non-aksesibilitas.
- **Belum dikonfirmasi**: durasi fps tinggi setelah tangan hilang, pengaruh pelambatan segmen terakhir terhadap fling di tiap app, dan latensi antar segmen `continueStroke` (tidak ada angka terdokumentasi). Semuanya diukur saat spike di perangkat.
- **Spike** (pinch andal di berbagai sudut dan cahaya, goresan terasa mulus di Instagram/TikTok, ukur latensi lewat log debug) dikerjakan sebagai tiket pertama, bukan prototype terpisah, karena jawabannya hanya ada di perangkat sungguhan.
