#!/bin/bash

# ============================================================================
# 🚀 NEXUSFX - BUILD E INSTALAÇÃO UNIVERSAL
# Versão: 2.0.0
# Descrição: Script completo para build e instalação do framework NexusFX
# Plataformas: Linux, macOS, Windows (Git Bash/WSL)
# ============================================================================

set -e  # Para em caso de erro

# ============================================================================
# CONFIGURAÇÕES
# ============================================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_DIR=$(pwd)
MVN_REPO="$HOME/.m2/repository/com/ossobo/nexusfx"
MVN_WRAPPER="./mvnw"  # Maven Wrapper (se existir)

# ============================================================================
# FUNÇÕES UTILITÁRIAS
# ============================================================================

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

# ============================================================================
# 1. VERIFICAR/INSTALAR MAVEN
# ============================================================================
print_header "🔍 PASSO 1: VERIFICANDO MAVEN"

check_maven() {
    if command -v mvn &> /dev/null; then
        MAVEN_VERSION=$(mvn --version | head -n1 | awk '{print $3}')
        print_success "Maven encontrado: versão $MAVEN_VERSION"
        return 0
    else
        return 1
    fi
}

install_maven_linux() {
    print_warning "Instalando Maven via apt..."
    sudo apt update
    sudo apt install -y maven
}

install_maven_macos() {
    print_warning "Instalando Maven via Homebrew..."
    if ! command -v brew &> /dev/null; then
        print_warning "Homebrew não encontrado. Instalando Homebrew primeiro..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi
    brew install maven
}

install_maven_windows() {
    print_warning "Windows detectado. Instalando Maven via Chocolatey..."
    if ! command -v choco &> /dev/null; then
        print_error "Chocolatey não encontrado. Instale manualmente: https://chocolatey.org/install"
        print_error "Ou use Maven Wrapper (recomendado para Windows)"
        return 1
    fi
    choco install maven -y
}

# Detectar SO
detect_os() {
    case "$(uname -s)" in
        Linux*)     OS="Linux";;
        Darwin*)    OS="macOS";;
        CYGWIN*|MINGW*|MSYS*) OS="Windows";;
        *)          OS="Unknown";;
    esac
    echo $OS
}

# Verificar/instalar Maven
if ! check_maven; then
    print_warning "Maven não encontrado. Instalando..."
    OS=$(detect_os)
    echo "Sistema operacional detectado: $OS"

    case $OS in
        Linux)
            install_maven_linux
            ;;
        macOS)
            install_maven_macos
            ;;
        Windows)
            if ! install_maven_windows; then
                print_warning "Usando Maven Wrapper como fallback..."
                if [ ! -f "$MVN_WRAPPER" ]; then
                    print_warning "Maven Wrapper não encontrado. Baixando..."
                    mvn -N io.takari:maven:wrapper
                fi
                MVN_CMD="$MVN_WRAPPER"
            else
                MVN_CMD="mvn"
            fi
            ;;
        *)
            print_error "Sistema não suportado para instalação automática"
            exit 1
            ;;
    esac

    # Verificar se instalação funcionou
    if ! check_maven && [ -z "$MVN_CMD" ]; then
        print_error "Falha na instalação do Maven"
        exit 1
    fi
fi

# Definir comando Maven
MVN_CMD=${MVN_CMD:-"mvn"}

# ============================================================================
# 2. EXTRAIR VERSÃO DO POM.XML
# ============================================================================
print_header "📦 PASSO 2: LENDO VERSÃO DO PROJETO"

if [ ! -f "pom.xml" ]; then
    print_error "pom.xml não encontrado no diretório atual!"
    exit 1
fi

# Extrair versão do pom.xml
VERSION=$(grep -m1 "<version>" pom.xml | sed 's/<version>\(.*\)<\/version>/\1/' | xargs)
if [ -z "$VERSION" ]; then
    print_error "Não foi possível extrair versão do pom.xml"
    exit 1
fi

print_success "Versão do projeto: $VERSION"
print_success "Comando Maven: $MVN_CMD"

# ============================================================================
# 3. BUILD DO PROJETO
# ============================================================================
print_header "🔨 PASSO 3: COMPILANDO E GERANDO JARS"

echo "📦 Gerando JAR principal..."
$MVN_CMD clean compile package -DskipTests

echo "📚 Gerando JAR com sources..."
$MVN_CMD source:jar -DskipTests

echo "📖 Gerando JAR com JavaDoc..."
$MVN_CMD javadoc:jar -DskipTests

echo "🥫 Gerando fat JAR (com dependências)..."
$MVN_CMD package -P shade -DskipTests 2>/dev/null || print_warning "Profile 'shade' não encontrado. Ignorando fat JAR."

