# MedAssist - Medical Recognition Full-Stack Application

A comprehensive medical recognition app that combines AI-powered medicine identification with pharmacy location services, medication reminders, and health tracking.

## Project Structure

```
MedAssist-FullStack/
├── Medassistandroid/           # Android app (Kotlin + Jetpack Compose)
├── medassist-backend/          # Spring Boot REST API (Java)
├── docs/                       # Project documentation
└── README.md                   # This file
```

## Technology Stack

### Frontend (Android)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture (MVVM)
- **Dependencies**: Retrofit, Room, Hilt, CameraX, ML Kit, Lottie, DataStore, WorkManager

### Backend (Spring Boot)
- **Language**: Java
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL
- **Security**: Spring Security with JWT
- **Documentation**: Swagger/OpenAPI

### AI Integration
- **Primary AI**: Claude API for medical information
- **Image Recognition**: ML Kit + Custom vision models
- **Natural Language Processing**: Claude for symptom analysis

## Development Setup

### Prerequisites
- Android Studio (Latest version)
- IntelliJ IDEA (for Spring Boot development)
- Java 17+
- PostgreSQL
- Git

### Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/Yashvvvv/MedAssist-FullStack.git
   cd MedAssist-FullStack
   ```

2. **Backend Setup**
   - Open `medassist-backend` in IntelliJ IDEA
   - Configure PostgreSQL database
   - Run the Spring Boot application

3. **Android Setup**
   - Open `Medassistandroid` in Android Studio
   - Update API base URL in configuration
   - Build and run the Android app

## Features

### Core Features
- 📸 **Medicine Recognition**: Camera-based medicine identification using AI
- 🔍 **Smart Search**: Search by name, symptoms, or description
- 💊 **Comprehensive Info**: Dosage, side effects, interactions, manufacturing details
- 📍 **Pharmacy Locator**: Find nearby pharmacies with map view
- 🤖 **AI Assistant**: Claude-powered medical guidance

### Medication Management
- ⏰ **Medication Reminders**: Schedule and manage medicine reminders
  - Daily, weekly, and custom reminder frequencies
  - Toggle reminders on/off
  - Time picker for precise scheduling
- 📋 **Medicine History**: Track medicine intake over time
  - Today/All history views
  - Statistics (taken, skipped, missed counts)
  - Manual intake logging with mood tracking
- ⚠️ **Drug Interactions**: Check for medicine interactions and safety

### User Features
- 👤 **User Profile**: Edit profile and manage account
- 🔐 **Authentication**: Secure login, registration, password reset
- 🔑 **Change Password**: Update password from settings
- ⚙️ **Settings**: Theme, notifications, language preferences
  - Light/Dark/System theme
  - Notification sound and vibration controls
  - Multi-language support

### Advanced Features
- 📱 **Modern UI/UX**: Material 3 design with smooth animations
- 💾 **Offline Support**: Local database with Room
- 🔄 **Data Sync**: DataStore for preferences persistence
- 📊 **Health Dashboard**: Personal medication management

## Screenshots

| Home | Medicine Search | Reminders |
|------|-----------------|-----------|
| Quick actions grid | AI-powered search | Medication scheduling |

| History | Settings | Drug Interactions |
|---------|----------|-------------------|
| Intake tracking | App preferences | Safety checker |

## API Documentation

Once the backend is running, visit:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/api-docs`

### API Endpoints

#### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/forgot-password` - Password reset
- `POST /api/auth/change-password` - Change password

#### Medicines
- `GET /api/medicines/search` - Search medicines
- `GET /api/medicines/{id}` - Get medicine details
- `POST /api/medicines/analyze` - AI analysis of medicine
- `POST /api/medicines/analyze-interactions` - Check drug interactions

#### Pharmacies
- `GET /api/pharmacies/nearby` - Find nearby pharmacies
- `GET /api/pharmacies/{id}` - Get pharmacy details

#### User
- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Roadmap

- [ ] Push notifications for reminders
- [ ] Health data export (PDF/CSV)
- [ ] Integration with wearables
- [ ] Telemedicine support
- [ ] Prescription scanning
- [ ] Family medication management

## Contact

For questions or support, please open an issue on GitHub.

---

**Built with ❤️ for better healthcare accessibility**