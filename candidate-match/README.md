# Candidate Match Backend

## JDK og Maven via SDKMAN (auto-bytte)

Dette repoet har en .sdkmanrc i rotmappen som foreslår Java 25 (Temurin) og Maven 3.9.9. Det er repo-lokalt.

- Slå på auto-bytte én gang:
```bash
echo "sdkman_auto_env=true" >> "$HOME/.sdkman/etc/config"
```

- Bytt når du går inn i repoet:
```bash
cd /Users/tandersen/git/cloudberries-candidate-match
sdk env install   # første gang installerer manglende versjoner
# eller
sdk env           # bare bytt
```

- Midlertidig overstyring:
```bash
sdk use java 21.0.7-tem
```

Se også hoved-README for mer detaljer.

