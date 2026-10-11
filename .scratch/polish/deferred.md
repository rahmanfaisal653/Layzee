# Polish: rencana yang ditunda

Dicatat dari sesi grilling 2026-10-11. Dua hal ini disepakati **ditunda**, bukan dibatalkan. Belum jadi tiket; dijadikan tiket (`issues/NN-*.md`) saat pengguna siap mengerjakannya.

## Pengukuran baseline baterai dan beban (ditunda oleh pengguna)

Latar belakang: `CLAUDE.md` melarang optimasi tanpa pengukuran, tetapi angka CPU, mAh per jam, suhu, dan latensi tidak pernah tercatat di repo (issue 13 ditutup atas konfirmasi pengguna). HP uji: Realme RMX3241 (Dimensity 700, 7,8 GB RAM, Android 13), lebih dekat ke mid-range. Tidak ada HP low-end; itu risiko terbuka.

Rencana:
1. Skrip `adb` kecil: `dumpsys batterystats` (reset lalu baca), `dumpsys cpuinfo` untuk proses Layzee, suhu baterai/CPU, masing-masing untuk Idle, Tracking, dan layar mati. Masing-masing 15 menit.
2. Pengguna menjalankan skrip di HP dan mengirim hasilnya.
3. Angka masuk ke bagian "Anggaran Ringan" di `CLAUDE.md`.
4. Setiap optimasi berikutnya dikerjakan satu per satu dan diukur ulang.

Belum diukur sama sekali: CPU %, mAh/jam, suhu, latensi pinch-sampai-layar-bergerak, daya kamera saat Idle, perilaku layar mati, HP low-end.

Kandidat optimasi yang menunggu angka ini:
- Rotasi bitmap lewat `Canvas.drawBitmap` pada setiap frame yang diterima, bahkan saat rotasi 0 (`HandLandmarkerSource.kt`). Bisa diganti `ImageProcessingOptions` atau dilewati.
- `detectAsync` tanpa batas frame yang sedang diproses.
- Rilis masih ditandatangani kunci debug dan `proguard-rules.pro` kosong (aturan R8 untuk MediaPipe belum teruji).
- Fps Tracking 20 dan Pinch 30 mungkin lebih tinggi dari kebutuhan. Pengguna memutuskan rentang kamera 15-30 fps tidak diubah; yang bisa dipertimbangkan hanya laju model.

## Jarak pinch: world landmarks dibanding 2D (ditunda oleh pengguna)

Sekarang: jarak 2D dalam piksel dibagi jarak `wrist→MCP` (landmark 0 ke 9); z tidak dibaca (`PinchMeasure.kt`, `HandFrame.kt`).

Rencana saat dikerjakan: hitung kedua ukuran di build debug, catat ke logcat, bandingkan di HP uji pada jarak dekat dan jauh, lalu pilih satu untuk rilis. `worldLandmarks` (meter) dikenal lebih konsisten terhadap jarak, tetapi z hasil perkiraan model dan mungkin lebih berisik untuk tangan kecil. Tidak ada angka akurasi independen.
