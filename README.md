# 📱 AppKardex - Sistema de Gestión de Kardex

Sistema completo para la gestión de calificaciones y registros de alumnos, maestros y administradores.

## ✨ Características

- ✅ Autenticación con Google Sign-In
- ✅ Registro de Alumnos, Maestros y Administradores
- ✅ Panel de Administrador para aprobar/rechazar solicitudes
- ✅ Panel de Maestro para poner calificaciones
- ✅ Panel de Alumno para ver calificaciones
- ✅ Base de datos en Firestore (Firebase)
- ✅ Matrícula auto-incremental
- ✅ Validación de grupos (máximo 30 alumnos)

## 🚀 Configuración Rápida

### 1. Configurar Firebase

**⚠️ IMPORTANTE:** Sigue la guía completa en: **[FIREBASE_CONFIGURATION.md](FIREBASE_CONFIGURATION.md)**

Resumen rápido:
1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/)
2. Agrega una app Android con Package name: `com.mhrc.appkardex`
3. Descarga `google-services.json` y colócalo en `app/google-services.json`
4. Obtén el SHA-1 y agrégalo a Firebase
5. Habilita Firebase Authentication con Google
6. Crea Firestore Database
7. Configura las reglas de Firestore (ver archivo de configuración)
8. Copia el Web Client ID en `app/src/main/res/values/strings.xml`

### 2. Compilar y Ejecutar

```bash
# Sincronizar proyecto con Gradle
./gradlew clean build

# Ejecutar en dispositivo/emulador
./gradlew installDebug
```

O simplemente abre el proyecto en Android Studio y presiona "Run".

## 📂 Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/mhrc/appkardex/
│   │   ├── MainActivity.java              # Entry point
│   │   ├── SplashScreen.java              # Pantalla inicial (4s)
│   │   ├── SelectionScreen.java           # Inscribirse / Ya inscrito
│   │   ├── UserTypeSelectionActivity.java # Selección de tipo usuario
│   │   ├── SignInScreen.java              # Login con Google
│   │   ├── RegisterAlumnoActivity.java    # Registro de alumno
│   │   ├── RegisterMaestroActivity.java   # Registro de maestro
│   │   ├── AdminPanelActivity.java        # Panel admin
│   │   ├── AlumnoPanelActivity.java       # Panel alumno
│   │   └── MaestroPanelActivity.java      # Panel maestro
│   ├── res/
│   │   ├── layout/                        # Layouts XML
│   │   └── values/
│   │       ├── strings.xml                # Strings (incluye Web Client ID)
│   │       ├── colors.xml                 # Colores
│   │       └── themes.xml                 # Temas
│   └── AndroidManifest.xml                # Configuración de la app
├── build.gradle                           # Dependencias
└── google-services.json                   # Configuración Firebase ⚠️ REQUERIDO

