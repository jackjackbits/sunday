# Sun Day

UV tracking and vitamin D calculator for iOS.

[📖 Read the detailed methodology](METHODOLOGY.md) | [🔒 Privacy Policy](PRIVACY.md) | [🇪🇸 Leer en español](README.es.md)

<img height="500" alt="SunDay_1290x2796_v2" src="https://github.com/user-attachments/assets/b712cc98-1cc5-4e6f-8297-cabf8f801013" />

## Features

- Real-time UV index from your location
- Vitamin D calculation based on UV, skin type, and clothing
- Moon phase display at night
- Sunrise/sunset times
- Saves to Apple Health
- No API keys required
- Small and medium widgets for your home screen

## Requirements

- iOS 17.0+
- iPhone only
- Xcode 15+

## Setup

1. Clone the repo
2. Run `xcodegen generate` to create the Xcode project
3. Open `Sunday.xcodeproj`
4. Select your development team
5. Build and run

## Usage

1. Allow location and health permissions
2. Press the sun button to start tracking
3. Select your clothing level and skin type
4. The app calculates vitamin D intake automatically

## APIs Used

- Open-Meteo for UV data (free, no key)
- Farmsense for moon phases (free, no key)

## License

Public domain. Use however you want.
