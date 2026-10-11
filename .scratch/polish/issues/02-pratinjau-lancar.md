# 02: Pratinjau kamera lancar

**What to build:** Selama pratinjau dashboard/kalibrasi terbuka, pacer dan gerbang gerak dimatikan supaya gambar tidak patah saat tangan tidak terlihat.

**Spec:** `.scratch/polish/spec.md`

**Blocked by:** none

**Status:** ready-for-agent

- [ ] `CameraPipeline` menerima mode pratinjau yang melewati `FramePacer.admit` (semua frame diteruskan).
- [ ] Mode berjalan di service tidak berubah.
- [ ] Pratinjau hanya aktif saat layar itu terbuka dan Layzee Off (seperti sekarang).
- [ ] Diverifikasi di HP uji: gambar tidak patah saat tangan keluar frame.
