# Instrucciones para Subir Proyecto a GitHub

Este documento explica cómo usar los scripts automatizados para subir tu proyecto a GitHub.

## Scripts Disponibles

### Windows (PowerShell)
- **Archivo**: `subir-github.ps1`
- **Uso**: Ejecutar en PowerShell

### Linux/Mac (Bash)
- **Archivo**: `subir-github.sh`
- **Uso**: Ejecutar en terminal bash

## Requisitos Previos

1. **Git instalado**: Verifica que Git esté instalado en tu sistema
2. **Cuenta de GitHub**: Necesitas una cuenta de GitHub activa
3. **Repositorio creado**: El repositorio debe existir en GitHub (puede estar vacío)

## Uso del Script (Windows)

### Paso 1: Abrir PowerShell
1. Abre PowerShell en la carpeta del proyecto
2. O navega a la carpeta del proyecto:
   ```powershell
   cd "C:\Users\User\Downloads\AppKardex (3)\kardex"
   ```

### Paso 2: Ejecutar el Script
```powershell
.\subir-github.ps1
```

### Paso 3: Seguir las Instrucciones
El script te preguntará:
1. **URL del repositorio**: Ingresa la URL completa (ej: `https://github.com/SupremCuervo/AppKardex.git`)
2. **Confirmación**: Confirma que la URL es correcta
3. **Mensaje de commit**: Elige usar el mensaje por defecto o personalizado
4. **Manejo de conflictos**: Si el repositorio remoto tiene contenido, elige cómo manejarlo

## Uso del Script (Linux/Mac)

### Paso 1: Dar Permisos de Ejecución
```bash
chmod +x subir-github.sh
```

### Paso 2: Ejecutar el Script
```bash
./subir-github.sh
```

### Paso 3: Seguir las Instrucciones
Igual que en Windows, el script te guiará paso a paso.

## Opciones cuando el Repositorio Remoto tiene Contenido

Si el repositorio remoto ya tiene contenido (como un README.md), el script te ofrecerá 3 opciones:

### Opción 1: Pull y Merge (Recomendado)
- Descarga el contenido remoto
- Hace merge con tu contenido local
- Mantiene el historial de ambos
- **Usa esta opción si quieres conservar el contenido remoto**

### Opción 2: Force Push
- Sobrescribe completamente el contenido remoto
- **⚠️ ADVERTENCIA**: Esto eliminará todo el contenido remoto
- **Usa esta opción solo si estás seguro de que quieres reemplazar todo**

### Opción 3: Cancelar
- Cancela la operación
- Puedes resolver los conflictos manualmente después

## Ejemplo de Uso Completo

```powershell
# En PowerShell (Windows)
PS C:\Users\User\Downloads\AppKardex (3)\kardex> .\subir-github.ps1

========================================
  Script de Subida a GitHub
========================================

✓ Git encontrado: git version 2.40.0

Ingresa la URL del repositorio de GitHub (ej: https://github.com/usuario/repo.git): 
https://github.com/SupremCuervo/AppKardex.git

Repositorio configurado: https://github.com/SupremCuervo/AppKardex.git
¿Es correcto este repositorio? (s/n): s

Mensaje de commit por defecto: Initial commit: Sistema de reconocimiento facial con ML Kit
¿Deseas usar un mensaje personalizado? (s/n): n

Mensaje de commit: Initial commit: Sistema de reconocimiento facial con ML Kit

Inicializando repositorio Git...
✓ Repositorio Git inicializado
Agregando archivos al staging...
✓ Archivos agregados
Creando commit...
✓ Commit creado exitosamente
Configurando rama main...
✓ Rama configurada como 'main'
Remote configurado: https://github.com/SupremCuervo/AppKardex.git
Intentando subir a GitHub...

========================================
  ✓ Proyecto subido exitosamente!
========================================

Repositorio: https://github.com/SupremCuervo/AppKardex.git
Rama: main

¡Proceso completado!
```

## Solución de Problemas

### Error: "Git no está instalado"
**Solución**: Instala Git desde [git-scm.com](https://git-scm.com/)

### Error: "No tienes permisos en el repositorio"
**Solución**: 
1. Verifica que tengas acceso de escritura al repositorio
2. Verifica tus credenciales de GitHub
3. Si usas HTTPS, puede que necesites un token de acceso personal

### Error: "El repositorio no existe"
**Solución**: 
1. Verifica que la URL sea correcta
2. Asegúrate de que el repositorio exista en GitHub
3. Verifica que tengas acceso al repositorio

### Error: "Problemas de autenticación"
**Solución**:
1. Configura tus credenciales de Git:
   ```bash
   git config --global user.name "Tu Nombre"
   git config --global user.email "tu@email.com"
   ```
2. Si usas HTTPS, GitHub requiere un token de acceso personal en lugar de contraseña
3. Genera un token en: GitHub → Settings → Developer settings → Personal access tokens

### Error: "No se puede hacer push"
**Solución**:
1. Verifica tu conexión a internet
2. Verifica que el repositorio remoto exista
3. Intenta hacer pull primero: `git pull origin main --allow-unrelated-histories`

## Comandos Manuales (Si el Script Falla)

Si prefieres hacerlo manualmente:

```bash
# 1. Inicializar repositorio (si no está inicializado)
git init

# 2. Agregar archivos
git add .

# 3. Crear commit
git commit -m "Initial commit: Sistema de reconocimiento facial con ML Kit"

# 4. Configurar rama main
git branch -M main

# 5. Agregar remote
git remote add origin https://github.com/SupremCuervo/AppKardex.git

# 6. Hacer push
git push -u origin main
```

Si el repositorio remoto tiene contenido:
```bash
# Opción 1: Pull y merge
git pull origin main --allow-unrelated-histories
git push -u origin main

# Opción 2: Force push (cuidado!)
git push -u origin main --force
```

## Notas Importantes

1. **No subas archivos sensibles**: El archivo `google-services.json` contiene información sensible. Considera agregarlo al `.gitignore` o usar variables de entorno.

2. **`.gitignore`**: El proyecto ya tiene un `.gitignore` configurado que excluye:
   - Archivos de build
   - Archivos locales
   - Archivos de IDE

3. **Rama principal**: El script usa la rama `main`. Si tu repositorio usa `master`, puedes cambiarlo después con:
   ```bash
   git branch -M master
   git push -u origin master
   ```

## Próximos Pasos

Después de subir tu proyecto:

1. **Verifica en GitHub**: Visita tu repositorio y confirma que todos los archivos estén ahí
2. **Actualiza el README**: Asegúrate de que el README.md tenga información actualizada
3. **Configura GitHub Actions** (opcional): Para CI/CD automático
4. **Agrega un LICENSE** (opcional): Si quieres especificar la licencia del proyecto

## Soporte

Si encuentras problemas:
1. Revisa los mensajes de error del script
2. Verifica la documentación de Git: [git-scm.com/docs](https://git-scm.com/docs)
3. Verifica la documentación de GitHub: [docs.github.com](https://docs.github.com)

