# VitreGram — Android TV Instagram Client

![Platform](https://img.shields.io/badge/Platform-Android%20TV%20%7C%20Google%20TV-blue)
![API](https://img.shields.io/badge/Min%20API-28%20(Android%209)-green)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20for%20TV-purple)
![Video](https://img.shields.io/badge/Video%20Engine-AndroidX%20Media3%20ExoPlayer-red)
![Build](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions%20APK%20Builder-orange)

**VitreGram** is a native, third-party Instagram client engineered specifically for **Android TV** and **Google TV** displays. It features a **Dark Obsidian Liquid Glass** aesthetic, full D-pad remote navigation, hardware-accelerated Media3 ExoPlayer video streaming, and Clean Architecture.

---

## 📺 Key Highlights

- **Leanback Remote Navigation**: 100% operable with standard D-pad TV remotes (Up, Down, Left, Right, Select/OK, Play/Pause).
- **Instagram OAuth 2.0 Integration (xiandanin/instagram-login-android architecture)**: Supports official OAuth authorization code flow (`https://api.instagram.com/oauth/authorize`). Features automatic authorization code interception upon redirect, instant token exchange (`/oauth/access_token`) via OkHttp, custom TV toolbar with remote actions, and support for custom Meta Developer App credentials.
- **TV Browser Auto-Cookie Extractor**: No tedious remote-typing of long cookies! An embedded TV WebView loads Instagram Web; as soon as the user logs in with their credentials or Facebook, `sessionid` and `ds_user_id` are automatically captured, encrypted into Keystore, and seamlessly transitioned into TV mode.
- **Dark Obsidian Liquid Glass UI**: Near-black (`#08090D`) backdrop, translucent frosted cards, illuminated glass borders, and smooth focus scale animations.
- **Home Feed**: Real posts from followed accounts organized in horizontal TV shelves ("For You", "Popular with Friends").
- **Vertical Reels Player**: Centered 9:16 video player with blurred video backdrop glow, creator audio tags, like/comment counts, and seamless Up/Down transitions.
- **Stories Tray**: Top circular story carousel with vibrant Instagram gradient rings and full-screen timed progress playback.
- **Creator Search & Explore**: Search accounts and reels using the TV on-screen keyboard.
- **Zero Plaintext Passwords**: Uses session token authentication with hardware-backed Android Keystore (`EncryptedSharedPreferences`).

---

## 🚀 GitHub Actions Automated APK Build

This repository is equipped with an automated CI/CD pipeline (`.github/workflows/build-apk.yml`) that builds, signs, and packages installable APKs directly on GitHub's cloud runners.

### How to Get the APK:

1. **Push to GitHub**:
   ```bash
   git add .
   git commit -m "feat: complete VitreGram Android TV client v1.0.0"
   git branch -M main
   git remote add origin https://github.com/<your-username>/VitreGram.git
   git push -u origin main
   ```

2. **Automated CI Run**:
   - The GitHub Action triggers automatically on push, pull request, or manual dispatch.
   - It sets up Java 17, Android SDK Platform 34, executes unit tests, and compiles both **Debug** and **Release** APKs.
   - It signs the Release APK with an automated build keystore.

3. **Download the APK Artifact**:
   - Go to your repository's **Actions** tab on GitHub.
   - Click the latest workflow run: `Compile & Package Android TV APK`.
   - Scroll down to the **Artifacts** section and download `VitreGram-AndroidTV-APKs.zip`.
   - Install `VitreGram-TV-v1.0.0-release.apk` directly onto your Android TV or Google TV box using ADB or a USB drive:
     ```bash
     adb install VitreGram-TV-v1.0.0-release.apk
     ```

4. **Automated GitHub Release (Tagging)**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
   GitHub Actions will automatically generate a new GitHub Release with the signed APK attached as an asset.

---

## 🏛️ Project Architecture

```
VitreGram/
├── .github/
│   └── workflows/
│       └── build-apk.yml            # CI/CD Automated APK Builder
├── app/
│   ├── build.gradle.kts             # Compose for TV, Media3, Room, Security
│   └── src/main/java/com/vitregram/tv/
│       ├── MainActivity.kt          # Android TV landscape entry activity
│       ├── TvApplication.kt         # Application container & dependency wiring
│       ├── navigation/              # TvNavGraph & Screen routing
│       ├── ui/
│       │   ├── theme/               # Dark Obsidian theme, typography & palette
│       │   └── components/          # GlassCard, GlassSidebar, StoryAvatar, MediaCard
│       ├── player/                  # VideoPlayerManager & ReelPlayerOverlay (Media3)
│       ├── domain/                  # Models, UseCases, and Repository interfaces
│       ├── data/                    # Room Database, Keystore encryption, Adapters
│       └── presentation/            # Compose screens (Home, Reels, Stories, Search, Profile)
├── FEASIBILITY_REPORT.md            # Documented Instagram integration analysis
└── README.md
```

---

## 🛡️ Security & Privacy

- VitreGram never prompts for or stores Instagram account passwords.
- All session keys are stored in private sandboxed storage encrypted via AES-256 SIV + AES-256 GCM backed by the Android Keystore.
- Features not exposed by Instagram's API (such as RTMP Live broadcasting and WebRTC calling) are explicitly marked unavailable in the UI per the PRD specification.
