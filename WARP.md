# Warp guide for cloudberries-candidate-match-web

This document is a lightweight runbook for local development, CI/CD, and smooth handovers. Pair it with the reusable workflows in `warp.mf`.

## Prerequisites
- macOS + Warp Terminal
- Homebrew (installed)
- Node.js (use the repo's preferred version manager; e.g. nvm)
- Optional Java tooling (follow team rule):
  - Install SDKMAN, then JDK and Maven via SDKMAN

```bash
# SDKMAN (optional, for Java-related tooling)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.3-tem
sdk install maven
```

## Getting started
```bash
# 1) Install dependencies
npm install  # replace with yarn/pnpm if the repo uses those

# 2) Run the app locally
npm run dev

# 3) Lint / Test
npm run lint
npm test

# 4) Build
npm run build
```

If commands differ in this repo, update scripts in package.json and the workflows in `warp.mf` accordingly.

## Common workflows (Warp)
Use the pre-defined workflows in `warp.mf` from Warp’s command palette:
- install → install dependencies
- dev → start dev server
- lint → run linters
- test → run tests
- build → production build
- docker:build → build a Docker image with {{image_tag}}
- azdo:pipelines:run → run an Azure DevOps pipeline with {{pipeline_name}} on {{branch}}

## Git and PR workflow
- Trunk-based: short-lived feature branches, frequent PRs
- Conventional Commits (e.g. `feat:`, `fix:`, `chore:`)
- Branch naming: `feature/<ticket-id>-<short-desc>` or `fix/<ticket-id>-<short-desc>`
- PRs must include: What/Why, test notes, risk/rollback, links to Boards items
- At least 1–2 reviewers; green CI required before merge

## CI/CD (Azure DevOps)
- Org: https://dev.azure.com/cloudberriesas/
- Typical pipeline stages: install → lint/test → build → (optional) deploy
- Variables and secrets: use Variable Groups and Key Vault (no secrets in YAML)
- For infra repos, apply uses approvals via Environments

## Security and secrets
- Do not commit secrets. Fetch from secure stores and export into env vars locally
- For local DB access: use username/password only (no client certificate auth)

## Handover checklist
- Small, focused PRs linked to a Board task
- Update README and this warp.md when behavior or commands change
- Ensure `npm test` and linting are green locally and in CI
- Add/adjust `warp.mf` workflows if new commands were introduced
- Include rollback notes and feature flags if applicable

## Troubleshooting
- Node version mismatches: check `.nvmrc` or `.tool-versions` if present
- Clean install: remove node_modules and lockfile, then re-install
- Verify CI parity: run the same commands locally as in pipelines

## Useful links
- Azure DevOps org: https://dev.azure.com/cloudberriesas/
- Warp Notebooks and Workflows: https://docs.warp.dev/features/warp-drive/
