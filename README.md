# ⚡ ACTIVE SPARK GEN 7

> **Futuristic Fitness Battle App for Kids (Ages 8–16)**  
> Built with Kotlin · Jetpack Compose · Firebase · MediaPipe

---

## 🎮 What Is It?
Active Spark Gen 7 is a real-time fitness battle game where kids challenge each other to exercise duels. The AI-powered MediaPipe Pose Detection counts reps automatically and judges form accuracy.

---

## 🏗️ Architecture

```
MVVM + Hilt DI + Clean Architecture
├── ui/
│   ├── theme/          ← Neon dark theme (Colors, Type, Shape, Theme)
│   └── screens/
│       ├── splash/     ← Auth check → route
│       ├── onboarding/ ← Feature walkthrough (4 pages)
│       ├── login/      ← Sign In / Register (tabs)
│       ├── home/       ← Dashboard + quick battle
│       ├── profile/    ← Stats, XP, badges, rank
│       ├── challenge/  ← Exercise detail + rules
│       ├── matchmaking/← Real-time opponent search (Realtime DB)
│       ├── battle/     ← Live battle (MediaPipe + timer + sync)
│       ├── results/    ← Win/Loss/Draw + XP earned
│       ├── leaderboard/← Period-filtered global rankings
│       └── parentdashboard/ ← Child safety controls
├── data/
│   ├── models/         ← User, Match, Challenge, Score, LeaderboardEntry
│   └── repository/     ← FirebaseRepository (single source of truth)
├── navigation/
│   ├── Screen.kt       ← Sealed class route definitions
│   └── AppNavGraph.kt  ← NavHost with animated transitions
├── di/
│   └── FirebaseModule.kt ← Hilt singletons (Auth, Firestore, RTDB, FCM)
└── services/
    └── ActiveSparkMessagingService.kt ← FCM push handler
```

---

## 🎨 Theme Palette

| Token          | Hex       | Usage                    |
|----------------|-----------|--------------------------|
| Background     | `#0A0A0F` | App background           |
| Primary Neon   | `#00F5FF` | Cyan — primary actions   |
| Secondary Neon | `#39FF14` | Lime — wins, health      |
| Accent         | `#FF1B8D` | Pink — player 2, danger  |
| Purple         | `#BF00FF` | Diamond rank             |

---

## 🔥 Firebase Setup (Required!)

1. **Create a Firebase project** at [console.firebase.google.com](https://console.firebase.google.com)
2. **Add Android app** with package: `com.activespark.gen7`
3. **Download `google-services.json`** and replace the placeholder at `app/google-services.json`
4. **Enable**: Authentication (Email/Password), Firestore, Realtime Database, Cloud Messaging
5. **Deploy rules**: `firebase deploy --only firestore:rules,database`

---

## 📦 Key Dependencies

| Library          | Version    | Purpose                    |
|------------------|------------|----------------------------|
| Firebase BOM     | 33.4.0     | Auth, Firestore, RTDB, FCM |
| MediaPipe Vision | 0.10.14    | Pose detection, rep count  |
| Hilt Android     | 2.51.1     | Dependency injection       |
| Compose BOM      | 2024.09.03 | Full Compose UI            |
| Navigation       | 2.8.2      | Screen routing             |~~~~
| Lottie           | 6.5.2      | Exercise animations        |
| Coil             | 2.7.0      | Image loading              |

---

## 🚀 Getting Started

```bash
# 1. Clone / open the project in Android Studio Hedgehog+
# 2. Replace app/google-services.json with real Firebase config
# 3. Build & Run on API 24+ device or emulator
```

---

## 📋 Next Steps (After Base Setup)

- [ ] Integrate CameraX + MediaPipe `PoseLandmarkerHelper` in BattleScreen
- [ ] Add exercise-specific rep counting logic per `ExerciseType`
- [ ] Add Google Sign-In as auth option
- [ ] Implement Lottie exercise demonstration animations
- [ ] Add Orbitron + Exo 2 font TTF files to `res/font/`
- [ ] Implement Cloud Functions for leaderboard aggregation
- [ ] Implement push notification deep links for battle invites
- [ ] Add kid-safe avatar customization screen

---

## 🔒 Safety & Privacy

- All users must be 8+ years old (age field validated)
- Parental consent flag stored on user profile
- Parent Dashboard controls online battles, chat, and screen time
- Firestore rules prevent cross-user data access
- No chat enabled by default

---

*Made with ⚡ by Active Spark Team*
