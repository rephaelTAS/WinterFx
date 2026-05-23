#!/bin/bash

# ============================================================================
# 🚀 NEXUSFX - BUILD E INSTALAÇÃO UNIVERSAL (GRADLE)
# Versão: 1.0.0
# Descrição: Script completo para build e instalação do framework NexusFX via Gradle
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
GRADLE_WRAPPER="./gradlew"  # Gradle Wrapper

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
# 1. VERIFICAR/INSTALAR GRADLE
# ============================================================================
print_header "🔍 PASSO 1: VERIFICANDO GRADLE"

check_gradle() {
    if command -v gradle &> /dev/null; then
        GRADLE_VERSION=$(gradle --version | grep "Gradle" | head -n1 | awk '{print $2}')
        print_success "Gradle encontrado: versão $GRADLE_VERSION"
        GRADLE_CMD="gradle"
        return 0
    elif [ -f "$GRADLE_WRAPPER" ]; then
        print_success "Gradle Wrapper encontrado"
        GRADLE_CMD="$GRADLE_WRAPPER"
        return 0
    else
        return 1
    fi
}

install_gradle_linux() {
    print_warning "Instalando Gradle via sdkman..."
    if ! command -v sdk &> /dev/null; then
        print_warning "sdkman não encontrado. Instalando sdkman primeiro..."
        curl -s "https://get.sdkman.io" | bash
        source "$HOME/.sdkman/bin/sdkman-init.sh"
    fi
    sdk install gradle
}

install_gradle_macos() {
    print_warning "Instalando Gradle via Homebrew..."
    if ! command -v brew &> /dev/null;
        print_warning "Homebrew não encontrado. Instalando Homebrew primeiro..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi
    brew install gradle
}

install_gradle_windows() {
    print_warning "Windows detectado. Criando Gradle Wrapper..."
    if ! command -v java &> /dev/null; then
        print_error "Java não encontrado. Instale o JDK primeiro."
        exit 1
    fi

    # Baixar e configurar Gradle Wrapper
    if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
        print_warning "Baixando Gradle Wrapper..."
        gradle wrapper 2>/dev/null || {
            print_error "Não foi possível criar Gradle Wrapper"
            print_error "Instale manualmente o Gradle: https://gradle.org/install/"
            exit 1
        }
    fi
    GRADLE_CMD="./gradlew"
    print_success "Gradle Wrapper configurado"
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

# Verificar/instalar Gradle
if ! check_gradle; then
    print_warning "Gradle não encontrado. Instalando..."
    OS=$(detect_os)
    echo "Sistema operacional detectado: $OS"

    case $OS in
        Linux)
            install_gradle_linux
            GRADLE_CMD="gradle"
            ;;
        macOS)
            install_gradle_macos
            GRADLE_CMD="gradle"
            ;;
        Windows)
            install_gradle_windows
            # GRADLE_CMD já definido na função
            ;;
        *)
            print_error "Sistema não suportado para instalação automática"
            exit 1
            ;;
    esac

    # Verificar se instalação funcionou
    if ! check_gradle; then
        print_error "Falha na instalação do Gradle"
        exit 1
    fi
fi

print_success "Comando Gradle: $GRADLE_CMD"

# ============================================================================
# 2. EXTRAIR VERSÃO DO BUILD.GRADLE
# ============================================================================
print_header "📦 PASSO 2: LENDO VERSÃO DO PROJETO"

if [ ! -f "build.gradle" ] && [ ! -f "build.gradle.kts" ]; then
    print_error "build.gradle ou build.gradle.kts não encontrado!"
    exit 1
fi

# Extrair versão do build.gradle
if [ -f "build.gradle" ]; then
    VERSION=$(grep "^version" build.gradle | sed 's/version[[:space:]]*=[[:space:]]*['\''"]\(.*\)['\''"]/\1/' | xargs)
