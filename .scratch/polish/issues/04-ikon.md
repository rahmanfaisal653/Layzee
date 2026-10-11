# 04: Ikon app, overlay, dan notifikasi

**What to build:** Ikon yang sekarang tidak ada: launcher, overlay, notifikasi. Semuanya vector drawable.

**Spec:** `.scratch/polish/spec.md`

**Blocked by:** none

**Status:** implemented; menunggu uji visual di HP (Realme)

- [x] Adaptive icon launcher (vector) bermotif jempol dan telunjuk membentuk lingkaran, dengan versi monokrom untuk themed icon.
- [x] `android:icon` dan `android:roundIcon` di manifest.
- [x] Overlay: tetap bulat dan kecil, ring berwarna sesuai status, glyph berbeda per status (Idle/Tracking/Pinch) sehingga tidak bergantung pada warna saja.
- [x] Ikon kecil notifikasi monokrom menggantikan `android.R.drawable.ic_menu_camera` (`LayzeeService.kt`).
- [x] Ukuran APK rilis dicatat sebelum dan sesudah.

## Ukuran APK rilis (byte)

| ABI | Sebelum | Sesudah | Selisih |
|---|---|---|---|
| arm64-v8a | 20.917.586 | 20.921.594 | +4.008 |
| armeabi-v7a | 16.672.700 | 16.676.708 | +4.008 |
| x86_64 | 24.178.009 | 24.182.017 | +4.008 |

Ikon launcher/glyph overlay/notifikasi semuanya vector XML; selisih ~4 KB (termasuk ikon default sebelumnya yang tidak ada). Tampilan di HP belum dilihat langsung.
