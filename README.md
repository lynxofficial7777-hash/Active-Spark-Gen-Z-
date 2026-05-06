# ⚡ Active Spark Gen 7 — AI Fitness Battle App

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue?style=for-the-badge&logo=jetpackcompose" />
  <img src="https://img.shields.io/badge/AI-MediaPipe-orange?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Backend-Firebase-yellow?style=for-the-badge&logo=firebase" />
  <img src="https://img.shields.io/badge/Status-Active-brightgreen?style=for-the-badge" />
</p>

> **A real-time AI-powered fitness battle app for kids — built entirely in Kotlin with on-device pose detection, live multiplayer battles, and gamified XP progression.**

---

## 🎯 Problem Statement

Kids skip exercise because it's boring and solitary. Active Spark turns fitness into a **competitive game** — children challenge each other to squat, push-up, and jump battles. The AI camera tracks every rep automatically so there's no cheating and no manual counting. **Result: exercise becomes something kids actually want to do.**

---

## ✨ Key Features

- 🦴 **Real-Time Pose Detection** — MediaPipe PoseLandmarker tracking 33 body landmarks at ~30fps, entirely on-device. No cloud, no lag.
- 🏋️ **10+ Exercise Types** — Push-ups, squats, sit-ups, lunges, burpees, planks, high knees, mountain climbers, jumping jacks, dance moves
- ⚔️ **Live Multiplayer Battles** — Challenge friends in real-time. Opponent reps sync instantly via Firebase Realtime Database
- 🎮 **Gamified XP System** — Earn XP for every rep, level up, unlock ranks from Rookie to Legend
- 🏆 **Leaderboard** — Global rankings updated atomically via Firebase Cloud Functions (no race conditions)
- 🔔 **Push Notifications** — Battle requests, match results, and reminders via Firebase Cloud Messaging
- 👤 **Avatar & Profile System** — Customizable player profiles stored in Firebase Firestore
- 📊 **Form Score** — AI grades your exercise form in real time using joint angle geometry
- 🔐 **Firebase Authentication** — Secure email/password login and registration

---

## 🔧 Hard Engineering Problems Solved

### 1. CameraX Row-Stride Padding Corruption
RGBA_8888 frames from CameraX have padding bytes per row on many devices. Direct `copyPixelsFromBuffer()` produced garbled/corrupted images fed to MediaPipe. **Fixed** with row-by-row copy that strips padding bytes before building the bitmap.

### 2. Camera Restarting Every Rep
`AndroidView.update{}` fires on every Jetpack Compose recomposition — which happens on every state change (every rep count). Camera was calling `unbindAll()` + rebind every single rep. **Fixed** with an `isBound` guard flag that prevents rebinding if camera is already running.

### 3. Matrix Transform Order (Front Camera Mirror)
Flipping the bitmap after rotation used wrong pivot coordinates → incorrect mirror effect on front camera. **Fixed** by applying flip **before** rotation so the pivot is always relative to the original frame dimensions.

### 4. Leaderboard Race Condition
Concurrent rep submissions from multiple users caused incorrect leaderboard totals. **Fixed** using Firebase Cloud Function atomic transactions — reads and writes happen in a single server-side transaction, preventing data corruption.

### 5. Android 16 KB Page Alignment (API 36+)
MediaPipe native `.so` libraries are not 16 KB page-aligned, causing install failure on Android 16 (API 37) devices. **Fixed** with `useLegacyPackaging = true` in Gradle + `extractNativeLibs="true"` in AndroidManifest.

### 6. Rep Counter False Positives
Rep counter was counting reps without any exercise being performed. **Fixed** with:
- **Visibility gate** — skips frames where key landmarks (hips, shoulders) are below 50% visibility
- **800ms cooldown** — prevents rapid consecutive false reps
- **Angle-based detection** — uses joint angle geometry instead of raw Y-position, adapting to different body sizes and camera distances

---

## 🏗️ Architecture

```
app/
├── data/
│   ├── models/          # ExerciseType, User, Match, Score, AvatarConfig
│   └── repository/      # FirebaseRepository (Firestore + RTDB + Auth)
├── ui/
│   ├── screens/
│   │   ├── splash/      # SplashScreen + SplashViewModel
│   │   ├── auth/        # Login, Register screens
│   │   ├── home/        # HomeScreen, Dashboard
│   │   ├── battle/      # BattleScreen, RepCounter, PoseLandmarkerHelper, CameraPreviewView
│   │   ├── leaderboard/ # LeaderboardScreen
│   │   └── profile/     # ProfileScreen, AvatarSelector
│   └── theme/           # ActiveSparkTheme, Colors, Typography
├── navigation/          # AppNavGraph, Screen sealed class
├── services/            # ActiveSparkMessagingService (FCM)
└── di/                  # Hilt modules
```

**Pattern:** MVVM + Hilt Dependency Injection + Kotlin Coroutines + StateFlow

---

## 🧠 How Rep Counting Works

The `RepCounter` class uses a **3-state machine** (IDLE → DOWN → UP → IDLE) driven by **joint angle geometry**:

```
IDLE  →  angle drops below downAngle  →  DOWN
DOWN  →  angle rises above upAngle    →  UP   (rep detected ✅)
UP    →  reset                        →  IDLE
```

Each exercise maps to specific landmark triplets:
- **Push-up:** shoulder → elbow → wrist angle
- **Squat / Lunge / Burpee:** hip → knee → ankle angle
- **Sit-up:** shoulder → hip → knee angle (measured at hip vertex)
- **Plank:** body straightness (shoulder → hip → ankle > 155°)
- **High Knee / Mountain Climber:** knee Y position relative to hip

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Architecture | MVVM + Hilt DI |
| AI / Pose Detection | MediaPipe PoseLandmarker |
| Camera | CameraX |
| Auth | Firebase Authentication |
| Database | Firebase Firestore + Realtime Database |
| Backend Logic | Firebase Cloud Functions |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Animations | Lottie |
| Image Loading | Coil |
| Networking | Retrofit + OkHttp |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- Android device or emulator (API 24+)
- Firebase project with Auth, Firestore, Realtime Database, Cloud Functions, FCM enabled

### Setup

1. **Clone the repo**
```bash
git clone https://github.com/lynxofficial7777-hash/Active-Spark-Gen-Z-.git
cd Active-Spark-Gen-Z-
```

2. **Add Firebase config**
   - Download `google-services.json` from your Firebase Console
   - Place it in `app/google-services.json`

3. **Build & Run**
   - Open in Android Studio
   - Click **Run** or press `Shift+F10`

---

## 📊 Project Stats

- **Screens:** 8 (Splash, Auth, Home, Battle, Leaderboard, Profile, Challenge, Results)
- **Exercise Types:** 10+
- **Firebase Services Used:** 6 (Auth, Firestore, RTDB, Cloud Functions, FCM, App Distribution)
- **Architecture:** MVVM, clean separation of data/ui/navigation layers

---

## 👨‍💻 Developer

**Baranimoorthy** — BSc. Data Science, Sathyabama Institute of Science and Technology

- 🌐 [Portfolio](https://lynxofficial7777-hash.github.io)
- 💼 [LinkedIn](https://www.linkedin.com/in/baranimoorthy77)
- 📧 baranimoorthy77@gmail.com
- 🐙 [GitHub](https://github.com/lynxofficial7777-hash)

---

<p align="center">Built with ❤️ and way too many late nights debugging MediaPipe</p>