elif [ -f "build.gradle.kts" ]; then
    VERSION=$(grep "^version" build.gradle.kts | sed 's/version[[:space:]]*=[[:space:]]*['\''"]\(.*\)['\''"]/\1/' | xargs)
fi

if [ -z "$VERSION" ]; then
    # Tentar extrair do settings.gradle
    if [ -f "settings.gradle" ]; then
        VERSION=$(grep "rootProject.name" -A5 settings.gradle | grep "version" | sed 's/.*version[[:space:]]*=[[:space:]]*['\''"]\(.*\)['\''"].*/\1/' | xargs)
    fi
fi

if [ -z "$VERSION" ]; then
    print_warning "Versão não encontrada no build.gradle. Usando 1.0.0 como padrão."
    VERSION="1.0.0"
fi

print_success "Versão do projeto: $VERSION"

# ============================================================================
# 3. VERIFICAR PLUGINS NECESSÁRIOS
# ============================================================================
print_header "🔧 PASSO 3: VERIFICANDO CONFIGURAÇÃO DO GRADLE"

# Verificar se tem os plugins necessários para publicar no Maven Local
if [ -f "build.gradle" ]; then
    if ! grep -q "maven-publish" build.gradle && ! grep -q "maven" build.gradle; then
        print_warning "Plugin 'maven-publish' não encontrado. Adicionando configuração temporária..."

        # Backup do build.gradle
        cp build.gradle build.gradle.backup

        # Adicionar configuração de publicação
        cat >> build.gradle << 'EOF'

// Configuração para publicação no Maven Local (adicionada automaticamente)
apply plugin: 'maven-publish'

publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
            artifact sourceJar {
                classifier 'sources'
            }
            artifact javadocJar {
                classifier 'javadoc'
            }
        }
    }
}

task sourceJar(type: Jar) {
    from sourceSets.main.allJava
    archiveClassifier = 'sources'
}

task javadocJar(type: Jar, dependsOn: javadoc) {
    from javadoc.destinationDir
    archiveClassifier = 'javadoc'
}
EOF
        print_success "Configuração de publicação adicionada (backup criado: build.gradle.backup)"
    fi
fi

# ============================================================================
# 4. BUILD DO PROJETO
# ============================================================================
print_header "🔨 PASSO 4: COMPILANDO E GERANDO JARS"

echo "📦 Limpando builds anteriores..."
$GRADLE_CMD clean

echo "📦 Compilando projeto..."
$GRADLE_CMD compileJava compileTestJava

echo "📦 Gerando JAR principal..."
$GRADLE_CMD jar