# Listar JARs gerados
echo -e "\n${BLUE}JARs gerados:${NC}"
ls -la target/*.jar 2>/dev/null || print_warning "Nenhum JAR encontrado em target/"

# ============================================================================
# 4. GERENCIAR INSTALAÇÃO NO REPOSITÓRIO LOCAL
# ============================================================================
print_header "📥 PASSO 4: GERENCIANDO INSTALAÇÃO LOCAL"

# Verificar se já existe versão instalada
if [ -d "$MVN_REPO" ]; then
    INSTALLED_VERSIONS=$(ls -1 "$MVN_REPO" 2>/dev/null | grep -E '^[0-9]' | sort -V)

    if [ ! -z "$INSTALLED_VERSIONS" ]; then
        echo -e "${YELLOW}Versões instaladas encontradas:${NC}"
        echo "$INSTALLED_VERSIONS" | while read ver; do
            if [ "$ver" = "$VERSION" ]; then
                echo "  ${GREEN}▶ $ver (atual)${NC}"
            else
                echo "  $ver"
            fi
        done

        # Perguntar se deseja remover versões antigas
        echo
        read -p "Remover versões antigas? (s/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Ss]$ ]]; then
            for ver in $INSTALLED_VERSIONS; do
                if [ "$ver" != "$VERSION" ]; then
                    print_warning "Removendo versão $ver..."
                    rm -rf "$MVN_REPO/$ver"
                fi
            done
            print_success "Versões antigas removidas"
        fi

        # Remover versão atual se existir (para reinstalação limpa)
        if [ -d "$MVN_REPO/$VERSION" ]; then
            print_warning "Versão $VERSION já existe. Removendo para reinstalação limpa..."
            rm -rf "$MVN_REPO/$VERSION"
        fi
    else
        print_warning "Nenhuma versão válida encontrada no diretório"
    fi
else
    print_warning "Nenhuma instalação anterior encontrada"
fi

# ============================================================================
# 5. INSTALAR NOVA VERSÃO
# ============================================================================
print_header "🚀 PASSO 5: INSTALANDO NOVA VERSÃO"

# Encontrar o JAR principal
MAIN_JAR=$(ls target/NexusFX-${VERSION}.jar 2>/dev/null | head -n1)
if [ -z "$MAIN_JAR" ]; then
    MAIN_JAR=$(ls target/*.jar | grep -v sources | grep -v javadoc | grep -v shade | head -n1)
fi

if [ -z "$MAIN_JAR" ]; then
    print_error "JAR principal não encontrado!"
    exit 1
fi

print_success "Instalando: $MAIN_JAR"

# Instalar via Maven
$MVN_CMD install:install-file \
    -Dfile="$MAIN_JAR" \
    -DgroupId=com.ossobo \
    -DartifactId=nexusfx \
    -Dversion="$VERSION" \
    -Dpackaging=jar \
    -DgeneratePom=true \
    -DlocalRepositoryPath="$HOME/.m2/repository"

# ============================================================================
# 6. VERIFICAÇÃO FINAL
# ============================================================================
print_header "✅ PASSO 6: VERIFICANDO INSTALAÇÃO"

if [ -f "$MVN_REPO/$VERSION/nexusfx-${VERSION}.jar" ]; then
    print_success "Instalação confirmada!"
    echo -e "\n${BLUE}Arquivos instalados:${NC}"
    ls -la "$MVN_REPO/$VERSION/"

    # Mostrar tamanho do JAR
    JAR_SIZE=$(du -h "$MVN_REPO/$VERSION/nexusfx-${VERSION}.jar" | cut -f1)
    print_success "Tamanho do JAR: $JAR_SIZE"
else
    print_error "Falha na verificação! JAR não encontrado em $MVN_REPO/$VERSION/"
    exit 1
fi

# ============================================================================
# 7. INSTRUÇÕES DE USO
# ============================================================================
print_header "📖 COMO USAR"

echo -e "${GREEN}✅ NEXUSFX v${VERSION} INSTALADO COM SUCESSO!${NC}"
echo
echo "Para usar em outros projetos, adicione no pom.xml:"
echo
echo "    <dependency>"
echo "        <groupId>com.ossobo</groupId>"
echo "        <artifactId>nexusfx</artifactId>"
echo "        <version>${VERSION}</version>"
echo "    </dependency>"
echo
echo "E não esqueça do JavaFX (seu projeto precisa incluir):"
echo
echo "    <dependency>"
echo "        <groupId>org.openjfx</groupId>"
echo "        <artifactId>javafx-controls</artifactId>"
echo "        <version>25.0.2</version>"
echo "    </dependency>"
echo
echo -e "${BLUE}Arquivos gerados:${NC}"
echo "  • JAR principal: target/NexusFX-${VERSION}.jar"
echo "  • Sources JAR:   target/NexusFX-${VERSION}-sources.jar"
echo "  • JavaDoc JAR:   target/NexusFX-${VERSION}-javadoc.jar"
if [ -f "target/NexusFX-${VERSION}-shaded.jar" ]; then
    echo "  • Fat JAR:       target/NexusFX-${VERSION}-shaded.jar"
fi
echo
echo -e "${GREEN}========================================${NC}"

# ============================================================================
# FIM
# ============================================================================
exit 0