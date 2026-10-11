# Layzee

Ganti page Reels, TikTok, atau Instagram tanpa menyentuh layar. Cukup **pinch** (jempol bertemu telunjuk) di depan kamera depan, dan Layzee mengirim satu swipe ke atas untuk pindah ke page berikutnya.

Layzee adalah app Android "air trackpad" yang ringan dan presisi, dirancang untuk HP yang disandarkan di stand sambil menonton feed.

## Cara pakai

1. Buka **Dashboard**, ikuti panduan izin, lalu tekan **Start**.
2. Letakkan HP di stand dan buka app feed favoritmu.
3. Pinch di depan kamera depan: satu pinch = satu swipe ke atas.

Ikon **overlay** melayang di atas app lain dan warnanya menunjukkan status. Ketuk untuk membuka menu kecil: **Stop** atau **Buka dashboard**.

## Fitur

- **Pinch = swipe.** Begitu pinch terdeteksi, Layzee mengirim satu flick cepat ke atas dari tengah layar. Menahan pinch tidak mengulang swipe, dan gerak tangan tidak dibaca.
- **Berlaku di semua app** yang sedang di depan, tanpa daftar putih.
- **Deteksi andal.** Jarak jempol-telunjuk dinormalisasi dengan ukuran tangan, jadi tidak bergantung pada jarak tangan ke kamera. Pinch palsu sengaja dibuat lebih sulit terjadi daripada pinch yang terlewat.
- **Hemat beban.** Resolusi analisis rendah, fps adaptif (rendah saat tidak ada tangan, naik saat tangan terlihat), dan hanya satu tangan yang dibaca. Dirancang untuk HP low-end dan mid-range.
- **Layar uji pinch** di Dashboard: pratinjau kamera dengan landmark tangan, label status hidup, dan daftar yang bisa discroll dengan pinch.
- **Tema** gelap dan terang mengikuti sistem.

## Status

| Status | Artinya |
| --- | --- |
| **Off** | Layzee mati: kamera tidak dipakai dan overlay tidak terlihat. |
| **Idle** | Layzee berjalan, tetapi tidak ada tangan terlihat. |
| **Tracking** | Tangan terlihat, belum pinch. |
| **Pinch** | Pinch terdeteksi. |

Layzee tidak pernah mematikan dirinya sendiri: hanya kamu yang bisa menghentikannya (Stop dari overlay atau Dashboard). Saat layar mati, kamera dilepas otomatis dan dipasang lagi begitu layar menyala.

## Izin yang dibutuhkan

| Izin | Untuk apa |
| --- | --- |
| **Accessibility Service** | Mengirim gerakan swipe ke app lain. Hanya dipakai untuk swipe ini, tidak membaca isi layar. |
| **Kamera** | Membaca pinch lewat kamera depan. |
| **Tampil di atas app lain** | Menampilkan ikon overlay. |
| **Notifikasi** | Notifikasi foreground service yang menjaga kamera tetap aktif. |
| **Pengecualian optimasi baterai / autostart** | Mencegah sistem mematikan Layzee di latar belakang (penting di HP dengan manajemen baterai agresif). Dicentang manual dari Dashboard. |

## Privasi

- Semua gambar diproses **di perangkat**. Tidak ada gambar yang disimpan atau dikirim.
- Tidak ada akun, cloud, analitik, maupun izin internet.

## Persyaratan

- Android 8.0 (API 26) atau lebih baru.
- HP dengan kamera depan. Mode penyangga (HP di stand) adalah target pertama; mode genggam menyusul.

## Teknologi

Kotlin native dengan CameraX dan MediaPipe Hand Landmarker (CPU), tampilan View klasik tanpa Compose, dan dependensi seminimal mungkin.

## Status proyek

Versi `0.1.0`, masih tahap MVP dan didistribusikan lewat APK langsung (sideload).
