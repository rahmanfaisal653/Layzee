# Layzee

App Android "air trackpad": kamera depan membaca gesture pinch satu tangan untuk menggulir feed di app lain lewat Accessibility Service.

## Language

**Pinch**:
Gesture satu tangan: ujung jempol dan ujung telunjuk bertemu. Satu-satunya gesture yang dibaca Layzee.
_Avoid_: Gesture (terlalu umum), klik, grab

**Swipe**:
Satu flick cepat ke atas dari titik awal tetap di layar, dikirim lewat Accessibility Service. Satu pinch = satu swipe, jadi pinch langsung mengganti page di app paging (Reels, TikTok). Gerak tangan tidak dibaca.
_Avoid_: Scroll, drag, virtual finger (konsep lama: jari yang menempel dan mengikuti gerak tangan, sudah dihapus)

**Hand size**:
Jarak pergelangan ke pangkal jari tengah (landmark 0 ke 9). Satuan ukur yang membuat deteksi pinch tidak bergantung pada jarak tangan ke kamera.
_Avoid_: Ukuran tangan di frame, hand scale

### Status

Empat status tetap yang dipakai state machine, overlay, dan aturan fps adaptif. Tidak ada status lain.

**Off**:
Layanan dan kamera mati, dan overlay tidak terlihat. Hanya dicapai oleh pengguna: Stop dari overlay atau dari dashboard. Tidak ada auto-Off. Keluar dari Off hanya lewat Start di dashboard.
_Avoid_: Stopped, berhenti, mati, paused

**Idle**:
Layanan berjalan dan kamera menyala selama layar menyala, tetapi tidak ada tangan terlihat. Fps rendah. Saat layar mati kamera dilepas dan dipasang lagi otomatis begitu layar menyala; itu bukan perubahan status dan bukan **Off**.
_Avoid_: Standby, siaga, waiting

**Tracking**:
Tangan terlihat tetapi belum melakukan pinch. Tangan yang terlalu jauh (**hand size** di bawah batas minimum) tetap **Tracking**: pinch tidak dihitung sama sekali.
_Avoid_: Detected, ready

**Pinch** (sebagai status):
Pinch sedang bertahan. Pinch yang baru terdeteksi mengirim satu **Swipe**; menahannya tidak mengirim swipe lagi. Hilangnya tangan atau lepasnya pinch kembali ke **Tracking** atau **Idle**, bukan **Off**.
_Avoid_: Pinching, Active, Dragging

### Aplikasi

**Dashboard**:
Layar utama app untuk setup dan pengaturan. Satu-satunya tempat untuk Start, dan tempat kalibrasi dan pratinjau kamera berada. Menampilkan status Layzee yang sedang berjalan.
_Avoid_: Home, main screen

**Overlay**:
Ikon kecil melayang di atas app lain yang menunjukkan status dan membuka menu Stop atau Buka dashboard. Bisa digeser dan menempel ke tepi layar. Terlihat selama Layzee tidak **Off**.
_Avoid_: Widget, floating button, bubble

**Calibration**:
Sesi singkat di dashboard: pengguna pinch dan membuka tangan beberapa kali, dan ambang pinch pribadinya ditentukan dari kedua kondisi itu. Wajib sebelum Start pertama.
_Avoid_: Setup, training

**Pinch sensitivity**:
Pengaturan yang menggeser sedikit ambang hasil **calibration** ke lebih longgar atau lebih ketat. Tidak menggantikan calibration.
_Avoid_: Threshold slider
