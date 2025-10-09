# Azure Deployment Guide - Backend

This document explains how to deploy the candidate-match Spring Boot backend to Azure.

## Prerequisites

- Java 21 (via SDKMAN): `sdk use java 21.0.7-tem`
- Maven 3.9.9 (via SDKMAN): `sdk use maven 3.9.9`
- Docker installed locally
- Azure CLI installed and logged in
- An Azure subscription

## Local Development

### Team Rules
- **Always use Java SDK 21.0.7-tem** to avoid bytebuddy version conflicts
- **For local development**: Use username/password database authentication only (no certificates)

### Build and Run Locally

```bash
# Use correct Java version
sdk use java 21.0.7-tem
sdk use maven 3.9.9

# Build the application
mvn clean package -DskipTests

# Build Docker image
docker build -t candidate-match-backend:local .

# Run locally
docker run --rm -p 8080:8080 candidate-match-backend:local
```

The application will be available at http://localhost:8080

Health check: http://localhost:8080/actuator/health

## Running Locally with Azure Database (IntelliJ backend + Terminal frontend)

This section uses the configuration values in your local .env.azure file without printing any secrets here. Replace placeholders with your real values from that file.

- Azure PostgreSQL host: cloudberries-candidate-match-pgdb.postgres.database.azure.com
- Azure Container App (backend) URL: https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io/
- Azure Static Web App (frontend) URL: https://delightful-meadow-056d48003.1.azurestaticapps.net

1) Backend: Run locally in IntelliJ using Azure DB
- In IntelliJ, open Run/Debug Configurations for the Spring Boot app.
- Set Active profiles: local
- Add Environment variables (use your .env.azure values):
  - DB_URL=jdbc:postgresql://cloudberries-candidate-match-pgdb.postgres.database.azure.com:5432/candidatematch?sslmode=require
  - DB_USERNAME={{DB_USERNAME_FROM_ENV}}
  - DB_PASSWORD={{DB_PASSWORD_FROM_ENV}}
- Make sure Java version is 21.0.7-tem (SDKMAN): sdk use java 21.0.7-tem
- Start the app. It will run at http://localhost:8080 and connect to Azure PostgreSQL over SSL.

2) Frontend: Run locally in terminal against the local backend
- In another terminal:
  - cd ../cloudberries-candidate-match-web
  - export VITE_API_BASE=http://localhost:8080
  - npm install (first time)
  - npm run dev
- The frontend will call http://localhost:8080/auth/login, /consultants, /chatbot, etc. (no /api prefix in the backend). For development we point directly at the locally running backend to avoid CORS.

3) Verifying locally
- Health: curl http://localhost:8080/health
- Login: curl -X POST http://localhost:8080/auth/demo
- Example search: curl -X POST http://localhost:8080/chatbot/search -H 'Content-Type: application/json' -d '{"text":"java dev"}'

Notes
- Do not commit secrets. Use environment variables or your local secret manager.
- For 405 issues during local dev: ensure the POST path matches the backend mapping (e.g., /auth/login) and that IntelliJ profile is "local".

## Azure Deployment Options

### Option A: Azure Container Apps (Recommended)

Azure Container Apps (ACA) provides serverless containers with automatic scaling.

```bash
# Set variables
RG=rg-candidate-match
LOC=westeurope
ACA_ENV=candidate-match-env
APP=candidate-match-backend
ACR_NAME=candidateregistry$RANDOM
IMAGE=${ACR_NAME}.azurecr.io/candidate-match-backend:$(date +%Y%m%d%H%M%S)

# Create resources
az group create -n $RG -l $LOC
az acr create -n $ACR_NAME -g $RG --sku Basic --admin-enabled true
az acr login -n $ACR_NAME

# Build and push image
docker tag candidate-match-backend:local $IMAGE
docker push $IMAGE

# Create Container Apps environment
az containerapp env create -n $ACA_ENV -g $RG -l $LOC

# Deploy the container app with external ingress
az containerapp create -n $APP -g $RG \
  --image $IMAGE \
  --environment $ACA_ENV \
  --ingress external \
  --target-port 8080 \
  --cpu 0.5 \
  --memory 1.0Gi \
  --min-replicas 1 \
  --max-replicas 3

# Get the public URL for the frontend configuration
BACKEND_URL=$(az containerapp show -n $APP -g $RG --query properties.configuration.ingress.fqdn -o tsv)
echo "Backend URL for frontend: https://$BACKEND_URL"
```

### Option B: Azure App Service (Linux Container)

Alternative deployment using App Service with container support.

