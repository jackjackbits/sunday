# Sunday - Android Version

Android version of [Sunday](https://github.com/jackjackbits/sunday), a comprehensive UV exposure tracking and vitamin D generation app, fully migrated from the original iOS Swift implementation.

## 🌟 Complete Feature Migration Summary

This Android version achieves **100% feature parity** with the original iOS app through systematic three-phase migration:

### 📊 Feature Comparison Table

| Feature | iOS Original | Android Implementation | Status | Implementation Phase |
|---------|-------------|----------------------|---------|---------------------|
| **Core UV Tracking** | ✅ | ✅ | Complete | Base |
| Real-time UV Index | HealthKit + OpenWeather | Open-Meteo API + Room Cache | ✅ Complete | Base |
| Vitamin D Calculation | Custom Swift Algorithm | Kotlin VitaminDCalculator | ✅ Complete | Base |
| Skin Type Selection | 6 Fitzpatrick Types | 6 Fitzpatrick Types | ✅ Complete | Base |
| Clothing Level Adjustment | 4 Levels | 4 Levels (Minimal→Heavy) | ✅ Complete | Base |
| **Health Integration** | ✅ | ✅ | Complete | Base |
| Health Data Sync | HealthKit | Google Fit API | ✅ Complete | Base |
| Session Tracking | Core Data | Room Database | ✅ Complete | Base |
| **Moon Phase System** | ✅ | ✅ | Complete | Phase 1 |
| Moon Phase Display | Manual Calculation | Farmsense API Integration | ✅ Complete | Phase 1 |
| Night Mode Interface | Static Moon Icons | Dynamic Phase Icons + Animation | ✅ Enhanced | Phase 1 |
| Lunar Data Caching | In-Memory | Room Database Cache | ✅ Enhanced | Phase 1 |
| **Solar Timing & Notifications** | ✅ | ✅ | Complete | Phase 2 |
| Solar Noon Calculation | Core Location | SolarCalculator.kt | ✅ Complete | Phase 2 |
| Optimal Sun Notifications | UserNotifications | AlarmManager + NotificationService | ✅ Complete | Phase 2 |
| Location-based Timing | CLLocationManager | LocationManager + Geocoding | ✅ Complete | Phase 2 |
| **Widget System** | ✅ | ✅ | Complete | Phase 2 |
| Home Screen Widget | WidgetKit | Glance for Android | ✅ Complete | Phase 2 |
| UV Index Display | Static Layout | Dynamic UV/Moon Toggle | ✅ Enhanced | Phase 2 |
| Widget Updates | Timeline Provider | Periodic Work Manager | ✅ Complete | Phase 2 |
| Day/Night Widget Modes | Manual Switch | Automatic Solar-based | ✅ Enhanced | Phase 2 |
| **UI/UX Features** | ✅ | ✅ | Complete | Phase 3 |
| Settings Screen | SwiftUI Forms | Jetpack Compose | ✅ Complete | Phase 3 |
| Smooth Animations | SwiftUI Transitions | Compose AnimatedVisibility | ✅ Complete | Phase 3 |
| Material Design | iOS Design System | Material Design 3 | ✅ Platform-optimized | Phase 3 |
| Accessibility Support | VoiceOver | TalkBack + Semantics | ✅ Complete | Phase 3 |
| **Performance & Optimization** | ✅ | ✅ | Enhanced | Phase 3 |
| Memory Management | ARC | Kotlin Coroutines + StateFlow | ✅ Enhanced | Phase 3 |
| Background Processing | BackgroundTasks | WorkManager | ✅ Platform-optimized | Phase 3 |
| Network Monitoring | Network.framework | ConnectivityManager | ✅ Complete | Phase 3 |
| Error Handling | Result Types | Sealed Classes + Exception Handling | ✅ Enhanced | Phase 3 |
| **Development Tools** | ✅ | ✅ | Enhanced | Phase 3 |
| Debug Diagnostics | Basic Logging | DiagnosticService.kt | ✅ Enhanced | Phase 3 |
| Migration System | Core Data Migration | Room Migration + MigrationService | ✅ Enhanced | Phase 3 |

### 🚀 Android-Specific Enhancements

Beyond iOS parity, the Android version includes platform-specific improvements:

- **Enhanced Widget**: Automatic day/night mode switching based on solar calculations
- **Advanced Caching**: Room database with intelligent cleanup and offline support
- **Material Design 3**: Platform-native design language with dynamic theming
- **Notification Channels**: Android-specific notification management
- **Background Optimization**: WorkManager integration for efficient background tasks
- **Permission Management**: Granular Android permission system integration

## 🔧 Technical Architecture

### Core Components
- **MVVM Architecture** with StateFlow for reactive UI updates
- **Room Database** for local data persistence and caching
- **Retrofit + OkHttp** for network operations with automatic retry
- **Jetpack Compose** for modern, declarative UI development
- **Google Fit API** for health data integration
- **WorkManager** for reliable background task execution

### API Integrations
- **Open-Meteo API**: UV index and weather data
- **Farmsense API**: Accurate moon phase calculations
- **Google Fit**: Health data integration
- **Android Location Services**: GPS and network location

### Performance Optimizations
- Intelligent data caching with automatic cleanup
- Coroutine-based async operations
- Memory-efficient state management
- Background task optimization

## 📋 Requirements

### Technical Requirements
- **Android 8.0 (API 26)** or higher
- **Google Play Services** installed and updated
- **Location Services** enabled for accurate UV data
- **Internet Connection** for weather data and API calls

### Permissions
The app requires the following permissions:
- **Location Access**: Precise location for UV index and solar calculations
- **Google Fit**: Health data integration for vitamin D tracking
- **Notifications**: Solar timing alerts and UV warnings
- **Internet**: Weather data and API communication
- **Background Processing**: Widget updates and scheduled notifications

## 🚀 Installation & Setup

### Development Setup
```bash
# Clone the repository
git clone [repository-url]
cd sunday---Android

# Open in Android Studio
# Ensure you have Android Studio Arctic Fox or newer

# Sync Gradle dependencies
./gradlew build

# Run the application
./gradlew assembleDebug
```

### API Configuration
1. **Open-Meteo API**: No API key required (free service)
2. **Farmsense API**: No API key required (free service)
3. **Google Fit**: Enable in Google Cloud Console and add your SHA-1 fingerprint

### Build Variants
- **Debug**: Development build with verbose logging
- **Release**: Production build with optimizations

## 🔧 Project Structure

```
app/src/main/java/com/gmolate/sunday/
├── MainActivity.kt                 # App entry point
├── SundayApplication.kt           # Application class
├── model/                         # Data models and database
│   ├── AppDatabase.kt            # Room database configuration
│   ├── UserPreferences.kt        # User settings entity
│   ├── VitaminDSession.kt        # Session tracking
│   ├── CachedUVData.kt          # UV data caching
│   └── CachedMoonData.kt        # Moon phase caching
├── service/                       # Business logic services
│   ├── UVService.kt              # UV data management
│   ├── VitaminDCalculator.kt     # Vitamin D calculations
│   ├── MoonPhaseService.kt       # Moon phase management
│   ├── SolarCalculator.kt        # Solar timing calculations
│   ├── NotificationService.kt    # Notification management (Original)
│   ├── MigrationService.kt       # Data migration
│   └── DiagnosticService.kt      # Development diagnostics
├── ui/                           # User interface
│   ├── view/
│   │   ├── ContentView.kt        # Main app screen
│   │   └── SettingsView.kt       # Settings screen (Original)
│   └── viewmodel/
│       └── MainViewModel.kt      # State management
└── widget/
    └── SundayWidget.kt           # Home screen widget (Original)
```

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew connectedAndroidTest
```

### Widget Testing
Test the home screen widget functionality by:
1. Long-pressing on home screen
2. Adding "Sunday UV Widget"
3. Verifying UV/moon phase display

## 📱 Features in Detail

### Main Application
- **Real-time UV tracking** with location-based precision
- **Vitamin D calculation** using scientifically-accurate algorithms
- **Smart notifications** for optimal sun exposure timing
- **Comprehensive settings** for personalization
- **Offline mode** with intelligent data caching

### Home Screen Widget
- **Dynamic display** switching between UV index and moon phases
- **Automatic day/night detection** based on solar calculations
- **Error-resistant updates** with graceful fallback states
- **Material Design 3** styling with system theme integration

### Background Services
- **Solar noon notifications** calculated for precise location
- **Automatic data updates** with network monitoring
- **Battery optimization** through intelligent scheduling
- **Migration support** for seamless app updates

## 🤝 Contributing

We welcome contributions to Sunday! Please follow these guidelines to ensure a smooth development process.

### How to Contribute

1.  **Fork the repository** to your own GitHub account.
2.  **Clone the forked repository** to your local machine.
3.  **Create a new branch** for your feature or bug fix: `git checkout -b feature/your-feature-name` or `bugfix/your-bug-fix`.
4.  **Make your changes** and ensure the code follows the project's style guidelines.
5.  **Write or update tests** for your changes.
6.  **Run the tests** to make sure everything is working as expected.
7.  **Commit your changes** with a clear and descriptive commit message.
8.  **Push your branch** to your forked repository.
9.  **Create a pull request** from your forked repository to the main Sunday repository.

### Coding Style

-   Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
-   Use 4 spaces for indentation.
-   Write clear and concise comments where necessary.

### Commit Messages

-   Use the present tense (e.g., "Add feature" not "Added feature").
-   Use the imperative mood (e.g., "Fix bug" not "Fixes bug").
-   Limit the first line to 72 characters or less.
-   Reference issues and pull requests liberally.

## 📄 License

This project is licensed under the [MIT License](LICENSE).

## 🙏 Credits

Based on the original [Sunday](https://github.com/jackjackbits/sunday) iOS project by **jackjackbits**.

### Android Migration
- **Complete iOS feature migration** to Android/Kotlin
- **Platform-specific optimizations** for Android ecosystem
- **Enhanced widget functionality** with day/night modes
- **Modern Android architecture** with Jetpack Compose and Room

### Third-party Services
- **Open-Meteo API**: Weather and UV data
- **Farmsense API**: Moon phase calculations
- **Google Fit**: Health data integration
- **Material Design 3**: UI design system

---

**Note**: This Android version maintains complete feature parity with the original iOS app while providing Android-specific enhancements and optimizations. The migration was completed through a systematic three-phase approach ensuring no functionality was lost in translation.
