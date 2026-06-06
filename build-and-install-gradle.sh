#!/usr/bin/env bash
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

print_header() {
  echo -e "${BLUE}========================================${NC}"
  echo -e "${BLUE}$1${NC}"
  echo -e "${BLUE}========================================${NC}"
}

print_success() {
  echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
  echo -e "${YELLOW}⚠️ $1${NC}"
}

print_error() {
  echo -e "${RED}❌ $1${NC}"
}

print_header "🚀 Publicando no Maven Local"

if ! command -v gradle >/dev/null 2>&1 && [[ ! -f "./gradlew" ]]; then
  print_error "Gradle não encontrado e gradlew não existe."
  exit 1
fi

GRADLE_CMD="gradle"
[[ -f "./gradlew" ]] && GRADLE_CMD="./gradlew"

print_success "Usando: $GRADLE_CMD"

if [[ ! -f "build.gradle" && ! -f "build.gradle.kts" ]]; then
  print_error "build.gradle ou build.gradle.kts não encontrado."
  exit 1
fi

print_header "🔨 Build"
$GRADLE_CMD clean publishToMavenLocal

print_header "✅ Resultado"
print_success "Publicação concluída com sucesso."

echo
echo "Dependência para consumir o framework:"
echo "    implementation 'com.ossobo:winterfx:1.0.0'"