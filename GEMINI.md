# CamerPC

## Overview
CamerPC is an Android application designed to stream the phone's camera feed to a computer connected to the same Wi-Fi network. It follows modern Android development practices, utilizing a clean, layered architecture.

## Features
-   **Camera Streaming:** Streams the camera preview to a specified IP and Port.
-   **Video Encoding:** Encodes raw camera frames into H.264 (AVC) format using hardware acceleration.
-   **Background Streaming:** Offloads network writes to background coroutines using conflated Kotlin Channels to prevent UI freezes and ensure low-latency delivery.
-   **Stable Connections:** Implements Keyframe-first logic, ensuring clients receive a full picture immediately upon connection.
-   **On-Demand Keyframes:** Automatically requests a hardware sync frame when a new client connects for instant playback.
-   **Camera Selection:** Allows users to switch between available camera modules (Front/Back) and select specific zoom levels (0.6x, 1.0x, 3.0x).
-   **Modern UI:** Built with Jetpack Compose and Material 3.
-   **Latest Tech:** Utilizes the latest Android features and CameraX.
-   **Robust Architecture:** Implements the official Android recommended layered architecture (MVVM).

## Architecture
The application is structured into two main layers:

1.  **UI Layer (`com.fumes.camerpc.ui`)**:
    *   **Components:** `MainScreen` (Composable), `MainViewModel`.
    *   **Responsibility:** Displays data on the screen, handles camera selection, and captures user interactions.
    *   **State Management:** Uses `StateFlow` and `MainUiState` for reactive UI updates.
2.  **Data Layer (`com.fumes.camerpc.data`)**:
    *   **Components:** 
        *   `NetworkRepository`: Handles network address retrieval.
        *   `CameraRepository`: Fetches and organizes available camera capabilities.
        *   `VideoEncoder`: Manages `MediaCodec` to encode video directly from a Surface input into H.264.
    *   **Responsibility:** Handles business logic, data operations, and heavy media processing.

## Technologies Used
-   **Language:** Kotlin
-   **UI Framework:** Jetpack Compose (Material 3)
-   **Concurrency:** Kotlin Coroutines & Flow
-   **Architecture Pattern:** MVVM (Model-View-ViewModel)
-   **Camera:** CameraX (Preview, Lifecycle binding)
-   **Media:** MediaCodec (H.264 Encoding)
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
-   The app will display the IP address and Port (5000) to connect to.
-   Streaming encodes video data and transmits it over TCP. Use a player like VLC (using `tcp://<ip>:5000`) or a custom script to view the stream.
