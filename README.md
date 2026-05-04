# FitSathi 🥗🏃‍♂️

**FitSathi** is a comprehensive, privacy-focused fitness and nutrition tracking application for Android. It combines real-time activity monitoring with deep nutritional insights and personalized workout plans to help users achieve their health goals in a simple, intuitive way.

![FitSathi Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

## ✨ Features

### 🏃‍♂️ Activity & Step Tracking
*   **Real-time Step Counter:** Hardware-integrated tracking using device sensors via a dedicated foreground service.
*   **Live Metrics:** Instant calculation of calories burned (Kcal) and distance covered (km).
*   **Goal System:** Customizable daily step goals with an interactive circular progress dashboard.
*   **Achievement Celebrations:** Visual confetti animations upon reaching daily milestones.

### 🥗 Nutrition & Calorie Intelligence
*   **Global Food Search:** Integrated with **Nutritionix** and **Open Food Facts** APIs.
*   **Detailed Macros:** Track Calories, Carbs, Protein, Fat, Fiber, and Sugar for every meal.
*   **Categorized Logging:** Log Breakfast, Lunch, Dinner, and Snacks separately.
*   **Smart Hydration:** Dedicated water intake tracker with daily hydration goals.

### 🏋️‍♂️ Personalized Workout Engine
*   **Adaptive Plans:** Dynamically generated daily workouts based on user goals (Weight Loss, Muscle Gain, etc.).
*   **Context-Aware:** Exercises filtered by location (Home/Gym) and intensity level (Beginner to Advanced).
*   **Guided Movements:** Visual exercise guides with calculated sets, reps, and durations.

### 📊 Analytics & Progress
*   **Visual Trends:** Weekly bar charts for steps and calorie intake.
*   **Weight Tracker:** Monitor weight changes over time with graphical progress views.
*   **History Logs:** Comprehensive logs for all historical activity and nutrition data.

## 🛠️ Tech Stack
*   **Language:** Java / Kotlin
*   **Database:** Firebase Realtime Database
*   **Authentication:** Firebase Auth
*   **APIs:** Nutritionix API, Open Food Facts API
*   **UI Components:** Material Design, MPAndroidChart, Konfetti, CircularProgressBar
*   **Services:** Android Foreground Services, WorkManager

## 🔒 Security & Privacy
*   **Credential Masking:** API keys are secured via `local.properties` and injected through `BuildConfig`.
*   **Sensitive Data Protection:** `google-services.json` and other configuration files are strictly excluded via `.gitignore`.
*   **Local Storage:** Uses `SharedPreferences` for fast, local-first data persistence.

## 🚀 Getting Started

### Prerequisites
*   Android Studio Hedgehog or newer.
*   A Nutritionix API ID and Key.
*   A Firebase project.

### Setup
1.  Clone the repository:
    ```bash
    git clone https://github.com/pranavbairollu/FitSathi.git
    ```
2.  Add your Nutritionix credentials to `local.properties`:
    ```properties
    nutritionix.app.id=YOUR_APP_ID
    nutritionix.app.key=YOUR_APP_KEY
    ```
3.  Place your `google-services.json` file in the `app/` directory.
4.  Sync with Gradle and Run!

---
Developed by [Pranav Bairollu](mailto:pranavbairollu@gmail.com)
