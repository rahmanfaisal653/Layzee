# 03: Lepas kamera saat layar mati

**What to build:** Service melepas kamera saat layar mati dan memasangnya lagi saat layar menyala. Bukan Off dan bukan status baru.

**Spec:** `.scratch/polish/spec.md`

**Blocked by:** none

**Status:** resolved

- [x] Receiver `ACTION_SCREEN_OFF`/`ACTION_SCREEN_ON` didaftarkan dinamis di `LayzeeService` dan dilepas saat service berhenti.
- [x] Layar mati: use case kamera dilepas (model dan konversi berhenti); service, notifikasi, dan overlay tetap ada.
- [x] Layar menyala: kamera dipasang lagi tanpa tindakan pengguna, dengan status awal Idle.
- [ ] Diuji di HP uji (Realme): rebind di dalam foreground service yang sudah berjalan berhasil, termasuk setelah layar lama mati.
- [x] Tidak ada WakeLock, timer, atau polling baru.

## Comments

- 2026-10-11: Ditutup atas keputusan pengguna ("sudah cukup"). Kode selesai; uji di HP Realme tidak dicatat sebagai verifikasi terpisah.
