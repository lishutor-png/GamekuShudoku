# SudoKu 9x9 Master (Android)

Game Sudoku 9x9 modern dengan antarmuka Jetpack Compose bernuansa warna Lime Green neon, ramah pemula, dilengkapi opsi ragu-ragu (pensil), sorotan pembatas 3x3 per 9 kotak, serta sistem bantuan cerdas.

## Build APK di GitHub Actions

Repository ini sudah dilengkapi dengan alur otomatis GitHub Actions (`.github/workflows/build-apk.yml`).

### Cara Mengunduh APK dari GitHub:
1. Push / upload project ini ke repositori GitHub Anda (cabang `main` atau `master`).
2. Buka tab **Actions** di repositori GitHub Anda.
3. Alur kerja **Build Android APK** akan berjalan secara otomatis setiap kali ada `push` atau `pull request`, atau bisa dijalankan manual via tombol **Run workflow**.
4. Setelah build selesai (bertanda centang hijau), klik nama workflow run tersebut.
5. Pada bagian **Artifacts** di bawah halaman, klik file **sudoku-debug-apk** untuk mengunduh file `.zip` yang berisi file APK siap pasang ke ponsel Android Anda.

### Build APK Manual di Komputer Lokal:
Pastikan Anda memiliki JDK 17 terpasang:
```bash
# Pastikan gradlew executable (Linux/macOS)
chmod +x gradlew

# Build Debug APK
./gradlew assembleDebug
```
File APK akan berada di direktori:
`app/build/outputs/apk/debug/app-debug.apk`
