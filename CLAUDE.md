# CLAUDE.md

## Ringkasan Proyek

App Android "air trackpad": kamera depan membaca gesture **pinch** satu tangan untuk mengganti page feed di app lain (Reels/TikTok/Instagram). **Pinch = swipe**: satu pinch langsung mengirim satu flick ke atas yang mengganti page; gerak tangan tidak dibaca. Tampilan pemakaian ala Assistive Touch (overlay melayang).

Posisi produk: bukan sekadar "scroll pakai tangan", tapi kontrol yang **presisi dan ringan**.

Istilah domain (Pinch, Swipe, Hand size, Off/Idle/Tracking/Pinch, Overlay, Dashboard, Calibration, Pinch sensitivity) didefinisikan di `CONTEXT.md`. Pakai istilah itu, bukan sinonim yang tercantum di `_Avoid_`.

## Prinsip Utama

1. **Ringan di semua sisi**: ukuran APK kecil, beban CPU/baterai rendah, tech stack minimal.
2. **Capture pinch tetap akurat** meski ringan.
3. **Simple dulu.** Jangan menambah fitur di luar scope MVP.
4. **Privasi**: semua pemrosesan gambar lokal. Tidak ada akun, cloud, atau jaringan. Idealnya app tidak meminta izin internet.
5. **Target perangkat: low-end dan mid-range.** Optimasi dimulai dari HP terlemah.

## Keputusan yang Sudah Ditetapkan

- Gesture tunggal: **pinch**, dan **pinch = swipe**: begitu pinch terdeteksi (setelah debounce) dikirim satu flick cepat ke atas dari titik awal tetap, cukup untuk mengganti page di app paging. Satu pinch = satu swipe; menahan pinch tidak mengulang. Tidak ada jari virtual yang menempel dan mengikuti tangan (itu bikin Reels membaca tap/tekan-tahan sebagai pause).
- Arah swipe selalu ke atas (next). Titik awal tetap di tengah layar (x tengah, y sekitar 60-70% tinggi layar).
- Berlaku di semua app yang ada di depan, tanpa daftar putih.
- Empat status: **Off / Idle / Tracking / Pinch**. Tidak ada auto-Off: Layzee tidak pernah mematikan dirinya sendiri. Kamera depan menyala selama Layzee tidak Off dan layar menyala; saat layar mati kamera dilepas dan dipasang lagi otomatis begitu layar menyala (service, notifikasi, dan overlay tetap ada, tidak ada status baru). Start hanya dari dashboard; Stop dari overlay atau dashboard (overlay hilang saat Off).
- Hanya kamera depan, tanpa pemilih kamera.
- Mode overlay untuk pemakaian sehari-hari, dashboard untuk setup dan pengaturan.
- Mode penyangga (HP di stand) sebagai target pertama; handheld mode belakangan.
- Platform: Android saja. `minSdk` 26, `targetSdk` di atas 29 (di bawah itu, sampling gerakan `dispatchGesture` terkunci 100 ms dan patah-patah).
- Distribusi MVP lewat APK langsung (sideload). Alur pengungkapan dan persetujuan Accessibility di dalam app tetap dibangun sesuai syarat Google Play supaya jalur Play terbuka nanti. Jangan set `isAccessibilityTool=true`.

## Scope MVP

### Masuk
- Deteksi pinch satu tangan secara real-time.
- Pinch + gerak vertikal = swipe lewat Accessibility Service.
- Overlay: ikon melayang yang bisa digeser dan menempel ke tepi, warnanya mengikuti status (Idle / Tracking / Pinch). Klik membuka menu kecil: "Stop" dan "Buka dashboard". Tidak ada tombol Start.
- Dashboard:
  - Panduan izin langkah demi langkah dengan status centang: Accessibility Service, overlay, dan kamera dicek otomatis; pengecualian optimasi baterai/autostart dicentang manual oleh pengguna (app membuka halaman pengaturannya). Sertakan penjelasan jujur kenapa Accessibility dibutuhkan dan bahwa gambar diproses lokal.
  - Kalibrasi pinch dengan pratinjau kamera dan landmark. Wajib sebelum Start pertama; bisa diulang kapan saja.
  - Pengaturan inti: sensitivitas pinch (menggeser sedikit hasil kalibrasi).
  - Pratinjau kamera dengan landmark dan label status hidup (Off/Idle/Tracking/Pinch), tanpa tampilan angka. Menggantikan daftar band warna di area uji lama; tidak ada konten scroll.
  - Tampilan informatif dan rapi, bukan layar debug: ikon status berwarna di panduan izin, tanggal kalibrasi terakhir, bagian Tentang & privasi (diproses lokal, tidak ada izin internet, versi app, alasan Accessibility), dan cara pakai 3 baris.
  - Tanpa dependensi UI baru: warna dan gaya lewat `colors.xml`/`themes.xml` (gelap-first, terang mengikuti sistem), kartu lewat drawable XML, ikon lewat vector drawable.
