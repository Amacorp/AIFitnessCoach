# AI Fitness Coach - Android App

A production-ready Android application featuring an AI-powered fitness coach with voice-first onboarding, computer vision form correction, and hybrid intelligence.

## Features

### Phase 1: Voice-First Onboarding (100% Offline)
- Text-to-Speech (TTS) greeting on first launch
- Offline Speech-to-Text (STT) for user input
- No forms or text inputs required
- AI Coach asks: "Welcome! I'm your AI Coach. What is your fitness goal, and do you have any injuries?"

### Phase 2: Intelligent Planning (Online)
- Sends collected voice data to OpenRouter API
- Generates personalized 7-day workout table using Google Gemini Flash 1.5
- Stores workout plan locally

### Phase 3: Task-Based Dashboard
- Modern dark-themed dashboard (Slate & Electric Blue)
- Training table with daily exercises
- Manual or voice-based task completion ("Task complete")
- Progress tracking

### Phase 4: Live Vision Coaching (Hybrid)
- CameraX preview with MediaPipe Pose Landmarker
- Real-time skeletal wireframe overlay (33 landmarks)
- Every 2 seconds: extracts (x,y,z) coordinates and sends to OpenRouter API
- AI analyzes pose and returns specific form corrections
- Rep counting for common exercises (squats, push-ups, lunges)

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Hilt
- **Networking**: Retrofit
- **Camera**: CameraX
- **Computer Vision**: Google MediaPipe Pose Landmarker
- **Security**: EncryptedSharedPreferences for API key storage

## Project Structure

```
com.aicoach.fitness/
├── data/
│   ├── local/          # SecurePreferences, WorkoutPlanDao
│   ├── remote/         # OpenRouterService, NetworkModule
│   └── repository/     # FitnessRepository
├── domain/
│   └── model/          # Data models (WorkoutPlan, PoseData, etc.)
├── ui/
│   ├── components/     # Reusable UI components
│   ├── navigation/     # Navigation setup
│   ├── screens/        # Screen composables
│   ├── theme/          # Colors, Typography, Theme
│   └── viewmodel/      # ViewModels
├── utils/              # VoiceAssistant, PoseAnalyzer
└── di/                 # Dependency injection modules
```

## Setup Instructions

### 1. Prerequisites
- Android Studio Ladybug (2024.2.1) or later
- JDK 17
- Android SDK 35

### 2. Get OpenRouter API Key
1. Visit [openrouter.ai](https://openrouter.ai)
2. Create an account
3. Generate an API key
4. The key should start with `sk-or-`

### 3. Download MediaPipe Model
Download the pose landmarker model and place it in `app/src/main/assets/`:
```bash
# Download from MediaPipe documentation
# File: pose_landmarker_lite.task
# URL: https://developers.google.com/mediapipe/solutions/vision/pose_landmarker
```

### 4. Build and Run
1. Open the project in Android Studio
2. Sync Gradle files
3. Run on a device or emulator with API 26+

## Required Permissions

The app requires the following permissions:
- **Camera**: For pose detection during workouts
- **Record Audio**: For voice commands
- **Internet**: For OpenRouter API calls

## Usage

### First Launch
1. Enter your OpenRouter API key (or skip for now)
2. The AI Coach will greet you via voice
3. Speak your fitness goal and any injuries
4. Your personalized 7-day plan will be generated

### During Workouts
1. Select an exercise from your dashboard
2. Position yourself in front of the camera
3. The AI will analyze your form every 2 seconds
4. Follow the voice feedback for corrections
5. Rep counting happens automatically

### Voice Commands
- "Task complete" - Mark exercise as done
- "Start workout" - Begin recording
- "Stop" - Pause recording

## Configuration

### API Key Management
- API key is stored securely using EncryptedSharedPreferences
- Key can be cleared in Settings
- All AI features are disabled without a valid key

### Theme
- Professional Dark Mode
- Slate and Electric Blue color scheme
- Customizable in `ui/theme/Color.kt`

## Troubleshooting

### Camera not working
- Ensure camera permission is granted
- Try using front camera (default)
- Check if another app is using the camera

### Voice recognition not working
- Ensure microphone permission is granted
- Speak clearly and close to the microphone
- Check internet connection for online recognition

### API errors
- Verify your API key is correct (starts with `sk-or-`)
- Check your OpenRouter account has available credits
- Ensure stable internet connection

## License

MIT License - See LICENSE file for details

## Acknowledgments

- Google MediaPipe for pose detection
- OpenRouter for AI model access
- Jetpack Compose for modern UI
