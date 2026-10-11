# 04: Ikon app, overlay, dan notifikasi

**What to build:** Ikon yang sekarang tidak ada: launcher, overlay, notifikasi. Semuanya vector drawable.

**Spec:** `.scratch/polish/spec.md`

**Blocked by:** none

**Status:** ready-for-agent

- [ ] Adaptive icon launcher (vector) bermotif jempol dan telunjuk membentuk lingkaran, dengan versi monokrom untuk themed icon.
- [ ] `android:icon` dan `android:roundIcon` di manifest.
- [ ] Overlay: tetap bulat dan kecil, ring berwarna sesuai status, glyph berbeda per status (Idle/Tracking/Pinch) sehingga tidak bergantung pada warna saja.
- [ ] Ikon kecil notifikasi monokrom menggantikan `android.R.drawable.ic_menu_camera` (`LayzeeService.kt`).
- [ ] Ukuran APK rilis dicatat sebelum dan sesudah.