gradle/
└── libs.versions.toml                     # Versiones de librerías
```

## 🎯 Flujo de Usuario

### Registro de Alumno
1. Splash → Seleccionar "Inscribirse"
2. Seleccionar "Alumno"
3. Login con Google (obligatorio)
4. Completar formulario (nombre, carrera, grupo)
5. Matrícula auto-generada
6. Envío de solicitud a admin

### Registro de Maestro
1. Splash → Seleccionar "Inscribirse"
2. Seleccionar "Maestro"
3. Ingresar contraseña: **123456**
4. Login con Google
5. Completar formulario
6. Envío de solicitud a admin

### Registro de Admin
1. Splash → Seleccionar "Inscribirse"
2. Seleccionar "Administrador"
3. Ingresar contraseña: **7890124**
4. Login con Google
5. Redirección automática al panel

### Panel Admin
- Ver solicitudes pendientes de alumnos y maestros
- Aceptar/Rechazar solicitudes
- Asignar grupo final a alumnos
- Validar que grupo no tenga más de 30 alumnos

### Panel Maestro
- Ver grupos asignados según área académica
- Ver alumnos por grupo
- Poner calificaciones por materia
- Validación: calificaciones entre 0-100

### Panel Alumno
- Ver información personal y grupo
- Ver calificaciones del 1er Parcial
- Ver materias y calificaciones
- Botón para imprimir PDF (funcionalidad pendiente)

## 🔐 Contraseñas

- **Maestro:** `123456`
- **Administrador:** `7890124`

## 📊 Estructura de Datos (Firestore)

### Colección: `usuarios`
```json
{
  "id": "uid_firebase",
  "nombre": "Juan Pérez",
  "email": "juan@email.com",
  "rol": "alumno|maestro|admin",
  "estado": "pendiente|aprobado|rechazado",
  "fechaCreacion": "timestamp"
}
```

### Colección: `alumnos`
```json
{
  "id": "uid_firebase",
  "nombre": "Juan Pérez",
  "email": "juan@email.com",
  "matricula": 100,
  "carrera": "Programación|Ingeniería Civil|Arquitectura",
  "semestre": "1",
  "grupo": "1|2|3",
  "rol": "alumno",
  "estado": "pendiente|aprobado|rechazado",
  "parcial": "1",
  "materias": ["Materia 1", "Materia 2", "..."],
  "grupoSolicitado": "1",
  "calificaciones": {
    "Materia 1": 85,
    "Materia 2": 92
  }
}
```

### Colección: `maestros`
```json
{
  "id": "uid_firebase",
  "nombre": "María García",
  "email": "maria@email.com",
  "areaAcademica": "Programación|Ingeniería Civil|Arquitectura",
  "rol": "maestro",
  "estado": "pendiente|aprobado|rechazado",
  "materias": ["Materia 1", "Materia 2", "..."],
  "fechaCreacion": "timestamp"
}
```

## 🛠️ Tecnologías

- **Lenguaje:** Java 8
- **SDK:** Android API 24+ (Android 7.0+)
- **UI:** Material Design Components
- **Backend:** Firebase Firestore
- **Auth:** Firebase Authentication + Google Sign-In
- **Gradle:** 8.1.4

## 📝 Dependencias Principales

- Firebase BOM: `32.7.0`
- Firebase Auth
- Firebase Firestore
- Google Play Services Auth: `20.7.0`
- Material Components: `1.10.0`
- AppCompat: `1.6.1`
- ConstraintLayout: `2.1.4`

## ⚠️ Requisitos Previos

1. Android Studio Hedgehog (2023.1.1) o superior
2. JDK 8 o superior
3. Cuenta de Google (para Firebase)
4. Conexión a internet

## 🔧 Solución de Problemas

Ver sección completa en: **[FIREBASE_CONFIGURATION.md](FIREBASE_CONFIGURATION.md)** → "Solución de Problemas"

### Errores Comunes

**"google-services.json not found"**
- Verifica que el archivo esté en `app/google-services.json`

**"default_web_client_id not found"**
- Verifica `strings.xml` y asegúrate de agregar el Web Client ID real

**"SHA-1 not configured"**
- Sigue el Paso 4 de FIREBASE_CONFIGURATION.md

## 📖 Documentación Adicional

- **[FIREBASE_CONFIGURATION.md](FIREBASE_CONFIGURATION.md)** - Guía completa de configuración de Firebase
- [Firebase Documentation](https://firebase.google.com/docs)
- [Google Sign-In Guide](https://firebase.google.com/docs/auth/android/google-signin)

## 👥 Roles del Sistema

### Alumno
- Se registra con Google
- Completa formulario de registro
- Espera aprobación del admin
- Ve sus calificaciones una vez aprobado

### Maestro
- Se registra con contraseña y Google
- Completa formulario de registro
- Espera aprobación del admin
- Gestiona calificaciones de sus grupos asignados

### Administrador
- Se registra con contraseña especial y Google
- Acceso inmediato al panel de administración
- Aprueba/rechaza solicitudes
- Asigna grupos finales a alumnos

## 🎓 Carreras y Materias

### Programación
- Programación para el Desarrollo de Soluciones Móviles
- Bases de Datos
- Estructuras de Datos
- Diseño de Interfaces Gráficas

### Ingeniería Civil
- Matemáticas Aplicadas
- Estadística
- Resistencia de Materiales
- Topografía

### Arquitectura
- Dibujo Arquitectónico
- Historia de la Arquitectura
- Construcción y Tecnología
- Urbanismo

## 🚧 Funcionalidades Pendientes

- [ ] Generación de PDF para kardex de alumno
- [ ] Notificaciones push
- [ ] Carga de foto de perfil
- [ ] Historial de calificaciones (múltiples parciales)
- [ ] Dashboard de estadísticas para admin

## 📄 Licencia

Este proyecto es de uso académico.

---

**Desarrollado con ❤️ para gestión educativa**

