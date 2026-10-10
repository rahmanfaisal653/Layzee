# 10: Dashboard: panduan izin dan persetujuan

**What to build:** Dashboard (satu Activity, View klasik) yang memandu pengguna baru langkah demi langkah memberi semua izin, dengan penjelasan jujur kenapa Accessibility dibutuhkan dan persetujuan di dalam app. Start baru bisa ditekan saat izin lengkap. Playground bisa dibuka dari sini.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 07 (Foreground service dan Start/Stop)

**Status:** ready-for-agent

- [ ] Dashboard berupa satu Activity dengan View klasik (XML), tanpa Compose.
- [ ] Panduan langkah demi langkah dengan status centang: Accessibility Service, overlay, dan kamera dicek otomatis.
- [ ] Langkah baterai/autostart: app membuka halaman pengaturan yang relevan, lalu pengguna sendiri mencentang "sudah saya atur".
- [ ] Penjelasan jujur kenapa Accessibility dibutuhkan dan bahwa gambar diproses lokal, ditampilkan di dalam app.
- [ ] Persetujuan eksplisit (tindakan afirmatif pengguna) di dalam app sebelum pengguna diarahkan ke pengaturan Accessibility; tidak cukup hanya di deskripsi atau kebijakan privasi.
- [ ] Tombol Start aktif hanya saat izin lengkap.
- [ ] Jika Layzee ditemukan mati tanpa Stop dari pengguna, Dashboard menampilkan petunjuk untuk memeriksa lagi langkah baterai/autostart.
- [ ] Playground (tiket 05) bisa dibuka dari Dashboard.
- [ ] Pratinjau kamera tidak aktif di layar ini (hanya aktif di kalibrasi).
