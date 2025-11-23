# Script para subir proyecto a GitHub
# Uso: .\subir-github.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Script de Subida a GitHub" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar si Git está instalado
try {
    $gitVersion = git --version
    Write-Host "✓ Git encontrado: $gitVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Error: Git no está instalado o no está en el PATH" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Solicitar URL del repositorio
$repoUrl = Read-Host "Ingresa la URL del repositorio de GitHub (ej: https://github.com/usuario/repo.git)"

# Validar formato de URL
if ($repoUrl -notmatch "^https://github\.com/[\w\-]+/[\w\-]+\.git$") {
    Write-Host "⚠ Advertencia: La URL no parece tener el formato correcto" -ForegroundColor Yellow
    $confirm = Read-Host "¿Deseas continuar de todas formas? (s/n)"
    if ($confirm -ne "s" -and $confirm -ne "S") {
        Write-Host "Operación cancelada." -ForegroundColor Yellow
        exit 0
    }
}

Write-Host ""
Write-Host "Repositorio configurado: $repoUrl" -ForegroundColor Cyan
$confirmRepo = Read-Host "¿Es correcto este repositorio? (s/n)"

if ($confirmRepo -ne "s" -and $confirmRepo -ne "S") {
    Write-Host "Operación cancelada." -ForegroundColor Yellow
    exit 0
}

Write-Host ""

# Solicitar mensaje de commit
$defaultMessage = "Initial commit: Sistema de reconocimiento facial con ML Kit"
Write-Host "Mensaje de commit por defecto: $defaultMessage" -ForegroundColor Gray
$customMessage = Read-Host "¿Deseas usar un mensaje personalizado? (s/n)"

if ($customMessage -eq "s" -or $customMessage -eq "S") {
    $commitMessage = Read-Host "Ingresa tu mensaje de commit"
    if ([string]::IsNullOrWhiteSpace($commitMessage)) {
        $commitMessage = $defaultMessage
        Write-Host "Usando mensaje por defecto." -ForegroundColor Yellow
    }
} else {
    $commitMessage = $defaultMessage
}

Write-Host ""
Write-Host "Mensaje de commit: $commitMessage" -ForegroundColor Cyan
Write-Host ""

# Verificar si ya es un repositorio Git
if (Test-Path ".git") {
    Write-Host "✓ Repositorio Git ya inicializado" -ForegroundColor Green
    
    # Verificar si ya tiene un remote
    $existingRemote = git remote -v 2>$null
    if ($existingRemote) {
        Write-Host ""
        Write-Host "Remote existente encontrado:" -ForegroundColor Yellow
        Write-Host $existingRemote
        $replaceRemote = Read-Host "¿Deseas reemplazar el remote existente? (s/n)"
        
        if ($replaceRemote -eq "s" -or $replaceRemote -eq "S") {
            git remote remove origin 2>$null
            Write-Host "✓ Remote anterior removido" -ForegroundColor Green
        } else {
            Write-Host "Usando remote existente." -ForegroundColor Yellow
            $repoUrl = (git remote get-url origin 2>$null)
            if (-not $repoUrl) {
                Write-Host "✗ No se pudo obtener la URL del remote" -ForegroundColor Red
                exit 1
            }
        }
    }
} else {
    Write-Host "Inicializando repositorio Git..." -ForegroundColor Yellow
    git init
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ Error al inicializar repositorio Git" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Repositorio Git inicializado" -ForegroundColor Green
}

# Agregar archivos
Write-Host ""
Write-Host "Agregando archivos al staging..." -ForegroundColor Yellow
git add .

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Error al agregar archivos" -ForegroundColor Red
    exit 1
}

# Verificar si hay cambios para commit
$status = git status --porcelain
if ([string]::IsNullOrWhiteSpace($status)) {
    Write-Host "⚠ No hay cambios para commitear" -ForegroundColor Yellow
    
    # Verificar si ya hay commits
    $commitCount = (git rev-list --count HEAD 2>$null)
    if ($commitCount -eq 0) {
        Write-Host "✗ No hay commits en el repositorio" -ForegroundColor Red
        exit 1
    } else {
        Write-Host "✓ Ya hay commits en el repositorio" -ForegroundColor Green
    }
} else {
    Write-Host "✓ Archivos agregados" -ForegroundColor Green
    
    # Hacer commit
    Write-Host ""
    Write-Host "Creando commit..." -ForegroundColor Yellow
    git commit -m $commitMessage
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ Error al crear commit" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Commit creado exitosamente" -ForegroundColor Green
}