echo "📚 Gerando JAR com sources..."
$GRADLE_CMD sourceJar 2>/dev/null || {
    print_warning "Task 'sourceJar' não encontrada. Criando manualmente..."
    # Criar sources JAR manualmente
    mkdir -p build/tmp/sources
    cp -r src/main/java/* build/tmp/sources/ 2>/dev/null || true
    cd build/tmp/sources
    jar cf "../../libs/NexusFX-${VERSION}-sources.jar" *
    cd "$PROJECT_DIR"
}

echo "📖 Gerando JAR com JavaDoc..."
$GRADLE_CMD javadocJar 2>/dev/null || {
    print_warning "Task 'javadocJar' não encontrada. Criando manualmente..."
    $GRADLE_CMD javadoc
    if [ -d "build/docs/javadoc" ]; then
        cd build/docs
        jar cf "../../libs/NexusFX-${VERSION}-javadoc.jar" javadoc
        cd "$PROJECT_DIR"
    fi
}

echo "🥫 Gerando fat JAR (com dependências)..."
$GRADLE_CMD shadowJar 2>/dev/null || {
    print_warning "Plugin 'shadow' não encontrado. Pulando fat JAR."
}

# Listar JARs gerados
echo -e "\n${BLUE}JARs gerados:${NC}"
ls -la build/libs/*.jar 2>/dev/null || ls -la build/libs/ 2>/dev/null || print_warning "Nenhum JAR encontrado em build/libs/"

# ============================================================================
# 5. GERENCIAR INSTALAÇÃO NO REPOSITÓRIO LOCAL
# ============================================================================
print_header "📥 PASSO 5: GERENCIANDO INSTALAÇÃO LOCAL"

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
# 6. INSTALAR NOVA VERSÃO NO MAVEN LOCAL
# ============================================================================
print_header "🚀 PASSO 6: INSTALANDO NOVA VERSÃO"

# Encontrar o JAR principal
MAIN_JAR=$(ls build/libs/NexusFX-${VERSION}.jar 2>/dev/null | head -n1)
if [ -z "$MAIN_JAR" ]; then
    MAIN_JAR=$(ls build/libs/*.jar | grep -v sources | grep -v javadoc | grep -v shadow | head -n1)
fi

if [ -z "$MAIN_JAR" ]; then
    print_error "JAR principal não encontrado!"
    exit 1
fi

print_success "Instalando: $MAIN_JAR"

# Tentar publicar via Gradle primeiro
if $GRADLE_CMD publishToMavenLocal 2>/dev/null; then
    print_success "Publicação via Gradle bem-sucedida!"
else
    print_warning "Publicação via Gradle falhou. Usando install:install-file..."

    # Instalar via Maven (requer Maven instalado)
    if command -v mvn &> /dev/null; then
        mvn install:install-file \
            -Dfile="$MAIN_JAR" \
            -DgroupId=com.ossobo \
            -DartifactId=nexusfx \
            -Dversion="$VERSION" \
            -Dpackaging=jar \
            -DgeneratePom=true \
            -DlocalRepositoryPath="$HOME/.m2/repository"
    else
        print_error "Maven não encontrado para instalação manual"
        print_error "Instale o Maven ou configure o plugin 'maven-publish' no Gradle"
        exit 1
    fi
fi

# ============================================================================
# 7. VERIFICAÇÃO FINAL
# ============================================================================
print_header "✅ PASSO 7: VERIFICANDO INSTALAÇÃO"

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
# 8. INSTRUÇÕES DE USO
# ============================================================================
print_header "📖 COMO USAR"

echo -e "${GREEN}✅ NEXUSFX v${VERSION} INSTALADO COM SUCESSO!${NC}"
echo
echo "Para usar em outros projetos, adicione no build.gradle:"
echo
echo "    repositories {"
echo "        mavenLocal()"
echo "        mavenCentral()"
echo "    }"
echo
echo "    dependencies {"
echo "        implementation 'com.ossobo:nexusfx:${VERSION}'"
echo "    }"
echo
echo "Ou no build.gradle.kts:"
echo
echo "    repositories {"
echo "        mavenLocal()"
echo "        mavenCentral()"
echo "    }"
echo
echo "    dependencies {"
echo "        implementation(\"com.ossobo:nexusfx:${VERSION}\")"
echo "    }"
echo
echo "E não esqueça do JavaFX (seu projeto precisa incluir):"
echo
echo "    implementation 'org.openjfx:javafx-controls:25.0.2'"
echo
echo -e "${BLUE}Arquivos gerados:${NC}"
echo "  • JAR principal: build/libs/NexusFX-${VERSION}.jar"
echo "  • Sources JAR:   build/libs/NexusFX-${VERSION}-sources.jar"
echo "  • JavaDoc JAR:   build/libs/NexusFX-${VERSION}-javadoc.jar"
if [ -f "build/libs/NexusFX-${VERSION}-shadow.jar" ]; then
    echo "  • Fat JAR:       build/libs/NexusFX-${VERSION}-shadow.jar"
fi
echo
echo -e "${GREEN}========================================${NC}"

# Restaurar backup se existir
if [ -f "build.gradle.backup" ]; then
    mv build.gradle.backup build.gradle
    print_success "Arquivo build.gradle original restaurado"
fi

# ============================================================================
# FIM
# ============================================================================
exit 0