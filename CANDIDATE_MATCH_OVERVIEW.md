# Candidate Match System

An advanced candidate matching platform that integrates with multiple external services to provide intelligent consultant discovery and project matching capabilities. The system combines traditional structured search with AI-powered semantic search to deliver optimal candidate matching results.

## Functionality

### Core Search Capabilities

**Structured Search (Relational)**
- Name-based consultant filtering
- Skills-based matching with AND/OR logic
- Quality score thresholds
- Active CV filtering
- Paginated results with sorting

**Semantic Search (AI-Powered)**
- Natural language query processing
- Contextual understanding using vector embeddings
- Cosine similarity matching via pgvector
- Combined filtering with quality scores
- Top-K result ranking

**AI Chat Search**
- Multi-mode intelligent routing (STRUCTURED, SEMANTIC, HYBRID, RAG)
- Conversation history management
- Consultant-specific targeting with CV context
- Real-time consultant name detection
- Follow-up question support with context preservation

### Business Features

**Project Request Management**
- PDF document upload and AI analysis
- Automatic extraction of customer requirements
- MUST vs SHOULD requirement categorization
- Project deadline detection
- Coverage status tracking (GREEN, YELLOW, RED, NEUTRAL)

**CV Analysis & Scoring**
- Automated CV quality assessment
- Skills extraction and normalization
- Experience categorization
- Industry tagging and analysis
- Batch processing capabilities

**Consultant Management**
- Flowcase API synchronization
- Normalized CV data structures
- Skills aggregation and statistics
- Active/inactive CV version tracking

## Tech Stack

### Backend Architecture

**Core Framework**
- **Kotlin** - Primary programming language
- **Spring Boot 3** - Application framework
- **Spring MVC** - REST API layer
- **Hibernate 6** - ORM with JSON/JSONB support
- **Liquibase** - Database migration management

**Database & Storage**
- **PostgreSQL 15** - Primary database
- **pgvector extension** - Vector storage for semantic search
- **JSONB** - Flexible CV data storage
- **Hypersistence** - Enhanced JSON mapping

**AI & Search**
- **Google Gemini** - Text generation and embeddings
- **OpenAI** - Alternative AI provider
- **pgvector** - Vector similarity search
- **Cosine distance** - Similarity calculations
- **Text chunking** - Document processing (400 tokens, 50 overlap)

**API & Documentation**
- **OpenAPI 3.0.3** - API specification
- **Springdoc OpenAPI** - Automatic documentation generation
- **Swagger UI** - Interactive API explorer

**Development & Testing**
- **Maven** - Build management
- **Testcontainers** - Integration testing
- **Docker Compose** - Local development environment
- **SDKMAN** - JDK and tool version management

### Multi-Service Architecture

**Candidate Match Service** (Port 8080)
- Core business logic and API endpoints
- Project request management
- CV scoring and analysis

**AI RAG Service** (Port 8081)
- Retrieval-augmented generation
- Embedding generation and storage
- Semantic search operations

**Teknologi Barometer Service** (Port 8082)
- Technology trend analysis
- Gmail integration
- Analytics and insights

## Integrations

### External APIs

**Flowcase Integration**
- **Purpose**: Primary HR system integration
- **Protocol**: HTTP/JSON REST API
- **Functions**: 
  - Consultant data synchronization
  - CV retrieval and normalization
  - Skills extraction
  - User profile management

**Google Gemini AI**
- **Purpose**: AI-powered content generation and embeddings
- **Protocol**: HTTP/JSON REST API
- **Functions**:
  - Natural language processing
  - Text embeddings generation (text-embedding-004, 768 dimensions)
  - Content analysis and summarization
  - Project requirement extraction

**OpenAI (Alternative)**
- **Purpose**: Backup AI provider
- **Protocol**: HTTP/JSON REST API
- **Functions**:
  - Text embeddings (text-embedding-3-small)
  - Chat completions
  - Content analysis

### Frontend Integration

**React Frontend**
- **Location**: `../cloudberries-candidate-match-web`
- **Communication**: REST API with generated TypeScript types
- **Features**:
  - Consultant search interface
  - Project request management
  - Real-time chat with AI
  - Mode selection (compact toolbar)
  - Conversation context persistence

**Type Generation Workflow**
```bash
# After backend OpenAPI changes
cp candidate-match/openapi.yaml ../cloudberries-candidate-match-web/openapi.yaml
npm --prefix ../cloudberries-candidate-match-web run gen:api
```

### Database Architecture

**PostgreSQL Configuration**
- **Version**: PostgreSQL 15
- **Extensions**: pgvector for vector operations
- **Port**: 5433 (local development)
- **Authentication**: Username/password (local), certificate-based (production)

**Data Structure**
- **Consultants**: Core consultant profiles
- **CVs**: JSONB storage for flexible CV data
- **Skills**: Normalized skill entities with relationships
- **Embeddings**: Vector representations stored in pgvector tables
- **Projects**: Customer project requests and analysis results

### Development Environment

**Local Setup**
- **Docker Compose**: PostgreSQL with pgvector pre-installed
- **Environment Variables**: API keys and database credentials
- **Health Checks**: Database, API connectivity, and service status
- **Monitoring**: Actuator endpoints for metrics and health

**API Documentation**
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs.yaml`
- **Health Check**: `http://localhost:8080/actuator/health`

The system provides a comprehensive platform for intelligent candidate matching, combining traditional database queries with modern AI capabilities to deliver precise and contextually relevant results for recruitment and project staffing needs.