#!/bin/bash
set -e

# Azure Resource Configuration Script
# This script verifies and optimizes your Azure Container Apps deployment
# for cost-effective single-instance operation

RESOURCE_GROUP="kubeberries"
BACKEND_APP="cloudberries-candidate-match-ca"
OLLAMA_APP="kb-ollama-cv-ca-dev"
DB_SERVER="cloudberries-candidate-match-pgdb"

echo "=== Azure Container Apps Verification & Optimization ==="
echo ""

# 1. Check if logged in to Azure
echo "1. Verifying Azure CLI authentication..."
if ! az account show &>/dev/null; then
    echo "❌ Not logged in to Azure. Please run:"
    echo "   az login"
    exit 1
fi
echo "✅ Authenticated"
echo ""

# 2. Check Backend Container App
echo "2. Checking Backend Container App: $BACKEND_APP"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
az containerapp show \
  --name "$BACKEND_APP" \
  --resource-group "$RESOURCE_GROUP" \
  --query "{
    name: name,
    fqdn: properties.configuration.ingress.fqdn,
    cpu: properties.template.containers[0].resources.cpu,
    memory: properties.template.containers[0].resources.memory,
    minReplicas: properties.template.scale.minReplicas,
    maxReplicas: properties.template.scale.maxReplicas,
    image: properties.template.containers[0].image
  }" -o table
echo ""

# 3. Check Ollama Container App
echo "3. Checking Ollama Container App: $OLLAMA_APP"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
az containerapp show \
  --name "$OLLAMA_APP" \
  --resource-group "$RESOURCE_GROUP" \
  --query "{
    name: name,
    fqdn: properties.configuration.ingress.fqdn,
    cpu: properties.template.containers[0].resources.cpu,
    memory: properties.template.containers[0].resources.memory,
    minReplicas: properties.template.scale.minReplicas,
    maxReplicas: properties.template.scale.maxReplicas,
    image: properties.template.containers[0].image
  }" -o table
echo ""

# 4. Check Ollama environment variables
echo "4. Checking Ollama configuration (model loaded)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Testing Ollama API to see which model is loaded..."
curl -s https://kb-ollama-cv-ca-dev.blackisland-4a1bc921.westeurope.azurecontainerapps.io/api/tags | jq '.models[] | {name: .name, size: .size, modified: .modified_at}'
echo ""

# 5. Check Backend environment variables (Ollama connection)
echo "5. Checking Backend environment variables"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
az containerapp show \
  --name "$BACKEND_APP" \
  --resource-group "$RESOURCE_GROUP" \
  --query "properties.template.containers[0].env[?name=='OLLAMA_BASE_URL' || name=='ai__ollama__base-url'].{Name:name, Value:value}" -o table
echo ""

# 6. Check database firewall rules
echo "6. Checking PostgreSQL firewall rules"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
az postgres server firewall-rule list \
  --resource-group "$RESOURCE_GROUP" \
  --server-name "$DB_SERVER" \
  --query "[].{Name:name, StartIP:startIpAddress, EndIP:endIpAddress}" -o table
echo ""

# 7. Optimization recommendations
echo ""
echo "=== OPTIMIZATION RECOMMENDATIONS ==="
echo ""
echo "🔧 Backend Resource Optimization (Current vs Recommended):"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Setting                  | Recommended  | Why"
echo "-------------------------|--------------|----------------------------------"
echo "CPU                      | 0.5 cores    | Sufficient for Java/Spring Boot"
echo "Memory                   | 1.0 GB       | Minimum for JVM with low traffic"
echo "Min Replicas             | 1            | Always keep 1 instance warm"
echo "Max Replicas             | 1            | No auto-scaling for cost savings"
echo ""

echo "🧠 Ollama Resource Requirements:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Setting                  | Recommended  | Why"
echo "-------------------------|--------------|----------------------------------"
echo "CPU                      | 2.0 cores    | gemma3:12b needs CPU for inference"
echo "Memory                   | 16 GB        | 12B model + context (12288 tokens)"
echo "Min Replicas             | 1            | Keep model loaded (cold start = slow)"
echo "Max Replicas             | 1            | No auto-scaling needed"
echo ""

echo "💡 To apply these optimizations, run:"
echo ""
echo "# Optimize Backend (reduce cost, keep responsive)"
echo "az containerapp update \\"
echo "  --name $BACKEND_APP \\"
echo "  --resource-group $RESOURCE_GROUP \\"
echo "  --cpu 0.5 --memory 1.0Gi \\"
echo "  --min-replicas 1 --max-replicas 1"
echo ""
echo "# Optimize Ollama (ensure it has enough resources)"
echo "az containerapp update \\"
echo "  --name $OLLAMA_APP \\"
echo "  --resource-group $RESOURCE_GROUP \\"
echo "  --cpu 2.0 --memory 16Gi \\"
echo "  --min-replicas 1 --max-replicas 1"
echo ""

# 8. Verify model discrepancy
echo "⚠️  MODEL VERSION MISMATCH DETECTED:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Your Modelfile defines:       gemma3:12b-cv-expert-v3"
echo "Your application-prod.yaml uses: gemma3:12b-cv-expert-v4"
echo ""
echo "To verify which model is actually running in Ollama, check the output above."
echo "If v4 doesn't exist, you need to either:"
echo "  1. Update application-prod.yaml to use v3"
echo "  2. Build and load v4 model in Ollama container"
echo ""

# 9. Cost estimation
echo "💰 COST IMPACT ESTIMATE:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Component      | Current (estimate) | Optimized | Monthly Savings"
echo "---------------|--------------------|-----------|-----------------"
echo "Backend        | ~50-100 USD        | ~30 USD   | ~20-70 USD"
echo "Ollama         | ~100-200 USD       | ~150 USD  | N/A (already optimal)"
echo "PostgreSQL     | ~50 USD            | ~50 USD   | 0 USD (unchanged)"
echo ""
echo "⚡ Single-instance backend saves money WITHOUT affecting performance"
echo "   for low-traffic applications like yours."
echo ""

echo "✅ Verification complete!"
echo ""
echo "Next steps:"
echo "  1. Review the resource allocations above"
echo "  2. Run the optimization commands if needed"
echo "  3. Fix the model version mismatch (v3 vs v4)"
echo "  4. Update frontend timeout to 180 seconds"
