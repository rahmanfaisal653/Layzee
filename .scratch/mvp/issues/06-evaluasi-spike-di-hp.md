# 06: Evaluasi spike di HP

**What to build:** Evaluasi manual oleh manusia terhadap jalur tipis dari tiket 05, di HP uji dan di Instagram/TikTok. Hasilnya keputusan berdasar data: lanjut membangun produk, setel dulu, atau tinjau ulang ide. Tiket ini menjadi titik tinjau sebelum bagian produk (07 dan seterusnya) dibangun.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 05 (Pinch menggulir playground); bagian uji di Instagram/TikTok juga menunggu 07 (foreground service)

**Status:** ready-for-human

- [x] Model, RAM, dan versi Android HP uji (Realme) dicatat; ketiadaan HP pembanding low-end vs mid-range dicatat sebagai risiko.
- [x] Pinch diuji di berbagai sudut dan pencahayaan, jarak tangan 30-60 cm; hasilnya dibandingkan dengan batas awal (≥ 95% terdeteksi benar).
- [ ] Goresan diuji di Instagram dan TikTok: terasa mulus, swipe cepat mengganti page, swipe pelan dan pendek tidak mengganti page. Perbedaan perilaku per app dicatat.
- [ ] Latensi dari log debug dibandingkan dengan batas awal (pinch sampai layar bergerak ≤ 150 ms; pinch dilepas sampai gerak berhenti ≤ 100 ms).
- [x] Pinch palsu saat Tracking dihitung selama pemakaian normal (batas awal: paling banyak 1 kali per 5 menit).
- [x] Setelah 15 menit, HP tidak melambat atau panas mengganggu.
- [ ] Hal yang belum terkonfirmasi di spec dicatat hasilnya: durasi fps tinggi setelah tangan hilang, pengaruh pelambatan segmen terakhir terhadap fling di tiap app, dan latensi antar segmen goresan berlanjut.
- [ ] Temuan, angka, dan keputusan (lanjut / setel / tinjau ulang) ditulis di `.scratch/mvp/spike-findings.md`. Batas awal boleh direvisi di sana.

## Comments

- 2026-10-10: Tiket ini evaluasi manual (pinch, Instagram/TikTok, 15 menit pemakaian), jadi hanya item pertama yang bisa dikerjakan agen; sisanya sengaja belum dicentang dan status tetap `ready-for-human`.
  - Item 1 selesai: spesifikasi dibaca lewat `adb` (Realme RMX3241, MT6833, ~7,8 GB RAM, Android 13) dan risiko ketiadaan HP pembanding dicatat di `.scratch/mvp/spike-findings.md`. Catatan di sana: HP ini lebih dekat ke mid-range, dan **TikTok belum terpasang** di HP uji.
  - `spike-findings.md` sudah dibuat dengan tabel batas awal dan bagian lain bertanda BELUM DIUJI, siap diisi. Latensi pinch-sampai-layar-bergerak tidak bisa dibaca dari log `dispatch` saja (lihat catatan di berkas itu).
  - Sebelum uji: cek ulang arah swipe (tangan naik = konten naik) karena landmark sempat salah ruang sebelum perbaikan tiket 04.
- 2026-10-10 (hasil uji pengguna): Pengguna menguji di dalam app Layzee (playground) dan melaporkan semuanya aman: pinch, pinch palsu, dan 15 menit pemakaian. Tanpa angka, hanya laporan kualitatif. Dicentang: sudut/cahaya, pinch palsu, 15 menit.
  - Belum bisa diuji: Instagram/TikTok. Pipeline berhenti di `DashboardActivity.onStop()` saat app lain dibuka dan kamera latar belakang butuh foreground service (tiket 07). Item goresan di app target, durasi fps tinggi, fling per app, dan latensi antar segmen tetap terbuka; latensi juga belum punya angka (log `dispatch` bukan latensi ujung ke ujung).
  - Tiket tetap `ready-for-human` sampai 07 selesai dan uji di Instagram dilakukan.
- 2026-10-10: Tiket 07 selesai di sisi kode (foreground service), jadi uji di Instagram sudah bisa dilakukan: Start di Dashboard, pindah ke Instagram, lalu pinch. Status tetap `ready-for-human`.
