# 06: Evaluasi spike di HP

**What to build:** Evaluasi manual oleh manusia terhadap jalur tipis dari tiket 05, di HP uji dan di Instagram/TikTok. Hasilnya keputusan berdasar data: lanjut membangun produk, setel dulu, atau tinjau ulang ide. Tiket ini menjadi titik tinjau sebelum bagian produk (07 dan seterusnya) dibangun.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 05 (Pinch menggulir playground)

**Status:** ready-for-human

- [ ] Model, RAM, dan versi Android HP uji (Realme) dicatat; ketiadaan HP pembanding low-end vs mid-range dicatat sebagai risiko.
- [ ] Pinch diuji di berbagai sudut dan pencahayaan, jarak tangan 30-60 cm; hasilnya dibandingkan dengan batas awal (≥ 95% terdeteksi benar).
- [ ] Goresan diuji di Instagram dan TikTok: terasa mulus, swipe cepat mengganti page, swipe pelan dan pendek tidak mengganti page. Perbedaan perilaku per app dicatat.
- [ ] Latensi dari log debug dibandingkan dengan batas awal (pinch sampai layar bergerak ≤ 150 ms; pinch dilepas sampai gerak berhenti ≤ 100 ms).
- [ ] Pinch palsu saat Tracking dihitung selama pemakaian normal (batas awal: paling banyak 1 kali per 5 menit).
- [ ] Setelah 15 menit, HP tidak melambat atau panas mengganggu.
- [ ] Hal yang belum terkonfirmasi di spec dicatat hasilnya: durasi fps tinggi setelah tangan hilang, pengaruh pelambatan segmen terakhir terhadap fling di tiap app, dan latensi antar segmen goresan berlanjut.
- [ ] Temuan, angka, dan keputusan (lanjut / setel / tinjau ulang) ditulis di `.scratch/mvp/spike-findings.md`. Batas awal boleh direvisi di sana.