# Configurar rama main
Write-Host ""
Write-Host "Configurando rama main..." -ForegroundColor Yellow
git branch -M main 2>$null
Write-Host "✓ Rama configurada como 'main'" -ForegroundColor Green

# Agregar remote si no existe
$currentRemote = git remote get-url origin 2>$null
if (-not $currentRemote -or ($replaceRemote -eq "s" -or $replaceRemote -eq "S")) {
    if ($currentRemote) {
        git remote set-url origin $repoUrl
    } else {
        git remote add origin $repoUrl
    }
    Write-Host "✓ Remote configurado: $repoUrl" -ForegroundColor Green
}

# Intentar hacer push
Write-Host ""
Write-Host "Intentando subir a GitHub..." -ForegroundColor Yellow
Write-Host ""

$pushResult = git push -u origin main 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  ✓ Proyecto subido exitosamente!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Repositorio: $repoUrl" -ForegroundColor Cyan
    Write-Host "Rama: main" -ForegroundColor Cyan
    Write-Host ""
} else {
    # Verificar si el error es por contenido remoto existente
    if ($pushResult -match "rejected" -or $pushResult -match "fetch first") {
        Write-Host ""
        Write-Host "⚠ El repositorio remoto contiene contenido que no tienes localmente" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Opciones:" -ForegroundColor Cyan
        Write-Host "  1. Hacer pull y merge (recomendado)" -ForegroundColor White
        Write-Host "  2. Hacer force push (sobrescribe el contenido remoto)" -ForegroundColor White
        Write-Host "  3. Cancelar" -ForegroundColor White
        Write-Host ""
        
        $option = Read-Host "Selecciona una opción (1/2/3)"
        
        switch ($option) {
            "1" {
                Write-Host ""
                Write-Host "Haciendo pull y merge..." -ForegroundColor Yellow
                git pull origin main --allow-unrelated-histories --no-edit
                
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "✓ Merge exitoso" -ForegroundColor Green
                    Write-Host ""
                    Write-Host "Subiendo cambios..." -ForegroundColor Yellow
                    git push -u origin main
                    
                    if ($LASTEXITCODE -eq 0) {
                        Write-Host ""
                        Write-Host "========================================" -ForegroundColor Green
                        Write-Host "  ✓ Proyecto subido exitosamente!" -ForegroundColor Green
                        Write-Host "========================================" -ForegroundColor Green
                    } else {
                        Write-Host "✗ Error al hacer push después del merge" -ForegroundColor Red
                        exit 1
                    }
                } else {
                    Write-Host "✗ Error al hacer merge. Puede haber conflictos." -ForegroundColor Red
                    Write-Host "Resuelve los conflictos manualmente y ejecuta: git push -u origin main" -ForegroundColor Yellow
                    exit 1
                }
            }
            "2" {
                Write-Host ""
                Write-Host "⚠ ADVERTENCIA: Esto sobrescribirá el contenido remoto" -ForegroundColor Red
                $forceConfirm = Read-Host "¿Estás seguro? (s/n)"
                
                if ($forceConfirm -eq "s" -or $forceConfirm -eq "S") {
                    Write-Host "Haciendo force push..." -ForegroundColor Yellow
                    git push -u origin main --force
                    
                    if ($LASTEXITCODE -eq 0) {
                        Write-Host ""
                        Write-Host "========================================" -ForegroundColor Green
                        Write-Host "  ✓ Proyecto subido exitosamente!" -ForegroundColor Green
                        Write-Host "========================================" -ForegroundColor Green
                    } else {
                        Write-Host "✗ Error al hacer force push" -ForegroundColor Red
                        exit 1
                    }
                } else {
                    Write-Host "Operación cancelada." -ForegroundColor Yellow
                    exit 0
                }
            }
            "3" {
                Write-Host "Operación cancelada." -ForegroundColor Yellow
                exit 0
            }
            default {
                Write-Host "Opción inválida. Operación cancelada." -ForegroundColor Red
                exit 1
            }
        }
    } else {
        Write-Host ""
        Write-Host "✗ Error al subir a GitHub:" -ForegroundColor Red
        Write-Host $pushResult
        Write-Host ""
        Write-Host "Posibles causas:" -ForegroundColor Yellow
        Write-Host "  - No tienes permisos en el repositorio" -ForegroundColor White
        Write-Host "  - El repositorio no existe" -ForegroundColor White
        Write-Host "  - Problemas de autenticación" -ForegroundColor White
        Write-Host ""
        Write-Host "Verifica tus credenciales de GitHub y los permisos del repositorio." -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host "¡Proceso completado!" -ForegroundColor Green

