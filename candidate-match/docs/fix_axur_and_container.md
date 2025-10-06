You’re almost there—the error is from Static Web Apps:

“Only Container Apps supporting https are allowed as Linked Backends.”

That means your Container App isn’t exposing a public HTTPS ingress the way SWA expects. Fix it in two parts:

⸻

1) Fix the Azure Container App (ingress)

Portal (quickest):
Container App ➜ Ingress ➜
•	Enable ingress: On
•	External (not internal)
•	Target port: 8080 (or whatever your app listens on)
•	Transport: Auto (HTTP inside, HTTPS at the gateway)
•	(Optional) Only HTTPS: On

GitHub Action (so it stays fixed on redeploy): add ingress settings to the deploy step:

- name: Build and push container image to registry
  uses: azure/container-apps-deploy-action@v2
  with:
  appSourcePath: ${{ github.workspace }}
  dockerfilePath: ./candidate-match/Dockerfile
  registryUrl: ghcr.io
  imageToBuild: ghcr.io/thomasandersen77/cloudberries-candidate-match:${{ github.sha }}
  resourceGroup: cloudberries-internal-dev
  containerAppName: cloudberries-candidate-match-ca
  # NEW: ingress config so SWA can link over HTTPS
  ingress: external
  targetPort: 8080
  ingressTransport: auto

After this, SWA ➜ APIs ➜ Link new Backend should accept the Container App.

⸻

2) Make sure the container listens correctly

Azure injects a PORT env var in some hosts. Bind your Spring app to 0.0.0.0 and honor PORT.

Dockerfile (small changes):

# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/*
COPY pom.xml ./
COPY candidate-match/pom.xml candidate-match/pom.xml
COPY ai-rag-service/pom.xml ai-rag-service/pom.xml
COPY teknologi-barometer-service/pom.xml teknologi-barometer-service/pom.xml
RUN mvn -B -DskipTests -DskipITs=true -pl candidate-match -am dependency:go-offline
COPY candidate-match/src candidate-match/src
RUN mvn -B -DskipTests -DskipITs=true -pl candidate-match -am clean package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd --system appuser && useradd --system --no-create-home --gid appuser --shell /usr/sbin/nologin appuser
WORKDIR /app
COPY --from=build /workspace/candidate-match/target/candidate-match.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser

# Expose container port (must match targetPort)
EXPOSE 8080

# Health endpoint (keep)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
CMD curl -fsS http://localhost:8080/actuator/health >/dev/null || exit 1

# IMPORTANT: bind to 0.0.0.0 and honor $PORT if present
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75.0 -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-8080} -jar app.jar"]

Spring Boot Actuator
Make sure spring-boot-starter-actuator is on the classpath and you expose the health endpoint:

# application.properties (or yaml)
management.endpoints.web.exposure.include=health,info
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true


⸻

3) Link from Static Web Apps (SWA)

In SWA ➜ APIs ➜ Link new Backend:
•	Backend type: Container App
•	Choose your subscription/RG
•	Pick cloudberries-candidate-match-ca
•	Environment: Production

If it still balks, check:
•	The container app URL is reachable over HTTPS (you should have a https://<app>.<region>.azurecontainerapps.io URL opening).
•	Ingress is External and target port matches your app (8080).
•	No private VNet-only settings blocking public access.

⸻

4) Optional hardening / nice-to-haves
   •	CORS/Routes: Configure SWA routes (if your API sits under /api/**) so the frontend calls are proxied cleanly.
   •	Auth: If you turn on SWA auth, consider adding an unauthenticated route for /actuator/health and protected routes for /api/**.
   •	Readiness probe: In Container App, you can add an HTTP probe to /actuator/health/readiness.

⸻

TL;DR
•	Enable external HTTPS ingress on the Container App (and set it in the deploy action).
•	Make the Spring app bind to 0.0.0.0 and honor the PORT env.
•	Ensure /actuator/health is exposed.
•	Then link from Static Web Apps.

Do the two code tweaks above (ENTRYPOINT + actuator settings) and the deploy action ingress settings, and your SWA ➜ “Link Backend” should go green.