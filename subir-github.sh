#!/bin/bash

# Script para subir proyecto a GitHub
# Uso: ./subir-github.sh

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  Script de Subida a GitHub${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# Verificar si Git está instalado
if ! command -v git &> /dev/null; then
    echo -e "${RED}✗ Error: Git no está instalado${NC}"
    exit 1
fi

GIT_VERSION=$(git --version)
echo -e "${GREEN}✓ Git encontrado: $GIT_VERSION${NC}"
echo ""

# Solicitar URL del repositorio
read -p "Ingresa la URL del repositorio de GitHub (ej: https://github.com/usuario/repo.git): " REPO_URL

# Validar formato de URL
if [[ ! $REPO_URL =~ ^https://github\.com/[a-zA-Z0-9_-]+/[a-zA-Z0-9_.-]+\.git$ ]]; then
    echo -e "${YELLOW}⚠ Advertencia: La URL no parece tener el formato correcto${NC}"
    read -p "¿Deseas continuar de todas formas? (s/n): " CONFIRM
    if [[ ! $CONFIRM =~ ^[sS]$ ]]; then
        echo -e "${YELLOW}Operación cancelada.${NC}"
        exit 0
    fi
fi

echo ""
echo -e "${CYAN}Repositorio configurado: $REPO_URL${NC}"
read -p "¿Es correcto este repositorio? (s/n): " CONFIRM_REPO

if [[ ! $CONFIRM_REPO =~ ^[sS]$ ]]; then
    echo -e "${YELLOW}Operación cancelada.${NC}"
    exit 0
fi

echo ""

# Solicitar mensaje de commit
DEFAULT_MESSAGE="Initial commit: Sistema de reconocimiento facial con ML Kit"
echo -e "${WHITE}Mensaje de commit por defecto: $DEFAULT_MESSAGE${NC}"
read -p "¿Deseas usar un mensaje personalizado? (s/n): " CUSTOM_MESSAGE

if [[ $CUSTOM_MESSAGE =~ ^[sS]$ ]]; then
    read -p "Ingresa tu mensaje de commit: " COMMIT_MESSAGE
    if [[ -z "$COMMIT_MESSAGE" ]]; then
        COMMIT_MESSAGE="$DEFAULT_MESSAGE"
        echo -e "${YELLOW}Usando mensaje por defecto.${NC}"
    fi
else
    COMMIT_MESSAGE="$DEFAULT_MESSAGE"
fi

echo ""
echo -e "${CYAN}Mensaje de commit: $COMMIT_MESSAGE${NC}"
echo ""

# Verificar si ya es un repositorio Git
if [ -d ".git" ]; then
    echo -e "${GREEN}✓ Repositorio Git ya inicializado${NC}"
    
    # Verificar si ya tiene un remote
    EXISTING_REMOTE=$(git remote -v 2>/dev/null)
    if [ ! -z "$EXISTING_REMOTE" ]; then
        echo ""
        echo -e "${YELLOW}Remote existente encontrado:${NC}"
        echo "$EXISTING_REMOTE"
        read -p "¿Deseas reemplazar el remote existente? (s/n): " REPLACE_REMOTE
        
        if [[ $REPLACE_REMOTE =~ ^[sS]$ ]]; then
            git remote remove origin 2>/dev/null
            echo -e "${GREEN}✓ Remote anterior removido${NC}"
        else
            echo -e "${YELLOW}Usando remote existente.${NC}"
            REPO_URL=$(git remote get-url origin 2>/dev/null)
            if [ -z "$REPO_URL" ]; then
                echo -e "${RED}✗ No se pudo obtener la URL del remote${NC}"
                exit 1
            fi
        fi
    fi
else
    echo -e "${YELLOW}Inicializando repositorio Git...${NC}"
    git init
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Error al inicializar repositorio Git${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Repositorio Git inicializado${NC}"
fi

# Agregar archivos
echo ""
echo -e "${YELLOW}Agregando archivos al staging...${NC}"
git add .

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Error al agregar archivos${NC}"
    exit 1
fi

# Verificar si hay cambios para commit
STATUS=$(git status --porcelain)
if [ -z "$STATUS" ]; then
    echo -e "${YELLOW}⚠ No hay cambios para commitear${NC}"
    
    # Verificar si ya hay commits
    COMMIT_COUNT=$(git rev-list --count HEAD 2>/dev/null)
    if [ "$COMMIT_COUNT" -eq 0 ]; then
        echo -e "${RED}✗ No hay commits en el repositorio${NC}"
        exit 1
    else
        echo -e "${GREEN}✓ Ya hay commits en el repositorio${NC}"
    fi
else
    echo -e "${GREEN}✓ Archivos agregados${NC}"
    
    # Hacer commit
    echo ""
    echo -e "${YELLOW}Creando commit...${NC}"
    git commit -m "$COMMIT_MESSAGE"
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Error al crear commit${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Commit creado exitosamente${NC}"
fi

# Configurar rama main
echo ""
echo -e "${YELLOW}Configurando rama main...${NC}"
git branch -M main 2>/dev/null
echo -e "${GREEN}✓ Rama configurada como 'main'${NC}"

# Agregar remote si no existe
CURRENT_REMOTE=$(git remote get-url origin 2>/dev/null)
if [ -z "$CURRENT_REMOTE" ] || [[ $REPLACE_REMOTE =~ ^[sS]$ ]]; then
    if [ ! -z "$CURRENT_REMOTE" ]; then
        git remote set-url origin "$REPO_URL"
    else
        git remote add origin "$REPO_URL"
    fi
    echo -e "${GREEN}✓ Remote configurado: $REPO_URL${NC}"
fi

# Intentar hacer push
echo ""
echo -e "${YELLOW}Intentando subir a GitHub...${NC}"
echo ""

git push -u origin main 2>&1
PUSH_EXIT_CODE=$?

if [ $PUSH_EXIT_CODE -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  ✓ Proyecto subido exitosamente!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${CYAN}Repositorio: $REPO_URL${NC}"
    echo -e "${CYAN}Rama: main${NC}"
    echo ""
else
    # Verificar si el error es por contenido remoto existente
    PUSH_OUTPUT=$(git push -u origin main 2>&1)
    if echo "$PUSH_OUTPUT" | grep -q "rejected\|fetch first"; then
        echo ""
        echo -e "${YELLOW}⚠ El repositorio remoto contiene contenido que no tienes localmente${NC}"
        echo ""
        echo -e "${CYAN}Opciones:${NC}"
        echo -e "${WHITE}  1. Hacer pull y merge (recomendado)${NC}"
        echo -e "${WHITE}  2. Hacer force push (sobrescribe el contenido remoto)${NC}"
        echo -e "${WHITE}  3. Cancelar${NC}"
        echo ""
        
        read -p "Selecciona una opción (1/2/3): " OPTION
        
        case $OPTION in
            1)
                echo ""
                echo -e "${YELLOW}Haciendo pull y merge...${NC}"
                git pull origin main --allow-unrelated-histories --no-edit
                
                if [ $? -eq 0 ]; then
                    echo -e "${GREEN}✓ Merge exitoso${NC}"
                    echo ""
                    echo -e "${YELLOW}Subiendo cambios...${NC}"
                    git push -u origin main
                    
                    if [ $? -eq 0 ]; then
                        echo ""
                        echo -e "${GREEN}========================================${NC}"
                        echo -e "${GREEN}  ✓ Proyecto subido exitosamente!${NC}"
                        echo -e "${GREEN}========================================${NC}"
                    else
                        echo -e "${RED}✗ Error al hacer push después del merge${NC}"
                        exit 1
                    fi
                else
                    echo -e "${RED}✗ Error al hacer merge. Puede haber conflictos.${NC}"
                    echo -e "${YELLOW}Resuelve los conflictos manualmente y ejecuta: git push -u origin main${NC}"
                    exit 1
                fi
                ;;
            2)
                echo ""
                echo -e "${RED}⚠ ADVERTENCIA: Esto sobrescribirá el contenido remoto${NC}"
                read -p "¿Estás seguro? (s/n): " FORCE_CONFIRM
                
                if [[ $FORCE_CONFIRM =~ ^[sS]$ ]]; then
                    echo -e "${YELLOW}Haciendo force push...${NC}"
                    git push -u origin main --force
                    
                    if [ $? -eq 0 ]; then
                        echo ""
                        echo -e "${GREEN}========================================${NC}"
                        echo -e "${GREEN}  ✓ Proyecto subido exitosamente!${NC}"
                        echo -e "${GREEN}========================================${NC}"
                    else
                        echo -e "${RED}✗ Error al hacer force push${NC}"
                        exit 1
                    fi
                else
                    echo -e "${YELLOW}Operación cancelada.${NC}"
                    exit 0
                fi
                ;;
            3)
                echo -e "${YELLOW}Operación cancelada.${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}Opción inválida. Operación cancelada.${NC}"
                exit 1
                ;;
        esac
    else
        echo ""
        echo -e "${RED}✗ Error al subir a GitHub${NC}"
        echo ""
        echo -e "${YELLOW}Posibles causas:${NC}"
        echo -e "${WHITE}  - No tienes permisos en el repositorio${NC}"
        echo -e "${WHITE}  - El repositorio no existe${NC}"
        echo -e "${WHITE}  - Problemas de autenticación${NC}"
        echo ""
        echo -e "${YELLOW}Verifica tus credenciales de GitHub y los permisos del repositorio.${NC}"
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}¡Proceso completado!${NC}"

