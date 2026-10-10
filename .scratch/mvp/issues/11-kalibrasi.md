# 11: Kalibrasi

**What to build:** Layar kalibrasi di Dashboard: pengguna pinch dan membuka tangan beberapa kali sambil melihat pratinjau kamera dan landmark, lalu ambang pinch pribadinya ditentukan dari kedua kondisi itu dan dipakai gesture engine. Wajib sebelum Start pertama dan bisa diulang kapan saja.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 10 (Dashboard: panduan izin dan persetujuan)

**Status:** ready-for-agent

- [ ] Layar kalibrasi menampilkan pratinjau kamera dan landmark; pratinjau hanya aktif di layar ini dan mati saat pengguna keluar.
- [ ] Pengguna diminta sekitar 5 kali pinch dan 5 kali tangan terbuka; ambang masuk dan keluar pinch dihitung dari nilai kedua kondisi.
- [ ] Start tidak bisa ditekan sebelum kalibrasi pernah selesai.
- [ ] Kalibrasi bisa diulang kapan saja dari Dashboard.
- [ ] Hasil kalibrasi tersimpan di perangkat dan dipakai gesture engine saat Start berikutnya.
- [ ] Jika tangan tidak terdeteksi atau sampel tidak cukup, pengguna melihat pesan jelas dan bisa mengulang.
- [ ] Diverifikasi di HP uji: setelah kalibrasi, pinch pengguna terbaca dan tangan terbuka tidak terbaca sebagai pinch.
