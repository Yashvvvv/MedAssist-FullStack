# MedAssist AI Service Layer Documentation

## Overview
The AI service layer integrates Google's Gemini API to provide intelligent medicine identification and information retrieval capabilities for the MedAssist application.

## Features Implemented

### 🤖 Core AI Services
- **Text-based Medicine Analysis**: Query medicines using natural language
- **Image Processing & OCR**: Extract text from medicine packaging images
- **Combined Analysis**: Use both text queries and images for enhanced accuracy
- **Drug Interaction Analysis**: Check for potential drug interactions
- **Intelligent Caching**: Reduce API costs and improve performance

### 🏗️ Architecture Components

#### DTOs (Data Transfer Objects)
- `GeminiRequest.java` - Request structure for Gemini API
- `GeminiResponse.java` - Response structure from Gemini API
- `MedicineAnalysisRequest.java` - Medicine analysis request with support for text/image/combined
- `MedicineAnalysisResponse.java` - Comprehensive medicine information response

#### Configuration
- `GeminiConfig.java` - WebClient and caching configuration
- Enhanced `application.properties` with AI service settings

#### Services
- `GeminiAIService.java` - Core Gemini API communication
- `MedicineAIService.java` - High-level medicine analysis orchestration
- `PromptTemplateService.java` - Structured prompts for consistent AI responses
- `ImageProcessingService.java` - Image enhancement and optimization
- `DataInitializationService.java` - Sample data loading

#### Controllers
- `MedicineAIController.java` - REST API endpoints for AI functionality
- `MedicineController.java` - CRUD operations for medicines
- `PharmacyController.java` - CRUD operations for pharmacies

#### Exception Handling
- `AIServiceExceptionHandler.java` - Comprehensive error handling

## API Endpoints

### Medicine AI Analysis
```http
POST /api/ai/medicine/analyze/text
Parameters: query (string)

POST /api/ai/medicine/analyze/image
Parameters: image (MultipartFile)

POST /api/ai/medicine/analyze/combined
Parameters: query (string), image (MultipartFile)

POST /api/ai/medicine/analyze/interactions
Parameters: primaryMedicine (string), otherMedicines (List<string>)

GET /api/ai/medicine/health
GET /api/ai/medicine/supported-formats
```

### Medicine Database
```http
GET /api/medicines
GET /api/medicines/search?q={term}
GET /api/medicines/search/name?name={name}
GET /api/medicines/search/manufacturer?manufacturer={name}
POST /api/medicines
PUT /api/medicines/{id}
DELETE /api/medicines/{id}
```

### Pharmacy Database
```http
GET /api/pharmacies
GET /api/pharmacies/search?q={term}
GET /api/pharmacies/nearby?latitude={lat}&longitude={lon}&radius={km}
GET /api/pharmacies/24hours
POST /api/pharmacies
PUT /api/pharmacies/{id}
```

## Configuration Required

### Environment Variables
```bash
GEMINI_API_KEY=your_actual_gemini_api_key_here
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

### Database Setup
The application uses PostgreSQL. Update connection details in `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/medassist_auth
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Key Features

### 🎯 Smart Medicine Identification
- **Multi-modal Analysis**: Combines text queries with image recognition
- **Local Database Integration**: Checks existing medicine database first
- **Confidence Scoring**: Provides reliability metrics for AI responses
- **Fallback Mechanisms**: Graceful degradation when AI services fail

### 🖼️ Advanced Image Processing
- **Image Enhancement**: Contrast and noise reduction for better OCR
- **Format Support**: JPEG, PNG, WebP images up to 10MB
- **Text Extraction**: Optimized for medicine packaging text recognition
- **Automatic Resizing**: Maintains quality while reducing processing time

### ⚡ Performance Optimization
- **Intelligent Caching**: 24-hour cache for frequent queries
- **Async Processing**: Non-blocking API calls
- **Retry Logic**: Automatic retry for transient failures
- **Rate Limiting**: Built-in protection against API abuse

### 🔒 Error Handling & Safety
- **Comprehensive Exception Handling**: Specific error codes and messages
- **Input Validation**: File size, format, and content validation
- **Safety Settings**: Content filtering for harmful responses
- **Timeout Protection**: Prevents hanging requests

## Usage Examples

### Text Analysis
```bash
curl -X POST "http://localhost:8080/api/ai/medicine/analyze/text" \
  -d "query=What is Paracetamol used for?"
```

### Image Analysis
```bash
curl -X POST "http://localhost:8080/api/ai/medicine/analyze/image" \
  -F "image=@medicine_photo.jpg"
```

### Combined Analysis
```bash
curl -X POST "http://localhost:8080/api/ai/medicine/analyze/combined" \
  -F "image=@medicine_photo.jpg" \
  -d "query=Is this safe during pregnancy?"
```

## Response Structure
```json
{
  "medicine_name": "Paracetamol",
  "generic_name": "Acetaminophen",
  "brand_names": ["Tylenol", "Panadol"],
  "active_ingredients": ["Acetaminophen"],
  "strength": "500mg",
  "form": "Tablet",
  "manufacturer": "Generic Pharma",
  "description": "Pain reliever and fever reducer",
  "dosage_information": {
    "adult_dosage": "500-1000mg every 4-6 hours",
    "maximum_daily_dose": "4000mg"
  },
  "side_effects": ["Nausea", "Stomach upset"],
  "requires_prescription": false,
  "confidence_score": 0.95,
  "analysis_source": "COMBINED"
}
```

## Deployment Notes

### Prerequisites
1. Java 21+
2. PostgreSQL database
3. Valid Gemini API key
4. Maven 3.6+

### Run Application
```bash
cd medassist-backend/medassist-backend
mvn spring-boot:run
```

### Production Considerations
- Set up proper database credentials
- Configure email service for notifications
- Set up monitoring for AI service usage
- Implement API rate limiting
- Configure SSL/TLS for secure communication

## Sample Data
The application automatically loads sample data including:
- 5 common medicines (Paracetamol, Ibuprofen, Amoxicillin, Metformin, Lisinopril)
- 4 pharmacy locations in New York

## Next Steps
1. Set up your Gemini API key
2. Configure database connection
3. Start the application
4. Test with the provided sample data
5. Begin integrating with your Android application

The AI service layer is now fully implemented and ready for integration with your Android frontend!
