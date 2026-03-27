# Azure Container Apps: Log Access via SSH

## ✅ Configuration Summary

### Logback Configuration
- **Log location:** `/tmp/logs/application.log`
- **Max file size:** 50MB per file
- **Retention:** 3 days (72 hours)
- **Total cap:** 200MB across all log files
- **Rotation:** Size + time based (daily rollover)

### Why `/tmp/logs`?
- ✅ **SSH accessible** - You can tail it in real-time
- ✅ **No persistent volume needed** - Uses ephemeral storage (free)
- ✅ **Automatic cleanup** - Container restart clears old logs
- ⚠️ **Lost on restart** - Logs are ephemeral (intentional for cost)

## 🔌 SSH Access to Container Apps

### Method 1: Azure Portal (Easiest)

1. Go to Azure Portal → Container Apps
2. Select your app: `cloudberries-candidate-match-ca`
3. Click **Console** in the left menu
4. Select **Bash** shell
5. Run: `tail -f /tmp/logs/application.log`

### Method 2: Azure CLI

```bash
# Install Azure CLI extension
az extension add --name containerapp

# Open SSH session
az containerapp exec \
  --name cloudberries-candidate-match-ca \
  --resource-group kubeberries \
  --command /bin/bash

# Then inside the container:
tail -f /tmp/logs/application.log
```

### Method 3: Direct SSH Command (One-liner)

```bash
# Tail logs directly
az containerapp exec \
  --name cloudberries-candidate-match-ca \
  --resource-group kubeberries \
  --command "tail -f /tmp/logs/application.log"

# View last 100 lines
az containerapp exec \
  --name cloudberries-candidate-match-ca \
  --resource-group kubeberries \
  --command "tail -n 100 /tmp/logs/application.log"
```

## 📁 Log Files Structure

```
/tmp/logs/
├── application.log              # Current log (active)
├── application.2026-03-27.0.log # Today's rollover (if > 50MB)
├── application.2026-03-26.0.log # Yesterday
├── application.2026-03-26.1.log # Yesterday (2nd file if > 50MB)
└── application.2026-03-25.0.log # 2 days ago
```

**Automatic cleanup:**
- Files older than 3 days: Deleted
- Total > 200MB: Oldest deleted first
- On restart: All cleaned (ephemeral storage)

## 🛠️ Useful Log Commands

### Tail with filtering

```bash
# Follow logs (real-time)
tail -f /tmp/logs/application.log

# Follow only ERROR logs
tail -f /tmp/logs/application.log | grep ERROR

# Follow only specific requestId
tail -f /tmp/logs/application.log | grep "requestId:abc123"

# Follow without timestamps (cleaner)
tail -f /tmp/logs/application.log | cut -d' ' -f4-
```

### Search and filter

```bash
# Find all ERROR logs today
grep ERROR /tmp/logs/application.log

# Find logs for specific consultant
grep "consultantId:12345" /tmp/logs/application.log

# Find logs between timestamps
awk '/2026-03-27T14:00/,/2026-03-27T15:00/' /tmp/logs/application.log

# Count errors
grep -c ERROR /tmp/logs/application.log
```

### Check log size

```bash
# Current log size
ls -lh /tmp/logs/application.log

# All logs size
du -sh /tmp/logs/

# List all log files
ls -lht /tmp/logs/
```

## 💰 Cost Impact

### With File Logging (Current Setup)
```
Storage: /tmp (ephemeral)       $0 (included in container)
Max usage: 200MB                $0 (no extra charge)
Container cost:                 $30/month (0.5 CPU / 1GB RAM)
```

### With Persistent Volume (NOT recommended)
```
Azure Files: 5GB minimum        ~$1-2/month
Container cost:                 $30/month
Total:                          $31-32/month
```

**Verdict:** Ephemeral `/tmp` storage is **FREE** and sufficient for debugging.

## ⚠️ Important Notes

### Ephemeral Storage Limitations
- **Logs lost on restart** - This is intentional for cost savings
- **Not suitable for auditing** - Use Azure Application Insights for that
- **Good for real-time debugging** - Perfect for SSH + tail
- **No backup needed** - If you need audit logs, use external logging

### When to Use Persistent Storage
❌ **Don't use persistent volume** unless:
- You need logs for compliance/auditing (> 3 days)
- You need logs to survive restarts
- You can justify the extra $1-2/month

✅ **Current setup is best for:**
- Real-time debugging via SSH
- Cost-conscious operations
- Low-traffic applications
- Temporary troubleshooting

