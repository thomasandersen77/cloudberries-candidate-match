#!/usr/bin/env bash

set -euo pipefail

find_project_root() {
  local dir="$1"
  while [[ "$dir" != "/" ]]; do
    if [[ -f "$dir/pom.xml" ]] && grep -q "<modules>" "$dir/pom.xml"; then
      echo "$dir"
      return 0
    fi
    dir="$(dirname "$dir")"
  done
  return 1
}

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(find_project_root "$SCRIPT_DIR" || true)"

if [[ -z "${ROOT_DIR:-}" ]]; then
  ROOT_DIR="$(find_project_root "$(pwd)" || true)"
fi

if [[ -z "${ROOT_DIR:-}" ]]; then
  echo "Fant ikke prosjektrot med parent pom.xml og <modules>."
  exit 1
fi

OUTPUT_DIR="$ROOT_DIR/_export_for_ai"
mkdir -p "$OUTPUT_DIR"

mapfile -t MODULES < <(awk -F'[<>]' '/<module>/{print $3}' "$ROOT_DIR/pom.xml")

if [[ "${#MODULES[@]}" -eq 0 ]]; then
  echo "Fant ingen moduler i $ROOT_DIR/pom.xml."
  exit 1
fi

echo "Prosjektrot: $ROOT_DIR"
echo "Eksportmappe: $OUTPUT_DIR"
echo
ROOT_INCLUDE=()

for readme in "$ROOT_DIR"/README*; do
  if [[ -f "$readme" ]]; then
    ROOT_INCLUDE+=("$(basename "$readme")")
  fi
done

for cfg in pom.xml openapi.yaml WARP.md .sdkmanrc mvnw mvnw.cmd .mvn; do
  if [[ -e "$ROOT_DIR/$cfg" ]]; then
    ROOT_INCLUDE+=("$cfg")
  fi
done

for module in "${MODULES[@]}"; do
  module_path="$ROOT_DIR/$module"
  if [[ ! -d "$module_path" ]]; then
    echo "Hopper over '$module' (mappen finnes ikke: $module_path)"
    continue
  fi

  zip_path="$OUTPUT_DIR/${module}-for-ai.zip"
  rm -f "$zip_path"
  include_paths=("$module")
  include_paths+=("${ROOT_INCLUDE[@]}")

  (
    cd "$ROOT_DIR"
    zip -rq "$zip_path" "${include_paths[@]}" \
      -x "*/target/*" \
      -x "*/build/*" \
      -x "*/.idea/*" \
      -x "*/.gradle/*" \
      -x "*/node_modules/*" \
      -x "*/.DS_Store"
  )

  size="$(du -h "$zip_path" | awk '{print $1}')"
  readme_count="$(zipinfo -1 "$zip_path" | grep -E '(^|/)[Rr][Ee][Aa][Dd][Mm][Ee]' | wc -l | tr -d ' ')"
  config_count="$(zipinfo -1 "$zip_path" | grep -E '(application[^/]*\.ya?ml$|\.properties$|\.conf$|pom\.xml$|openapi\.yaml$|\.sdkmanrc$|maven-wrapper\.properties$)' | wc -l | tr -d ' ')"
  echo "Opprettet: $zip_path ($size) - README-filer: $readme_count, konfig-filer: $config_count"
done

echo
echo "Ferdig. Zip-filene ligger i: $OUTPUT_DIR"
