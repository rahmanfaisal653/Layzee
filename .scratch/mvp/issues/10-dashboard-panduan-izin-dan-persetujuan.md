# 10: Dashboard: panduan izin dan persetujuan

**What to build:** Dashboard (satu Activity, View klasik) yang memandu pengguna baru langkah demi langkah memberi semua izin, dengan penjelasan jujur kenapa Accessibility dibutuhkan dan persetujuan di dalam app. Start baru bisa ditekan saat izin lengkap. Playground bisa dibuka dari sini.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 07 (Foreground service dan Start/Stop)

**Status:** resolved

- [x] Dashboard berupa satu Activity dengan View klasik (XML), tanpa Compose.
- [x] Panduan langkah demi langkah dengan status centang: Accessibility Service, overlay, dan kamera dicek otomatis.
- [x] Langkah baterai/autostart: app membuka halaman pengaturan yang relevan, lalu pengguna sendiri mencentang "sudah saya atur".
- [x] Penjelasan jujur kenapa Accessibility dibutuhkan dan bahwa gambar diproses lokal, ditampilkan di dalam app.
- [x] Persetujuan eksplisit (tindakan afirmatif pengguna) di dalam app sebelum pengguna diarahkan ke pengaturan Accessibility; tidak cukup hanya di deskripsi atau kebijakan privasi.
- [x] Tombol Start aktif hanya saat izin lengkap.
- [x] Jika Layzee ditemukan mati tanpa Stop dari pengguna, Dashboard menampilkan petunjuk untuk memeriksa lagi langkah baterai/autostart.
- [x] Playground (tiket 05) bisa dibuka dari Dashboard.
- [x] Pratinjau kamera tidak aktif di layar ini (hanya aktif di kalibrasi).

## Comments

- Diimplementasi. Menunggu tes di HP uji: panduan izin di Realme, halaman pengaturan baterai/autostart, dan deteksi mati-tanpa-Stop (matikan lewat pembersih/force stop sistem, buka Dashboard).
- Kalibrasi (tiket 11) belum ada, jadi Start belum bergantung padanya. Pratinjau kamera dan landmark dihapus dari Dashboard; `LayzeeService.preview` tetap ada untuk dipakai kalibrasi.
- Review: Start juga mensyaratkan persetujuan in-app (Accessibility yang dinyalakan langsung dari Settings tanpa dialog tidak cukup); flag "harus berjalan" ditulis saat service benar-benar jadi, bukan saat Start diminta; fallback pengaturan tidak crash. Dibiarkan: `LayzeeService.preview`/`LandmarkView` untuk tiket 11; Start tetap butuh izin overlay (sesuai tiket); pesan saat izin kamera ditolak belum ada.
- 2026-10-11: Diverifikasi selesai oleh pengguna (panduan izin, baterai/autostart, deteksi mati-tanpa-Stop di HP uji). Tiket resolved.
