#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(pwd)"
GROUP_ID="com.ossobo"
ARTIFACT_ID="winterfx"
MVN_REPO="$HOME/.m2/repository"
DEFAULT_VERSION="1.0.0"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

extract_version() {
  if [[ -f pom.xml ]]; then
    awk -F'[<>]' '/<version>/{print $3; exit}' pom.xml | tr -d '[:space:]'
  else
    echo ""
  fi
}

print_header "🔍 Verificando ambiente"

if ! command_exists mvn; then
  print_error "Maven não encontrado no PATH."
  exit 1
fi

if ! command_exists java; then
  print_error "Java não encontrado no PATH."
  exit 1
fi

VERSION="$(extract_version)"
if [[ -z "${VERSION}" ]]; then
  VERSION="$DEFAULT_VERSION"
  print_warning "Versão não encontrada no pom.xml. Usando ${VERSION}."
fi

print_success "Versão: ${VERSION}"

if [[ ! -f "pom.xml" ]]; then
  print_error "pom.xml não encontrado no diretório atual."
  exit 1
fi

print_header "🔨 Build"

mvn clean package -DskipTests
mvn source:jar -DskipTests
mvn javadoc:jar -DskipTests

print_header "📦 Artefatos gerados"

ls -la target/*.jar 2>/dev/null || {
  print_error "Nenhum JAR encontrado em target/."
  exit 1
}

MAIN_JAR="target/${ARTIFACT_ID}-${VERSION}.jar"
if [[ ! -f "${MAIN_JAR}" ]]; then
  MAIN_JAR="$(ls target/*.jar | grep -v -- '-sources\.jar$' | grep -v -- '-javadoc\.jar$' | head -n 1)"
fi

if [[ ! -f "${MAIN_JAR}" ]]; then
  print_error "JAR principal não encontrado."
  exit 1
fi

print_success "JAR principal: ${MAIN_JAR}"

print_header "📥 Instalando no Maven Local"

mvn install:install-file \
  -Dfile="${MAIN_JAR}" \
  -DgroupId="${GROUP_ID}" \
  -DartifactId="${ARTIFACT_ID}" \
  -Dversion="${VERSION}" \
  -Dpackaging=jar \
  -DgeneratePom=true

if [[ -f "target/${ARTIFACT_ID}-${VERSION}-sources.jar" ]]; then
  mvn install:install-file \
    -Dfile="target/${ARTIFACT_ID}-${VERSION}-sources.jar" \
    -DgroupId="${GROUP_ID}" \
    -DartifactId="${ARTIFACT_ID}" \
    -Dversion="${VERSION}" \
    -Dpackaging=jar \
    -Dclassifier=sources \
    -DgeneratePom=false
fi

if [[ -f "target/${ARTIFACT_ID}-${VERSION}-javadoc.jar" ]]; then
  mvn install:install-file \
    -Dfile="target/${ARTIFACT_ID}-${VERSION}-javadoc.jar" \
    -DgroupId="${GROUP_ID}" \
    -DartifactId="${ARTIFACT_ID}" \
    -Dversion="${VERSION}" \
    -Dpackaging=jar \
    -Dclassifier=javadoc \
    -DgeneratePom=false
fi

print_header "✅ Verificação final"

INSTALLED_JAR="${MVN_REPO}/$(echo "${GROUP_ID}" | tr '.' '/')/${ARTIFACT_ID}/${VERSION}/${ARTIFACT_ID}-${VERSION}.jar"

if [[ -f "${INSTALLED_JAR}" ]]; then
  print_success "Instalação confirmada: ${INSTALLED_JAR}"
  du -h "${INSTALLED_JAR}" | awk '{print "Tamanho: " $1}'
else
  print_error "JAR não encontrado no repositório local."
  exit 1
fi

print_header "📖 Uso"

echo "Adicione ao seu pom.xml:"
echo
echo "  <dependency>"
echo "    <groupId>${GROUP_ID}</groupId>"
echo "    <artifactId>${ARTIFACT_ID}</artifactId>"
echo "    <version>${VERSION}</version>"
echo "  </dependency>"
echo
print_success "Concluído com sucesso."