```bash
# Set variables
RG=rg-candidate-match
LOC=westeurope
APP_SERVICE_PLAN=candidate-match-plan
WEB_APP=candidate-match-backend-app
ACR_NAME=candidateregistry$RANDOM

# Create resources
az group create -n $RG -l $LOC
az acr create -n $ACR_NAME -g $RG --sku Basic --admin-enabled true
az acr login -n $ACR_NAME

# Build and push
docker build -t $ACR_NAME.azurecr.io/candidate-match-backend:latest .
docker push $ACR_NAME.azurecr.io/candidate-match-backend:latest

# Create App Service Plan (Linux)
az appservice plan create -n $APP_SERVICE_PLAN -g $RG -l $LOC --is-linux --sku B1

# Create Web App with container
az webapp create -n $WEB_APP -g $RG -p $APP_SERVICE_PLAN \
  --deployment-container-image-name $ACR_NAME.azurecr.io/candidate-match-backend:latest

# Configure container registry credentials
az webapp config container set -n $WEB_APP -g $RG \
  --docker-custom-image-name $ACR_NAME.azurecr.io/candidate-match-backend:latest \
  --docker-registry-server-url https://$ACR_NAME.azurecr.io

# Get the URL
az webapp show -n $WEB_APP -g $RG --query defaultHostName -o tsv
```

## Environment Configuration

### Application Profiles

The backend supports multiple Spring profiles:
- `local` - Local development with PostgreSQL
- `azure` - Production deployment in Azure
- `test` - Integration testing

### Environment Variables

Set these environment variables in your Azure deployment:

```bash
# Database connection
DB_URL=jdbc:postgresql://your-postgres-server:5432/candidatematch
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password

# Optional: Override default profile
SPRING_PROFILES_ACTIVE=azure
```

### Database Setup

You'll need a PostgreSQL database. Options include:
- Azure Database for PostgreSQL
- Azure Container Instances with PostgreSQL
- External PostgreSQL service

The application requires:
- PostgreSQL 15+ with pgvector extension
- Database schema: `candidatematch` (created automatically)
- User with CREATE/ALTER permissions on the schema

## Frontend Integration

After deploying the backend:

1. **Get the backend URL**: From Container Apps or App Service output
2. **Configure frontend**: Set `BACKEND_BASE_URL` GitHub secret in the frontend repository
3. **Deploy frontend**: Push to main branch to trigger Azure Static Web Apps deployment

The frontend will make API calls to `/api/*` which Azure Static Web Apps will proxy to your backend. Because the backend controllers do not include an `/api` prefix (e.g., `/auth`, `/consultants`, `/chatbot`, `/health`), the SWA configuration rewrites `/api/{*path}` to `__BACKEND_BASE_URL__/{path}`. This avoids 405/404 errors for login and other endpoints.

## Monitoring and Health Checks

### Health Endpoints
- `/actuator/health` - Application health status
- `/actuator/info` - Application information

### Logging
Application logs are available through:
- Azure Container Apps: `az containerapp logs show`
- Azure App Service: Azure Portal → Log Stream

### Metrics
Both deployment options provide built-in metrics for:
- CPU and memory usage
- Request rates and response times
- Error rates

## Using .env.azure in Azure

When deploying, set the following environment variables using the values from your .env.azure file. Do not paste secrets into source code or CI logs — use secret variables.

- DB_URL=jdbc:postgresql://cloudberries-candidate-match-pgdb.postgres.database.azure.com:5432/candidatematch?sslmode=require
- DB_USERNAME={{DB_USERNAME_FROM_ENV}}
- DB_PASSWORD={{DB_PASSWORD_FROM_ENV}}

In GitHub Actions for the frontend, set:
- BACKEND_BASE_URL=https://cloudberries-candidate-match-ca.whitesand-767916af.westeurope.azurecontainerapps.io
- AZURE_STATIC_WEB_APPS_API_TOKEN={{AZURE_SWA_TOKEN}}

## Security and 405 Method Not Allowed

To avoid 405 when logging in or calling APIs:
- Frontend (SWA): `/api/{*path}` is rewritten to `__BACKEND_BASE_URL__/{path}` so `/api/auth/login` becomes `{BACKEND}/auth/login`.
- Backend (Spring Security): CORS allows the SWA URL and localhost; OPTIONS is permitted; CSRF is disabled for stateless APIs. Login endpoints `/auth/**` are open.
- For local development: set `VITE_API_BASE=http://localhost:8080` so the browser calls the local backend directly.

If you still hit 405:
- Verify the method matches the controller mapping (POST /auth/login)
- Check that SWA rewrite goes to the correct path (no extra `/api` on the backend)
- Ensure IntelliJ is running the `local` profile

## Troubleshooting

### Common Issues

1. **Database Connection**: Verify `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables
2. **Port Configuration**: Ensure container exposes port 8080
3. **Health Check Failures**: Check `/actuator/health` endpoint accessibility
4. **CORS Issues**: Should be resolved by Azure Static Web Apps proxy

### Team Rules Reminder

- **ON DELETE CASCADE**: Consider adding to `ai_conversation_turn(conversation_id)` in future Liquibase migration to simplify cleanup
- **Java Version**: Always use 21.0.7-tem for consistency
- **Database Auth**: Username/password only for local development (no certificates)

## Security Considerations

- Container runs as non-root user (see Dockerfile)
- Database credentials via environment variables only
- HTTPS termination handled by Azure platform
- No CORS configuration needed due to Azure Static Web Apps proxy setup

## Cost Optimization

- **Container Apps**: Scales to zero when not in use
- **App Service**: Always-on for consistent performance
- Consider using Basic tiers for development/testing environments