- Ikon: launcher (adaptive vector, ada versi monokrom), overlay (glyph berubah sesuai status, bukan hanya warna), dan notifikasi (monokrom).
- Saat tangan hilang atau pinch dilepas, status kembali ke Tracking/Idle (bukan Off). Pinch berikutnya baru dihitung setelah jari sempat terbuka.
- Pemrosesan gambar lokal.

### Belum Perlu
- Model AI buatan sendiri.
- Banyak gesture.
- Akun dan cloud.
- Integrasi API Instagram/TikTok.
- Deteksi gerakan 3D penuh.
- Dukungan iOS.
- Statistik pemakaian, pemilih tema (tampilan hanya mengikuti mode gelap/terang sistem), tutorial panjang.
- Fitur ala Assistive Touch lain (home, back, screenshot). Overlay hanya wadah Stop dan indikator status.
- Tombol Start di overlay, auto-Off, pemilih kamera, daftar putih app.
- Tampilan angka latensi di app.
- Profil performa otomatis, pemantauan suhu, GPU delegate, ROI crop (ditunda sampai tes di perangkat menunjukkan perlu).

## Tech Stack

- **Kotlin native**, satu modul. Tanpa Flutter, React Native, atau Python.
  - Accessibility Service, overlay (`WindowManager`), dan foreground service kamera memang wajib komponen native.
- **CameraX (ImageAnalysis)** + **MediaPipe Hand Landmarker (Tasks API)**.
- Dependensi minimal. Hindari Firebase, Retrofit, dan library DI berat.
- Dashboard: satu Activity dengan **View klasik (XML), tanpa Compose**. Overlay: satu View kecil lewat `WindowManager`.
- Aktifkan R8 shrink dan ABI splits.
- Kalau nanti pengukuran menunjukkan logika pinch jadi bottleneck, baru pertimbangkan memindahkannya ke Rust/C++. Default: tidak.

## Arsitektur Pipeline

Pipeline satu arah:

```
Kamera (thread analisis, STRATEGY_KEEP_ONLY_LATEST)
  -> gerbang gerak / fps adaptif
  -> HandLandmarker (mode LIVE_STREAM, async, numHands = 1)
  -> PinchDetector (hitungan murni, tanpa alokasi objek)
  -> GestureStateMachine (Idle / Tracking / Pinch; Off = pipeline tidak berjalan)
  -> SwipeDispatcher (Accessibility Service)
```

- Satu thread khusus inferensi supaya UI tidak terganggu.
- Foreground service bertipe kamera dipakai agar kamera tetap aktif saat app lain di depan, dan berjalan sampai pengguna menghentikan (Off).

## Logika Pinch

- **Normalisasi jarak**: jarak ujung jempol (landmark 4) ke ujung telunjuk (landmark 8) dibagi **hand size** (jarak landmark 0 ke 9). Kalikan koordinat ternormalisasi dengan lebar/tinggi gambar sebenarnya sebelum menghitung jarak.
- **Hysteresis + debounce**: ambang masuk dan keluar pinch berbeda, status berubah setelah bertahan 2-3 frame. Angka awal hanya titik mulai dan harus disetel lewat kalibrasi.
- **Kalibrasi personal**: pengguna pinch dan buka tangan beberapa kali (sekitar 5 kali masing-masing), ambang ditentukan dari nilai kedua kondisi. Sensitivitas pinch hanya menggeser ambang itu sedikit.
- **Pinch palsu lebih mahal daripada pinch terlewat** (satu swipe tak sengaja langsung mengganti page di app lain). Karena itu:
  - Konfirmasi ukuran cadangan (misalnya jarak ujung jempol ke sendi tengah telunjuk): pinch hanya mulai jika jarak ujung **dan** ukuran cadangan sama-sama di bawah ambang (AND, bukan OR).
  - Debounce 3 frame, dan tidak ada pinch jika **hand size** di bawah batas minimum dalam piksel (angka awal disetel lewat uji); status tetap Tracking.
  - Smoothing One Euro filter pada satu angka rasio pinch (bukan pada koordinat landmark), dengan selang waktu dari timestamp nyata karena fps adaptif. Tanpa dependensi, tanpa alokasi.
  - Jika tangan hilang atau skor kepercayaan landmark jatuh saat status Pinch, status langsung keluar dari Pinch (tanpa masa tenggang).
- **Swipe**: satu `dispatchGesture` berisi satu goresan lurus ke atas, cepat (~120 ms, sekitar 45% tinggi layar) dan berakhir saat masih bergerak, supaya dibaca sebagai fling. Tidak ada goresan berlanjut, jadi tidak ada yang perlu diangkat atau dibatalkan. Flick yang diminta saat flick sebelumnya masih berjalan dibuang.
  - Uji per app (jarak, durasi) apakah page benar-benar berganti; jangan diasumsikan.

## Aturan Hemat Beban

