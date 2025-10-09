# SDKMAN Setup and Usage

This project uses SDKMAN to manage Java and Maven versions for consistent development environments.

## Team Rule: Always use Java SDK 21.0.7-tem 
This specific version is required to avoid bytebuddy version conflicts when running the candidate-match module.

## Installation

If you don't have SDKMAN installed:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

## Enable Auto-Switching

Enable automatic environment switching when entering this directory:

```bash
sed -i.bak 's/sdkman_auto_env=.*/sdkman_auto_env=true/' "$HOME/.sdkman/etc/config"
```

## Project Usage

When you enter this directory, SDKMAN will automatically use the correct versions defined in `.sdkmanrc`:

```bash
cd /Users/tandersen/git/cloudberries-candidate-match/candidate-match
sdk env
java -version   # should show: openjdk version "21.0.7"
mvn -version    # should show: Apache Maven 3.9.9
```

## Manual Installation (if needed)

If the required versions aren't installed automatically:

```bash
sdk install java 21.0.7-tem
sdk install maven 3.9.9
sdk use java 21.0.7-tem
sdk use maven 3.9.9
```

## Versions

- **Java**: 21.0.7-tem (Eclipse Temurin)
- **Maven**: 3.9.9

These versions are tested and known to work without dependency conflicts.