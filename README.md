<div align="center">
  <img src="overlay_loader_with_app_icon.png" alt="Infifly Recipe Logo" width="150" />

  # Infifly Recipe

  *If you have a recipe that you want the world to know, write it down and let the world tell you it was magic.*
</div>

**Infifly Recipe** is an elegant, natively built Android application designed for culinary enthusiasts to discover, share, and manage recipes. Featuring a clean, simple, and beautiful UI, the app seamlessly blends public recipes with a custom community-driven backend.

## ✨ Key Features

### 🔐 Secure Authentication & Onboarding
* **OTP Verification**: Secure login flow using an email and OTP system.
* **JWT Authorization**: Backend routes are protected via JSON Web Tokens.
* **NDK Security**: API keys and sensitive logic are salted and hidden using C++ NDK (Native Binary Code) to prevent reverse-engineering.

### 💎 Freemium Subscription Model
* **Free Tier**: Users can browse all standard recipes for free, with a limited preview (2 views) of Premium community recipes.
* **Premium Tier**: Unlocks unlimited recipe viewing and grants the ability to upload, edit, and manage personal recipes.

### 🍳 Recipe Discovery & Management
* **Dual-Source Engine**: Asynchronously fetches and merges data from both a public API (MealDB) and the custom backend into a unified feed.
* **Smart Search**: Features a dedicated search overlay with a dynamic "Recent Searches" cache utilizing Material Chips.
* **Sorting & Filtering**: Filter searches by Recipes or Chefs, and sort results (A-Z, Z-A, by Country) using Bottom Sheet dialogs.
* **Personal Cookbook**: Premium users can add, edit, and delete their own recipes with full image upload support.

### 🎨 Premium UI/UX Details
* **Elegant & Simple Design**: Built with a focus on a clean, attractive layout rather than cluttered, ornate designs.
* **Interactive Image Viewer**: Full-screen image viewer featuring a fluid, Spotify-style multi-directional swipe-to-dismiss gesture with dynamic opacity fading.
* **Custom Overlays**: Global custom loading dialogs that prevent user interaction while gracefully awaiting network responses.

## 🛠️ Tech Stack

### Android (Frontend)
* **Language**: Kotlin
* **Architecture**: Single Activity Architecture with Navigation Component
* **Asynchrony**: Coroutines & Flow (`Dispatchers.IO`, `async`/`await`)
* **UI Components**: AndroidX Fragment KTX, ViewBinding, Material3, ViewPager2, SwipeRefreshLayout
* **Networking**: Retrofit2, OkHttp
* **Image Loading**: Glide
* **Local Storage**: SharedPreferences

### Backend & Infrastructure
* **API**: Custom built API deployed on Render
* **Database**: PostgreSQL hosted on Neon Tech
* **Media Storage**: Cloudinary Storage

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug (or newer)
* Minimum SDK 24

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/YourUsername/InfiflyRecipe.git
   ```
2. Open the project in Android Studio.
3. Sync the Gradle files to download dependencies.
4. Build and run on an emulator or physical device.

### Testing Credentials
For testing the authentication and premium flow, the backend is currently configured with the following defaults:
* **Default OTP**: `123456`
* **Premium Upgrade PIN**: `12345`

## 🔮 Future Roadmap
* **Web Domain Integration**: Enable deep linking and recipe sharing outside the app.
* **Social Features**: Introduce likes, comments, and online recipe saving (similar to Instagram's bookmarking).


