# 07: Foreground service dan Start/Stop

**What to build:** Status Off ↔ Idle yang sungguhan. Pengguna menekan Start di Dashboard, kamera menyala lewat foreground service dan tetap aktif saat app lain di depan, sampai pengguna menekan Stop. Tidak ada auto-Off. Pipeline dari kamera sampai dispatcher berjalan di dalam service.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 06 (Evaluasi spike di HP)

**Status:** ready-for-agent

- [ ] Tombol Start di Dashboard menyalakan foreground service bertipe kamera (dengan notifikasi yang diwajibkan Android); status berpindah dari Off ke Idle.
- [ ] Kamera dan pipeline tetap berjalan saat app lain (misalnya Instagram) ada di depan, dan gesture menggulir app itu.
- [ ] Stop dari Dashboard mematikan service dan kamera sepenuhnya (Off).
- [ ] Tidak ada auto-Off: Idle yang lama tetap menyala sampai pengguna menghentikan.
- [ ] Tombol debug dari tiket 05 digantikan oleh Start/Stop.
- [ ] Jika service dihentikan sistem di tengah Pinch, jari virtual terangkat dan tidak ada goresan menggantung.
- [ ] Hanya satu tempat yang memulai pipeline (service); Dashboard tidak menjalankan kamera sendiri untuk pemakaian sehari-hari.
