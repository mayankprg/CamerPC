# CamerPC

## Overview
CamerPC is an Android application designed to stream the phone's camera feed to a computer connected to the same Wi-Fi network. It follows modern Android development practices, utilizing a clean, layered architecture.

## Features
-   **Camera Streaming:** Streams the camera preview to a specified IP and Port.
-   **Modern UI:** Built with Jetpack Compose and Material 3.
-   **Latest Tech:** Utilizes the latest Android features and CameraX.
-   **Robust Architecture:** Implements the official Android recommended layered architecture (MVVM).

## Architecture
The application is structured into two main layers:

1.  **UI Layer (`com.fumes.camerpc.ui`)**:
    *   **Components:** `MainScreen` (Composable), `MainViewModel`.
    *   **Responsibility:** Displays data on the screen and captures user interactions.
    *   **State Management:** Uses `StateFlow` and `MainUiState` for reactive UI updates.
2.  **Data Layer (`com.fumes.camerpc.data`)**:
    *   **Components:** `NetworkRepository`.
    *   **Responsibility:** Handles business logic and data operations (e.g., retrieving IP addresses).

## Technologies Used
-   **Language:** Kotlin
-   **UI Framework:** Jetpack Compose (Material 3)
-   **Concurrency:** Kotlin Coroutines & Flow
-   **Architecture Pattern:** MVVM (Model-View-ViewModel)
-   **Camera:** CameraX (Preview, Lifecycle binding)
-   **Dependency Injection:** Manual Dependency Injection (ViewModel Factory)
-   **Build System:** Gradle (Kotlin DSL)

## Prerequisites
-   Android 14 (API 34) or higher.
-   A computer connected to the same Wi-Fi network to receive the stream.

## Setup
1.  Open the project in Android Studio.
2.  Sync Gradle files.
3.  Run the application on your Android device.
4.  Grant Camera permissions when prompted.

## Notes
-   Ensure both devices are on the same Wi-Fi network.
-   The app will display the IP address to connect to.