## 🔒 Security Considerations

### SSH Access Control
Currently, anyone with **Contributor** role on the Container App can SSH.

To restrict:
1. Use Azure RBAC to limit who has access
2. Create custom role with only `Microsoft.App/containerApps/exec/action`
3. Use Azure AD groups for team access

### Log Content
⚠️ **Never log sensitive data:**
- Passwords, API keys, tokens
- Personal data (GDPR compliance)
- Credit card numbers
- Session IDs (in production)

Your current pattern includes `requestId` which is fine for correlation.

## 📊 Monitoring & Alerts

### Alternative: Azure Log Stream
If you don't trust Azure Log Stream but want real-time logs:

```bash
# Stream logs via CLI (without SSH)
az containerapp logs show \
  --name cloudberries-candidate-match-ca \
  --resource-group kubeberries \
  --follow \
  --tail 50
```

### Application Insights (Optional)
For production monitoring (not real-time debugging):

```yaml
# application-prod.yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,metrics"
  metrics:
    export:
      azure-monitor:
        enabled: true
```

Cost: ~$2-5/month for low-traffic app.

## 🧪 Testing the Setup

### 1. Deploy and Test

```bash
# Deploy backend with new logback config
git add candidate-match/src/main/resources/logback-spring.xml
git commit -m "Add file logging with rotation for SSH access"
git push

# After deployment, test SSH access
az containerapp exec \
  --name cloudberries-candidate-match-ca \
  --resource-group kubeberries \
  --command "ls -la /tmp/logs/"

# Should show application.log
```

### 2. Generate Test Logs

```bash
# Make some API calls to generate logs
curl https://cloudberries-candidate-match-ca.../api/consultants

# Check logs
az containerapp exec \
  --name cloudberries-candidate-match-ca \
  --resource-group kubeberries \
  --command "tail -20 /tmp/logs/application.log"
```

### 3. Test Log Rotation

```bash
# Check current log size
az containerapp exec \
  --name cloudberries-candidate-match-ca \
  --resource-group kubeberries \
  --command "ls -lh /tmp/logs/"

# If > 50MB, should see rollover files
```

## 📝 Quick Reference Commands

```bash
# SSH into container
az containerapp exec -n cloudberries-candidate-match-ca -g kubeberries --command /bin/bash

# Tail logs (real-time)
tail -f /tmp/logs/application.log

# Last 100 lines
tail -100 /tmp/logs/application.log

# Follow ERROR only
tail -f /tmp/logs/application.log | grep ERROR

# Search for specific text
grep "ConsultantService" /tmp/logs/application.log

# Show newest logs first
tac /tmp/logs/application.log | less

# Count log lines
wc -l /tmp/logs/application.log

# Disk usage
du -sh /tmp/logs/

# Exit SSH
exit
```

## 🎯 Best Practices

### For Development/Staging
✅ Use ephemeral `/tmp` logs with SSH access
✅ Keep retention short (3 days)
✅ Use `tail -f` for real-time debugging

### For Production
✅ Ephemeral `/tmp` logs for SSH debugging
✅ **Also** send critical logs to Application Insights
✅ Use structured logging (JSON) for parsing
✅ Monitor with alerts, not manual SSH

### For Cost Optimization
✅ Ephemeral storage (free)
✅ 3 days retention (not 7)
✅ 200MB total cap
✅ No persistent volumes

## 🔄 Rollback (If Needed)

If file logging causes issues:

```xml
<!-- Comment out FILE appender -->
<!--
<appender-ref ref="FILE"/>
-->
```

Redeploy. Logs will only go to console (Azure Log Stream).

## 🎉 Summary

**What you get:**
- ✅ Real-time logs via SSH (`tail -f`)
- ✅ 3 days retention (200MB cap)
- ✅ Automatic rotation (50MB per file)
- ✅ Free storage (ephemeral `/tmp`)
- ✅ No persistent volume costs

**What to remember:**
- ⚠️ Logs lost on container restart (by design)
- ⚠️ Only for debugging, not auditing
- ⚠️ SSH requires Azure RBAC permissions

**Commands you'll use most:**
```bash
# SSH in
az containerapp exec -n cloudberries-candidate-match-ca -g kubeberries --command /bin/bash

# Tail logs
tail -f /tmp/logs/application.log

# Filter errors
tail -f /tmp/logs/application.log | grep ERROR
```

**Perfect for your use case!** 🚀
