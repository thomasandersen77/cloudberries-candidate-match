# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Cloudberries Candidate Matcher is a Spring Boot application written in Kotlin that matches candidates to projects using
AI. It integrates with Flowcase API for candidate data and uses OpenAI/Gemini for intelligent matching and scoring of
candidates against project requirements.

## Build System & Dependencies

- **Language**: Kotlin 2.2.0 with Java 21
- **Framework**: Spring Boot 3.3.3 with Maven
- **Database**: PostgreSQL with Liquibase migrations
- **AI Integration**: OpenAI and Google Gemini APIs
- **External APIs**: Flowcase for candidate management

## Development Commands

### Local Development

```bash
# Build the project
mvn clean compile

# Run all tests
mvn test

# Run integration tests
mvn verify

# Package the application
mvn package

# Run the application locally
mvn spring-boot:run
```

### Docker Development

```bash
# Start PostgreSQL and the application
docker-compose up

# Start only PostgreSQL for local development
docker-compose up postgres

# Rebuild and start
docker-compose up --build

# Run tests with test database
docker-compose -f docker-compose-test.yml up
```

### Testing

```bash
# Run specific test
mvn test -Dtest=CandidateMatchingServiceTest

# Run integration tests only
mvn failsafe:integration-test

# Run all tests with coverage
mvn verify
```

## Architecture Overview

The application follows a layered architecture:

### Controllers Layer (`/controllers`)

- **MatchingController**: Main API endpoint for candidate matching (`/api/matches`)
- **CvScoreController**: CV scoring endpoints (`/api/score`)
- **AIController**: AI chat functionality
- **HealthController**: Health check endpoints

### Service Layer (`/service`)

- **CandidateMatchingService**: Core matching logic using AI providers
- **ScoreCandidateService**: CV scoring functionality
- **AIContentAnalysisService**: AI content analysis wrapper
- **NotificationScheduler**: Scheduled notifications

### Domain Layer (`/domain`)

- **candidate/**: Candidate domain objects and matching logic
- **consultant/**: Consultant-related domain models
- **ai/**: AI provider abstractions (AIProvider enum)
- **event/**: Domain events for consultant matching

### Infrastructure Layer (`/infrastructure`)

- **integration/**: External API clients (Flowcase, OpenAI, Gemini)
- **repositories/**: JPA repositories for data persistence
- **adapters/**: Domain-to-infrastructure adapters
- **entities/**: JPA entity classes

### Key Integration Points

1. **Flowcase Integration**: Syncs consultant data from external Flowcase API
2. **AI Providers**: Supports both OpenAI and Gemini for candidate matching
3. **PostgreSQL**: Persistent storage with Liquibase schema management
4. **Health Monitoring**: Custom health indicators for dependencies

## Database Setup

The application requires PostgreSQL with the schema `candidatematch`. Database migrations are handled by Liquibase.

### Environment Variables

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/candidatematch
SPRING_DATASOURCE_USERNAME=candidatematch
SPRING_DATASOURCE_PASSWORD=candidatematch123
FLOWCASE_API_KEY=your_api_key
OPENAI_API_KEY=your_api_key
GEMINI_API_KEY=your_api_key
```

## Testing Strategy

- **Unit Tests**: Use MockK for mocking Kotlin classes
- **Integration Tests**: Use embedded PostgreSQL (Zonky)
- **AI Testing**: WireMock for external API testing
- **Test Profiles**: Separate configuration for test environments

## Main Application Flow

1. **Candidate Data Sync**: SyncConsultantService pulls consultant data from Flowcase
2. **Match Request**: Client sends project requirements to `/api/matches`
3. **AI Analysis**: CandidateMatchingService uses AI providers to score candidates
4. **Response**: Returns ranked list of candidate matches with scores and summaries
5. **Event Publishing**: Domain events are published for successful matches

## Development Notes

- Main class: `no.cloudberries.candidatematch.MainKt`
- Uses Spring Boot with `@EnableScheduling` for background tasks
- Kotlin coroutines for async processing
- Jackson for JSON processing
- PDF parsing capabilities for CV analysis
- Rate limiting with Bucket4j
- Comprehensive logging with kotlin-logging

## CI/CD

GitHub Actions workflow includes:

- Maven build and test
- Gemini AI code review on pull requests
- Quality checks with Qodana
- Automated deployment to Azure Container Apps