- Resolusi analisis rendah (mulai sekitar 320x240 atau 480x360). Naikkan hanya jika uji menunjukkan pinch meleset. Arah optimasi: dari hemat ke akurat.
- **FPS adaptif**: rendah (sekitar 5 fps) saat tidak ada tangan (Idle), naik (15-20 fps) saat tangan terlihat, tertinggi saat pinch, kembali ke fps Idle jika tangan hilang beberapa detik. Tidak ada auto-Off.
- **Gerbang gerak**: saat Idle, bandingkan frame abu-abu sangat kecil (misalnya 64x48) dan lewati model jika selisih kecil.
- Hanya satu tangan, hanya frame terbaru, tanpa pratinjau kamera di overlay (pratinjau hanya aktif saat dashboard terbuka). Selama pratinjau dashboard/kalibrasi terbuka, pacer dan gerbang gerak dimatikan supaya gambar lancar; mode berjalan di app lain tidak berubah.
- Rentang fps kamera 15-30 tidak diubah per status (keputusan pengguna). Pengukuran baterai/beban ditunda: lihat `.scratch/polish/deferred.md`.
- Minta CameraX output `OUTPUT_IMAGE_FORMAT_RGBA_8888`, jangan konversi YUV ke RGB manual di Kotlin.
- Kunci rentang fps kamera (`CONTROL_AE_TARGET_FPS_RANGE`) agar fps tidak turun di cahaya redup.
- Pakai ulang buffer dan objek di jalur frame. Hindari membuat `List`/`Pair` baru per frame.
- **CPU saja** untuk MVP.
- Ditunda sampai tes di perangkat menunjukkan perlu: profil performa otomatis, pemantauan status termal, GPU delegate, ROI crop di sekitar tangan.

## Yang Sebaiknya Dihindari

- Deteksi kulit berbasis warna sebagai pengganti landmark.
- Model atau klasifier tambahan di tahap awal (simpan sebagai opsi jika aturan jarak + hysteresis terbukti tidak cukup).
- Optimasi tanpa pengukuran. Catat waktu tiap tahap (kamera, inferensi, logika, dispatch) sebelum menambah kompleksitas. Pencatatan hanya ada di build debug (logcat); tanpa tampilan angka dan tanpa biaya di build rilis.

## Anggaran "Ringan" (Ditetapkan Setelah Spike)

Tentukan target terukur setelah spike di HP low-end:
- Ukuran APK
- CPU rata-rata saat tracking
- Persen baterai per jam
- Suhu HP setelah 15 menit
- Latensi dari pinch sampai layar bergerak dan waktu scroll berhenti setelah pinch dilepas

Batas awal sementara untuk spike (boleh direvisi setelah ada data):
- Pinch terdeteksi benar ≥ 95% di cahaya ruangan normal, jarak tangan 30-60 cm.
- Pinch sampai layar bergerak ≤ 150 ms; pinch dilepas sampai gerak berhenti ≤ 100 ms.
- Pinch palsu (swipe tak sengaja) paling banyak 1 kali per 5 menit saat Tracking.
- HP tidak melambat atau panas mengganggu setelah 15 menit.

Kalau kamera yang menyala terus (tanpa auto-Off) melewati anggaran baterai, pertimbangkan lagi auto-Off.

## Perangkat Uji

- Idealnya dua HP: satu low-end dan satu mid-range, merek berbeda. Saat ini hanya ada satu HP Realme; catat model, RAM, dan versi Android-nya, dan catat ketiadaan pembanding sebagai risiko.
- Ukur di HP terlemah dulu.
- Realme termasuk merek dengan manajemen baterai agresif, jadi syarat uji itu terpenuhi.

## Langkah Kerja

1. **Spike 1-2 minggu** di HP uji yang ada:
   - Pinch andal di berbagai sudut dan pencahayaan.
   - Goresan Accessibility terasa mulus di Instagram/TikTok.
   - Ukur latensi dan kecepatan gerak berhenti lewat log debug.
2. Dashboard minimal (panduan izin, kalibrasi dengan preview, playground), yang juga menjadi alat uji spike.
3. Overlay.
4. Tetapkan anggaran ringan yang terukur.

## Konvensi Kerja

- Bahasa komunikasi: Indonesia. Nama kelas, fungsi, dan komentar kode boleh dalam bahasa Inggris.
- Utamakan solusi paling sederhana yang memenuhi scope MVP. Tanyakan dulu sebelum menambah dependensi atau fitur di luar daftar "Masuk".
- Setiap perubahan di jalur frame (kamera, inferensi, deteksi pinch) harus disertai pertimbangan beban dan, bila memungkinkan, pengukuran waktu.

## Agent skills

### Issue tracker

Issue dan spec dilacak sebagai file markdown lokal di `.scratch/<fitur>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Lima label default (needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: satu `CONTEXT.md` + `docs/adr/` di root. See `docs/agents/domain.md`.
