# 09: Overlay

**What to build:** Ikon kecil melayang di atas app lain yang menunjukkan status Layzee dan membuka menu "Stop" dan "Buka dashboard", mirip Assistive Touch. Muncul saat Start dan hilang saat Off. Tidak ada tombol Start di sini.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 07 (Foreground service dan Start/Stop)

**Status:** ready-for-agent

- [ ] Overlay muncul saat Layzee Start dan hilang saat Off, sebagai satu View kecil lewat `WindowManager`.
- [ ] Warna overlay mengikuti status Idle / Tracking / Pinch.
- [ ] Overlay bisa digeser dan menempel ke tepi layar.
- [ ] Klik overlay membuka menu kecil dengan dua tombol: "Stop" dan "Buka dashboard".
- [ ] "Stop" membuat Layzee Off sepenuhnya (kamera mati, overlay hilang); "Buka dashboard" membuka Dashboard.
- [ ] Overlay tidak menampilkan pratinjau kamera dan tidak punya tombol Start.
- [ ] Overlay yang menempel di tepi tidak menghalangi titik awal swipe di tengah layar.
- [ ] Diverifikasi di HP uji saat Instagram/TikTok di depan.
