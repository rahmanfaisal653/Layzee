# 12: Pengaturan: swipe ratio dan sensitivitas pinch

**What to build:** Dua pengaturan inti di Dashboard yang tersimpan di perangkat: swipe ratio (seberapa jauh gerak tangan untuk satu swipe) dan pinch sensitivity (menggeser sedikit hasil kalibrasi ke lebih longgar atau lebih ketat).

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 11 (Kalibrasi)

**Status:** ready-for-agent

- [ ] Dashboard punya slider swipe ratio dan slider pinch sensitivity dengan nilai default yang wajar.
- [ ] Pinch sensitivity hanya menggeser sedikit ambang hasil kalibrasi; tidak menggantikannya dan tidak memerlukan kalibrasi ulang.
- [ ] Kedua nilai tersimpan di perangkat dan tidak perlu diatur ulang tiap Start.
- [ ] Perubahan berlaku paling lambat saat Start berikutnya.
- [ ] Diverifikasi di playground: menaikkan swipe ratio membuat jarak swipe terlihat lebih jauh untuk gerak tangan yang sama; menurunkannya sebaliknya.
- [ ] Diverifikasi di HP uji: sensitivitas lebih ketat mengurangi pinch tak sengaja, lebih longgar membuat pinch lebih mudah terbaca.
- [ ] Tidak ada pengaturan "kecepatan scroll" atau "pilihan kamera" (di luar scope).

## Comments

- 2026-10-10: Swipe ratio tidak ada lagi (pinch = satu flick tetap, lihat komentar di tiket 07). Yang tersisa untuk tiket ini: sensitivitas pinch, dan bila perlu jarak/kecepatan flick (`flickDistanceFraction`, durasi di `SwipeDispatcher`) sebagai pengaturan. Tinjau ulang lingkup tiket ini sebelum dikerjakan.
