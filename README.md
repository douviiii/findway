# 🗺️ FindWay - Real-Time Navigation App (Android + Jetpack Compose)

**FindWay** is a real-time navigation app built using **Jetpack Compose**, **Google Maps SDK**, and **MVVM (Model-View-ViewModel)** architecture. The app allows users to:

- Search for locations using autocomplete
- Select any point on the map
- View address details in a bottom sheet
- Tap a button to fetch route directions
- See a live route updated in real-time as the user moves

This project is built to demonstrate modern **Android development practices** with a focus on **clean architecture**, **reactive state management** using `StateFlow`, and **location-based services**.

---

## 🚀 Features

- 🔍 **Search Places** using Google Places API with autocomplete
- 🗺️ **Google Map integration** with dynamic marker rendering
- 📍 **Tap-to-select location** with reverse geocoding
- 📦 **Bottom info box** showing address and coordinates
- ➡️ **"Go There" navigation** to fetch route from current location
- 📍 **Origin (red) & Destination markers** on the map
- 🧭 **Polyline drawing** for route from Directions API
- 📡 **Real-time tracking** of user movement and rerouting
- ⚙️ Clean separation of concerns with **MVVM + ViewModel**

---

## 🛠️ Tech Stack

| Tech                          | Description                                                                 |
|-------------------------------|-----------------------------------------------------------------------------|
| **Kotlin**                    | Primary programming language                                                |
| **Jetpack Compose**           | Modern declarative UI framework                                             |
| **Google Maps SDK**           | Display and interact with the map                                           |
| **Google Places API**         | Search and autocomplete functionality                                       |
| **Google Directions API**     | Fetch route data between origin and destination                             |
| **FusedLocationProviderClient** | Real-time user location updates                                           |
| **StateFlow** (Kotlin Flow)   | Reactive state management inside ViewModel                                  |
| **MVVM Architecture**         | Clear separation of logic, UI, and data                                     |
| **Geocoder** (Android SDK)    | Reverse geocoding from coordinates to human-readable address                |
| **Permissions API**           | Request and manage location permissions                                     |

---

## 📱 Screenshots

| Search & Select            | Bottom Sheet Info           | Route & Tracking           |
|----------------------------|------------------------------|-----------------------------|
| <img width="1344" height="1302" alt="Screenshot_20250725-081851" src="https://github.com/user-attachments/assets/80c35004-88d4-4072-a91a-f270c2063b09" />| <img width="604" height="262" alt="image" src="https://github.com/user-attachments/assets/827817e8-95f0-4340-9faa-1b42449ade57" /> | <img width="1344" height="1293" alt="Screenshot_20250725-082029" src="https://github.com/user-attachments/assets/d552641c-efc4-4716-8a8f-f8b0943cf6c5" />

---

## 🧩 Architecture Overview

- **UI Layer** (`Mapping.kt`): 
  - Composable map screen with GoogleMap composable
  - Responds to state from ViewModel
  - Displays markers, polylines, and search/autocomplete

- **ViewModel (`MapViewModel.kt`)**:
  - Holds app state: current location, destination, route
  - Triggers real-time location updates
  - Fetches suggestions, directions, and decodes polylines

- **State Management**:
  - `StateFlow` is used for observable states like:
    - `currentLocation`
    - `selectedLocation`
    - `routePoints`
    - `suggestions`

- **Location & Network Layer**:
  - Uses FusedLocationProvider for location tracking
  - Google Directions API for routing
  - Places API for search autocomplete

---

## 🔧 Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/yourusername/findway.git
Video demo: https://drive.google.com/file/d/1DmuJWQQ_XSZkPHA23dDJnx6wL5FVdcx5/view?usp=sharing
