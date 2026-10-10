# 11: Kalibrasi

**What to build:** Layar kalibrasi di Dashboard: pengguna pinch dan membuka tangan beberapa kali sambil melihat pratinjau kamera dan landmark, lalu ambang pinch pribadinya ditentukan dari kedua kondisi itu dan dipakai gesture engine. Wajib sebelum Start pertama dan bisa diulang kapan saja.

**Spec:** `.scratch/mvp/spec.md`

**Blocked by:** 10 (Dashboard: panduan izin dan persetujuan)

**Status:** ready-for-human

- [x] Layar kalibrasi menampilkan pratinjau kamera dan landmark; pratinjau hanya aktif di layar ini dan mati saat pengguna keluar.
- [x] Pengguna diminta sekitar 5 kali pinch dan 5 kali tangan terbuka; ambang masuk dan keluar pinch dihitung dari nilai kedua kondisi.
- [x] Start tidak bisa ditekan sebelum kalibrasi pernah selesai.
- [x] Kalibrasi bisa diulang kapan saja dari Dashboard.
- [x] Hasil kalibrasi tersimpan di perangkat dan dipakai gesture engine saat Start berikutnya.
- [x] Jika tangan tidak terdeteksi atau sampel tidak cukup, pengguna melihat pesan jelas dan bisa mengulang.
- [ ] Diverifikasi di HP uji: setelah kalibrasi, pinch pengguna terbaca dan tangan terbuka tidak terbaca sebagai pinch.

## Comments

- Diimplementasi. Menunggu tes di HP uji: kalibrasi di Realme (pratinjau dan landmark terbaca, 5x tangan terbuka + 5x pinch), lalu Start dan cek pinch terbaca / tangan terbuka tidak.
- Cara kerja: sepuluh "permintaan" bergantian (buka, pinch, ...), masing-masing jendela 1 detik setelah 0,7 detik penyesuaian; nilai tiap permintaan = median jarak (satuan hand size). Ambang masuk/keluar = pinch terlonggar + 30%/60% jarak ke tangan terbuka terketat (titik awal, disetel lewat pemakaian nyata). Ukuran cadangan (jempol ke PIP telunjuk) dimatikan bila tidak memisahkan kedua kondisi.
- Permintaan diulang otomatis dengan pesan bila tangan tidak terbaca atau terlalu bergerak; jika pinch dan tangan terbuka terlalu mirip, kalibrasi gagal dan hasil lama tidak ditimpa.
- Kalibrasi menjalankan kamera sendiri di Dashboard, jadi tombolnya nonaktif saat Layzee berjalan (kamera depan dipakai service); Stop dulu. `LayzeeService.preview` dihapus karena tidak dipakai.
- Start kini juga mensyaratkan kalibrasi (`SetupState.canStart(permissions, calibrated)`).
