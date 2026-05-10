# FitSathi 🥗🏃‍♂️

[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.java.com/)
[![Database](https://img.shields.io/badge/Database-Room-4CAF50.svg)](https://developer.android.com/training/data-storage/room)
[![Security](https://img.shields.io/badge/Security-AES--256-2196F3.svg)](https://developer.android.com/topic/security/data)
[![UI](https://img.shields.io/badge/UI-Material_3-7c4dff.svg)](https://m3.material.io/)

> **"Your health is your greatest wealth. FitSathi is the companion that helps you manage it."**

**FitSathi** (Healthy Companion) is a comprehensive, privacy-focused fitness and nutrition tracking application for Android. It combines high-precision activity monitoring with deep nutritional insights, community engagement, and AI-driven predictive analytics, all backed by a robust, secure offline-first architecture.

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" alt="FitSathi Logo">
</p>

---

## ⚡ The Core Systems

FitSathi is built on six pillars of health and community, each powered by dedicated intelligence modules and persistent data layers.

### 🏃‍♂️ 1. The Kinetic Engine (Activity Tracking)
- **High-Precision Monitoring:** Direct integration with hardware sensors (Step Counter/Detector).
- **Infinite Tracking:** A hardened **Android Foreground Service** ensures steps are logged accurately even under system pressure.
- **Biometric Analytics:** Real-time calculation of Kcal and Distance tailored to user-specific biometrics.
- **Health Connect Sync:** Bidirectional synchronization with the Android Health ecosystem for a unified health profile.

### 🥗 2. The Macro Intelligence (Nutrition)
- **Dual-API Ecosystem:** Synergetic integration with **Nutritionix** and **Open Food Facts**.
- **Vision Intelligence:** Instant nutritional lookup via **Google ML Kit** powered barcode scanning.
- **Deep Logging:** Detailed tracking of Calories, Macros (Carbs, Protein, Fat), and Micros (Fiber, Sugar).
- **Historical Insights:** Navigate through daily logs to visualize long-term nutritional trends.

### 🏋️‍♂️ 3. The Adaptive Coach (Workouts)
- **Personalized Evolution:** Dynamic workout generation based on fitness goals (Weight Loss, Muscle Gain, Maintenance).
- **Contextual Awareness:** Smart filtering for location (Home/Gym) and intensity levels.
- **Visual Library:** 150+ exercises with high-fidelity visual guides and precise instruction sets.

### 📈 4. Predictive Analytics (Weight Projection)
- **AI-Driven Forecasting:** Advanced mathematical models to project weight trends based on current activity and caloric intake.
- **Goal Estimation:** Real-time feedback on when you'll reach your target weight based on current performance.
- **Trend Visualization:** Interactive charts showing projected vs. actual weight progress.

### 👥 5. Social Squads & Community
- **Global Leaderboards:** Compete with friends and the community in real-time step challenges.
- **Squad Engagement:** Join or create squads to stay motivated and track collective progress.
- **Real-time Syncing:** Powered by Firebase Realtime Database for instantaneous leaderboard updates.

### 📱 6. Visual Progress Sharing
- **Progress Cards:** Generate stunning, Material 3 styled infographics of your weekly progress.
- **Social Integration:** One-tap sharing to Instagram, WhatsApp, and other social platforms.
- **Customizable Insights:** Highlight your best streaks, total steps, and nutritional milestones.

---

## 🏗️ Technical Architecture

FitSathi utilizes a modern, modular architecture that prioritizes data integrity and system responsiveness.

```mermaid
graph TD
    UI["📱 Material 3 UI"] --> VM["⚙️ Intelligence Managers"]
    VM --> Room["💾 Room Database (Local)"]
    VM --> HC["🏥 Health Connect"]
    VM --> Secure["🔐 EncryptedSharedPreferences"]
    
    Sync["🔄 SyncWorker"] --> Room
    Sync --> Cloud["☁️ Firebase Realtime DB"]
    
    Service["🏃 Foreground Step Service"] --> Hardware["🔌 Device Sensors"]
    Hardware --> Service
    Service --> VM
    
    API["🌐 External APIs (Nutritionix/OFF)"] --> VM
    
    style Room fill:#4CAF50,stroke:#333,stroke-width:2px,color:#fff
    style Secure fill:#2196F3,stroke:#333,stroke-width:2px,color:#fff
    style Cloud fill:#ffca28,stroke:#333,stroke-width:2px
    style HC fill:#e91e63,stroke:#333,stroke-width:2px,color:#fff
```

---

## 🛡️ Security & Privacy (The "Hardened" Standard)

Data privacy is the foundation of FitSathi. The application implements industry-standard security protocols:

- **AES-256 Encryption:** All sensitive user preferences and biometrics are stored using **Jetpack Security (EncryptedSharedPreferences)**, backed by hardware-level keystores.
- **ACID Compliance:** Historical logs are managed via **Room Database** to ensure data integrity even during crashes or power loss.
- **Background Synchronization:** **SyncWorker** handles secure, periodic synchronization between local Room storage and Firebase, ensuring your data is always backed up and consistent.
- **Credential Masking:** API keys are injected via `local.properties` and `BuildConfig`, never exposed in the source code.

---

## 🌍 Localization

FitSathi supports a global user base with seamless language switching:
- 🇺🇸 **English** (Standard)
- 🇮🇳 **Hindi** (Regional)
- 🇪🇸 **Spanish** (International)
- 🇫🇷 **French** (International)

---

## 🛠️ Tech Stack

- **Language:** Java (Modern SDK 34+)
- **Database:** Room (SQLite)
- **Health Integration:** Google Health Connect
- **Security:** Jetpack Security (Crypto)
- **Cloud:** Firebase (Auth, Realtime DB)
- **Networking:** OkHttp, Volley
- **UI/UX:** Material 3, MPAndroidChart, Glide, Konfetti
- **Vision:** Google ML Kit
- **Core:** Android Foreground Services, WorkManager (SyncWorker), AlarmManager

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (or newer)
- Nutritionix API Credentials
- Firebase `google-services.json`
- Health Connect API setup

### Setup
1. **Clone:** `git clone https://github.com/pranavbairollu/FitSathi.git`
2. **Configure Security:** Add `nutritionix.app.id` and `nutritionix.app.key` to `local.properties`.
3. **Add Firebase:** Place `google-services.json` in the `app/` folder.
4. **Deploy:** Sync Gradle and run on a physical device for full sensor and Health Connect support.

---

<p align="center">
  <b>Developed by Pranav Bairollu</b><br>
  <i>"FitSathi: Your Health, Hardened."</i><br>
  <a href="mailto:pranavbairollu@gmail.com">Contact Developer</a>
</p>
