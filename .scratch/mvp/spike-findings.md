# Temuan spike (tiket 06)

Diisi bertahap saat evaluasi di HP. Bagian bertanda **BELUM DIUJI** menunggu pengujian manusia; jangan diisi dari dugaan.

Keputusan akhir: **BELUM DITENTUKAN** (lanjut / setel / tinjau ulang)

## Perangkat uji

Dibaca lewat `adb` pada 2026-10-10.

| | |
|---|---|
| Model | Realme RMX3241 |
| SoC | MediaTek `mt6833` (8 core) |
| RAM | ~7,8 GB (`MemTotal` 7.824.820 kB) |
| Android | 13 (SDK 33) |
| Build | `RMX3241_11_F.12` |
| App target terpasang | Instagram (`com.instagram.android`), Threads. **TikTok belum terpasang.** |

**Risiko:** hanya ada satu HP uji, dan RAM 8 GB serta SoC-nya lebih dekat ke mid-range daripada low-end. Hasil di sini tidak mewakili HP low-end sungguhan, dan tidak ada pembanding antar merek. Angka CPU, baterai, dan suhu kemungkinan lebih baik daripada di HP terlemah target.

## Hasil per batas awal

| Batas awal | Hasil | Status |
|---|---|---|
| Pinch terdeteksi benar ≥ 95% (cahaya normal, 30-60 cm) | Dilaporkan aman di playground, tanpa angka | Lolos (kualitatif) |
| Pinch sampai layar bergerak ≤ 150 ms | | BELUM DIUJI |
| Pinch dilepas sampai gerak berhenti ≤ 100 ms | | BELUM DIUJI |
| Pinch palsu ≤ 1 kali per 5 menit saat Tracking | Dilaporkan aman di playground, tanpa hitungan | Lolos (kualitatif) |
| Tidak melambat atau panas mengganggu setelah 15 menit | Dilaporkan aman (hanya di app Layzee) | Lolos (kualitatif) |

Latensi: `adb logcat -s LayzeeTiming`. Catatan dari tiket 05: waktu `dispatch` di log hanya durasi pemanggilan `dispatchGesture`, bukan waktu sampai layar bergerak, jadi latensi pinch-sampai-layar-bergerak perlu cara ukur lain (mis. rekam layar + kamera, atau tambah log di sisi playground).

## Pinch: sudut dan pencahayaan

Dilaporkan pengguna aman di playground, tanpa rincian per kondisi. Kalau mau lebih tegas, catat per kondisi (sudut HP, cahaya, jarak, jumlah percobaan, jumlah terdeteksi benar).

## Goresan di app target

BELUM DIUJI: pipeline mati saat app lain dibuka (berhenti di `DashboardActivity.onStop()`, kamera latar belakang butuh foreground service tiket 07). Uji di Instagram/TikTok menunggu 07. Di playground goresan dilaporkan mulus.

Rencana: Per app (Instagram, TikTok): mulus atau tidak, swipe cepat mengganti page, swipe pelan dan pendek tidak mengganti page.

Sebelum uji: arah swipe harus dicek ulang. Setelah perbaikan rotasi di tiket 04, tangan naik harus membuat konten naik (lihat komentar verifikasi tiket 05).

## Belum terkonfirmasi di spec

- Durasi fps tinggi setelah tangan hilang: BELUM DIUJI. (Fps adaptif baru dibangun di tiket 08; di jalur tipis ini mungkin belum ada yang bisa diukur.)
- Pengaruh pelambatan segmen terakhir terhadap fling di tiap app: BELUM DIUJI.
- Latensi antar segmen goresan berlanjut: BELUM DIUJI.

## Revisi batas awal

Belum ada.
