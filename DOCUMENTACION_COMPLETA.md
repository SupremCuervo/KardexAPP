# 📚 DOCUMENTACIÓN COMPLETA - AppKardex

## 📋 Tabla de Contenidos

1. [Descripción General](#descripción-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Flujo de Navegación](#flujo-de-navegación)
4. [Pantallas y Funcionalidades](#pantallas-y-funcionalidades)
5. [Lógica de Negocio](#lógica-de-negocio)
6. [Estructura de Datos](#estructura-de-datos)
7. [Sistema de Materias](#sistema-de-materias)
8. [Sistema de Calificaciones](#sistema-de-calificaciones)
9. [Validaciones](#validaciones)

---

## 📖 Descripción General

**AppKardex** es una aplicación móvil Android desarrollada en Java que gestiona el sistema de registro y control académico para una institución educativa. Permite el registro de tres tipos de usuarios:

- **Alumnos**: Se registran, ven sus calificaciones y generan PDFs de su kardex
- **Maestros**: Se registran, ven grupos asignados y califican alumnos
- **Administradores**: Gestionan registros pendientes y asignan grupos/materias a maestros

La aplicación utiliza **Firebase** para autenticación y almacenamiento de datos (Firestore).

---

## 🏗️ Arquitectura del Sistema

### Stack Tecnológico

- **Lenguaje**: Java 8
- **UI Framework**: Android Views (no Jetpack Compose)
- **Material Design**: Material Components
- **Backend**: Firebase Firestore
- **Autenticación**: Firebase Authentication + Google Sign-In
- **SDK Mínimo**: Android 7.0 (API 24)
- **SDK Target**: Android 14 (API 34)

### Estructura de Paquetes

```
com.mhrc.appkardex/
├── MainActivity.java                 # Punto de entrada, redirige a SplashScreen
├── SplashScreen.java                 # Pantalla de inicio con delay
├── SelectionScreen.java              # Pantalla de selección (Login/Registro)
├── UserTypeSelectionActivity.java    # Selección de tipo de usuario (Alumno/Maestro/Admin)
├── SignInScreen.java                 # Autenticación con Google Sign-In
├── RegisterAlumnoActivity.java       # Formulario de registro de alumnos
├── RegisterMaestroActivity.java      # Formulario de registro de maestros
├── AdminPanelActivity.java           # Panel de administración
├── AlumnoPanelActivity.java         # Panel del alumno
├── MaestroPanelActivity.java         # Panel del maestro
└── MateriasHelper.java               # Helper para gestión de materias por semestre
```

---

## 🗺️ Flujo de Navegación

```
MainActivity
    ↓
SplashScreen (2 segundos)
    ↓
SelectionScreen
    ├── "Iniciar Sesión" → SignInScreen → [Según rol y estado]
    │                                           ├→ AdminPanelActivity (si admin aprobado)
    │                                           ├→ AlumnoPanelActivity (si alumno aprobado)
    │                                           ├→ MaestroPanelActivity (si maestro aprobado)
    │                                           └→ RegisterAlumnoActivity/MaestroActivity (si pendiente)
    │
    └── "Inscribirse" → UserTypeSelectionActivity
                          ├→ Alumno → SignInScreen → RegisterAlumnoActivity
                          ├→ Maestro → SignInScreen → RegisterMaestroActivity
                          └→ Admin → SignInScreen → AdminPanelActivity (requiere contraseña)
```

---

## 🖥️ Pantallas y Funcionalidades

### 1. **MainActivity**

**Ubicación**: `MainActivity.java`

**Propósito**: Punto de entrada de la aplicación.

**Lógica**:
- Al iniciar, redirige inmediatamente a `SplashScreen`
- Cierra la actividad actual con `finish()`

**Código Clave**:
```java
Intent intent = new Intent(this, SplashScreen.class);
startActivity(intent);
finish();
```

---

### 2. **SplashScreen**

**Ubicación**: `SplashScreen.java`  
**Layout**: `activity_splash.xml`

**Propósito**: Mostrar una pantalla de bienvenida con el logo de la aplicación.

**Funcionalidades**:
- Muestra el título "Cardex" y el logo
- Espera 2 segundos antes de redirigir
- Redirige a `SelectionScreen`

**Lógica**:
```java
new Handler(Looper.getMainLooper()).postDelayed(() -> {
    Intent intent = new Intent(SplashScreen.this, SelectionScreen.class);
    startActivity(intent);
    finish();
}, 2000); // 2 segundos
```

---

### 3. **SelectionScreen**

**Ubicación**: `SelectionScreen.java`  
**Layout**: `activity_selection.xml`

**Propósito**: Pantalla principal donde el usuario decide si iniciar sesión o registrarse.

**Elementos UI**:
- Título "Cardex" en rosa/accent, tamaño 64sp
- Logo/imagen de la app (ImageView con ic_launcher)
- Botón "Iniciar Sesión" (gris, texto negro)
- Texto "Inscribirse" (azul, subrayado, estilo link)

**Funcionalidades**:
- Botón "Iniciar Sesión" → Navega a `SignInScreen`
- Texto "Inscribirse" → Navega a `UserTypeSelectionActivity`

**Lógica**:
- El texto "Inscribirse" usa `SpannableString` con `UnderlineSpan` para el subrayado
- Los botones tienen estilos personalizados (drawable para fondo gris)

---

### 4. **UserTypeSelectionActivity**

**Ubicación**: `UserTypeSelectionActivity.java`  
**Layout**: `activity_user_type_selection.xml`

**Propósito**: Permite al usuario seleccionar su tipo (Alumno, Maestro, Admin).

**Funcionalidades**:
- Tres botones: "Alumno", "Maestro", "Administrador"
- Navega a `SignInScreen` pasando el tipo seleccionado como extra del Intent
- Si selecciona "Administrador", se valida contraseña antes de continuar

**Lógica**:
```java
Intent intent = new Intent(UserTypeSelectionActivity.this, SignInScreen.class);
intent.putExtra("userType", "alumno"); // o "maestro", "admin"
startActivity(intent);
```

**Validación de Contraseña Admin**:
- Contraseña hardcodeada: `"7890124"` (definida en strings.xml)
- Si es incorrecta, muestra Toast y no continúa

---

### 5. **SignInScreen**

**Ubicación**: `SignInScreen.java`  
**Layout**: `activity_sign_in.xml`

**Propósito**: Gestiona la autenticación con Google Sign-In y determina el flujo posterior.

**Funcionalidades**:
1. **Google Sign-In**:
   - Configura `GoogleSignInClient` con Web Client ID de Firebase
   - Fuerza la selección de cuenta (logout previo en `onCreate`)
   - Maneja el resultado de la autenticación

2. **Verificación de Usuario**:
   - Consulta Firestore en la colección `usuarios`
   - Verifica si el usuario existe y su estado

3. **Lógica de Redirección**:
   - **Si es nuevo usuario** y `userType != "login"`:
     - Redirige al formulario de registro correspondiente (`RegisterAlumnoActivity` o `RegisterMaestroActivity`)
   - **Si el usuario existe**:
     - Verifica el `rol` y el `estado`
     - Si está `aprobado` → Redirige al panel correspondiente
     - Si está `pendiente` → Muestra mensaje y redirige al formulario de registro

**Flujo Detallado**:
```
1. Usuario presiona "Continuar con Google"
2. Se abre selector de cuenta de Google
3. onActivityResult recibe el resultado
4. Si éxito:
   - Obtiene cuenta de Google
   - Autentica con Firebase
   - Consulta colección "usuarios"
   - Si no existe:
     → Si userType == "alumno" → RegisterAlumnoActivity
     → Si userType == "maestro" → RegisterMaestroActivity
     → Si userType == "admin" → AdminPanelActivity
   - Si existe:
     → Verifica rol y estado
     → Si aprobado → Panel correspondiente
     → Si pendiente → Formulario de registro
```

**Manejo de Errores**:
- `RESULT_CANCELED`: No muestra error, permite reintentar
- `ApiException`: Muestra mensaje específico del error
- Excepciones de Firestore: Muestra mensaje y registra en Logcat

**Importante**:
- En `onCreate` se hace `signOut()` para forzar selección de cuenta
- Se valida que el Web Client ID esté configurado en `strings.xml`

---

### 6. **RegisterAlumnoActivity**

**Ubicación**: `RegisterAlumnoActivity.java`  
**Layout**: `activity_register_alumno.xml`

**Propósito**: Formulario de registro de alumnos con validación de semestres.

**Elementos UI**:
- Título "Registro del Alumno" (rojo, negrita, centrado)
- Imagen del alumno (ic_alumno)
- Campo Nombre (editable)
- Campo Email (no editable, viene de Google)
- Campo Matrícula (no editable, auto-incremental)
- Spinner Carrera (Programación, Ingeniería Civil, Arquitectura)
- Lista de Materias (solo lectura, se muestra según carrera y semestre)
- TextView Semestre (dinámico, muestra semestre actual)
- Spinner Grupo (1, 2, 3)
- Texto "Eres de semestre X" (verde)
- Botón Registrar (gris, texto negro)

**Funcionalidades**:

#### 6.1. **Verificación de Alumno Existente**
```java
verificarAlumnoExistente()
```
- Consulta Firestore en `alumnos/{uid}`
- Si existe:
  - Obtiene el semestre actual del documento
  - Calcula el siguiente semestre: `semestreAnterior + 1`
  - **Validación crítica**: Verifica que todas las materias del semestre anterior tengan promedio >= 6
  - Si no cumple → Muestra mensaje y cierra la actividad
  - Si cumple → Actualiza `semestreActual` y muestra las materias correspondientes
- Si no existe:
  - `semestreActual = 1` (nuevo alumno)

#### 6.2. **Visualización de Materias**
```java
mostrarMaterias(String carrera, int semestre)
```
- Obtiene materias usando `MateriasHelper.getMateriasPorSemestre()`
- Muestra las materias en formato de lista con bullets (•)
- Se actualiza automáticamente al cambiar el spinner de carrera

#### 6.3. **Registro del Alumno**
```java
registerAlumno()
```
**Validaciones**:
1. Nombre no vacío
2. Carrera seleccionada
3. Grupo seleccionado
4. Matrícula válida (numérica)

**Datos Guardados en Firestore**:
```json
{
  "id": "uid_firebase",
  "nombre": "Juan Pérez",
  "email": "juan@email.com",
  "matricula": 101,
  "carrera": "Programación",
  "semestre": "2",
  "grupo": null,  // Admin lo asignará después
  "grupoSolicitado": "1",
  "rol": "alumno",
  "estado": "pendiente",
  "parcial": "1",
  "materias": ["Materia1", "Materia2", ...],  // Del semestre actual
  "calificaciones": {
    "Materia1": {
      "parcial1": null,
      "parcial2": null,
      "parcial3": null
    },
    ...
  }
}
```

**Proceso**:
1. Valida campos
2. Obtiene materias del semestre actual
3. Inicializa estructura de calificaciones (3 parciales por materia)
4. Guarda en colección `alumnos`
5. También guarda en colección `usuarios` con estado "pendiente"
6. Muestra mensaje de éxito y cierra la actividad

**Matrícula Auto-incremental**:
```java
setupMatricula()
```
- Consulta todos los documentos en `alumnos`
- Encuentra la matrícula máxima
- La nueva matrícula = maxMatricula + 1
- Si no hay alumnos previos, empieza en 100

---

### 7. **RegisterMaestroActivity**

**Ubicación**: `RegisterMaestroActivity.java`  
**Layout**: `activity_register_maestro.xml`

**Propósito**: Formulario de registro de maestros.

**Elementos UI**:
- Campo Nombre (editable)
- Campo Email (no editable)
- Spinner Área Académica (Programación, Ingeniería Civil, Arquitectura)
- Checkboxes de Materias (dinámicos, según área seleccionada)
- Botón Registrar

**Funcionalidades**:

#### 7.1. **Materias Dinámicas**
```java
actualizarMaterias()
```
- Al seleccionar área académica, se muestran checkboxes con las materias de esa área
- El usuario puede seleccionar múltiples materias
- Las materias se obtienen de `getMateriasByArea()`

**Validación**: Al menos una materia debe estar seleccionada

#### 7.2. **Registro**
```java
registerMaestro()
```
**Datos Guardados**:
```json
{
  "id": "uid_firebase",
  "nombre": "María García",
  "email": "maria@email.com",
  "areaAcademica": "Programación",
  "materias": ["Materia1", "Materia2", ...],  // Las seleccionadas
  "rol": "maestro",
  "estado": "pendiente"
}
```

**Proceso**:
1. Valida nombre y que haya al menos una materia seleccionada
2. Guarda en colección `maestros`
3. Guarda en colección `usuarios`
4. Muestra mensaje: "Espera a que seas autorizado. Gracias por tu registro"

---

### 8. **AdminPanelActivity**

**Ubicación**: `AdminPanelActivity.java`  
**Layout**: `activity_admin_panel.xml`

**Propósito**: Panel de administración para gestionar alumnos y maestros pendientes.

**Elementos UI**:
- Tabs: "Alumnos" y "Maestros"
- Spinner de Carrera (solo en tab Alumnos, para filtrar)
- Lista scrollable de solicitudes pendientes

**Funcionalidades**:

#### 8.1. **Gestión de Alumnos**

**Carga de Alumnos Pendientes**:
```java
loadPendingAlumnos()
```
- Consulta `alumnos` donde `estado == "pendiente"`
- Si hay filtro de carrera, aplica filtro adicional
- Muestra para cada alumno:
  - Nombre, Email, Matrícula
  - Carrera, Grupo Solicitado
  - **Lista completa de materias** con maestros asignados (o "No hay maestro aún")
  - Botones "Aceptar" y "Rechazar"

**Aceptar Alumno**:
```java
aceptarAlumno(String uid, String carrera, String grupoSolicitado)
```
**Lógica**:
1. Valida que el grupo no tenga más de 30 alumnos
   - Consulta `alumnos` donde `carrera == carrera` y `grupo == grupoSolicitado` y `estado == "aprobado"`
   - Si count >= 30 → Muestra error y no permite
2. Si el grupo tiene espacio:
   - Actualiza el documento del alumno:
     - `estado = "aprobado"`
     - `grupo = grupoSolicitado` (grupo final asignado)
   - Actualiza el documento en `usuarios`
   - Llama a `asignarMaestrosAAlumno()` para asignar maestros automáticamente

**Asignar Maestros a Alumno**:
```java
asignarMaestrosAAlumno(String uidAlumno, String carrera, String grupo)
```
**Lógica**:
1. Busca maestros en `maestros` donde:
   - `carreraAsignada == carrera`
   - `grupoAsignado == grupo` (para compatibilidad con sistema anterior)
   - `estado == "aprobado"`
2. Para cada materia del alumno:
   - Inicializa `maestrosAsignados[materia] = "No hay maestro aún"`
3. Si encuentra maestros asignados:
   - Actualiza las materias correspondientes con el nombre del maestro

**Rechazar Alumno**:
```java
rechazarAlumno(String uid)
```
- Actualiza `estado = "rechazado"` en `alumnos` y `usuarios`
- Elimina el documento del alumno

#### 8.2. **Gestión de Maestros**

**Carga de Maestros Pendientes**:
```java
loadPendingMaestros()
```
- Consulta `maestros` donde `estado == "pendiente"`
- Muestra para cada maestro:
  - Nombre, Email
  - Área Académica
  - **Lista completa de materias** que puede impartir
  - El nombre es clickeable para ver detalles

**Ver Detalles de Maestro**:
```java
showMaestroDetailsDialog(DocumentSnapshot doc)
```
- Muestra diálogo con toda la información del maestro
- Botón "Asignar Grupo y Materia"

**Asignar Grupo y Materia a Maestro**:
```java
showAsignarGrupoMateriaDialog(String uidMaestro, String carrera)
```

**Lógica Compleja**:

1. **Obtención de Disponibilidad**:
   ```java
   actualizarDisponibilidadMaterias(List<String> gruposSeleccionados)
   ```
   - Consulta TODOS los maestros aprobados de esa carrera
   - Para cada materia y grupo seleccionado, verifica si ya hay un maestro asignado
   - Marca materias como "Ocupada en Grupo X" si están asignadas

2. **Validación de Conflictos**:
   ```java
   validarYAsignarMaestro(...)
   ```
   - Verifica que no haya otro maestro asignado a la misma combinación (grupo + materia)
   - Si hay conflicto → Muestra lista de conflictos y NO permite asignación
   - Si no hay conflicto → Llama a `asignarGruposMateriasMaestro()`

3. **Asignación**:
   ```java
   asignarGruposMateriasMaestro(...)
   ```
   **Datos Guardados en `maestros`**:
   ```json
   {
     "gruposAsignados": ["1", "2"],
     "materiasAsignadas": ["Materia1", "Materia2"],
     "carreraAsignada": "Programación",
     "asignaciones": [
       {"grupo": "1", "materia": "Materia1"},
       {"grupo": "1", "materia": "Materia2"},
       {"grupo": "2", "materia": "Materia1"}
     ]
   }
   ```

4. **Actualización de Alumnos**:
   ```java
   actualizarAlumnosConMaestro(String carrera, String grupo, String materia, String uidMaestro)
   ```
   - Busca TODOS los alumnos de esa carrera y grupo
   - Para cada alumno que tenga esa materia:
     - Inicializa todas sus materias con "No hay maestro aún" (si no existen)
     - Actualiza la materia específica con el nombre del maestro: `"Nombre Maestro (uid)"`

**Rechazar Maestro**:
```java
rechazarMaestro(String uid)
```
- Similar a rechazar alumno

---

### 9. **AlumnoPanelActivity**

**Ubicación**: `AlumnoPanelActivity.java`  
**Layout**: `activity_alumno_panel.xml`

**Propósito**: Panel del alumno para ver información y calificaciones.

**Elementos UI**:
- Información del alumno (nombre, grupo, carrera, matrícula)
- Lista de calificaciones por materia (con 3 parciales)
- Botón "Imprimir PDF"
- Botón "Cerrar Sesión"

**Funcionalidades**:

#### 9.1. **Carga de Datos**
```java
loadAlumnoData()
```
- Consulta `alumnos/{uid}` en Firestore
- Extrae:
  - Información personal
  - Lista de materias
  - Calificaciones (con compatibilidad hacia atrás)
  - Maestros asignados

**Compatibilidad con Estructura Antigua**:
- Si las calificaciones vienen como `Map<String, Long>` (una sola calificación por materia):
  - Convierte a estructura nueva: `parcial1 = calificacion, parcial2 = null, parcial3 = null`

#### 9.2. **Visualización de Calificaciones**
```java
displayCalificaciones(List<String> materias, Map<String, Map<String, Long>> calificaciones)
```
- Muestra para cada materia:
  - Nombre de la materia
  - **Parcial 1, Parcial 2, Parcial 3** (o "-" si no hay calificación)
  - **Promedio calculado automáticamente**

**Formato de Visualización**:
```
P1: 85 | P2: 90 | P3: 88
Promedio: 87.7
```

#### 9.3. **Generación de PDF**
```java
imprimirPDF()
```

**Proceso Completo**:

1. **Validación de Datos**:
   - Verifica que existan datos del alumno cargados

2. **Creación del Documento PDF** (usando iTextPDF):
   - Crea documento con tamaño A4
   - Configura márgenes

3. **Contenido del PDF**:
   - **Encabezado**: "KARDEX ACADÉMICO"
   - **Información del Alumno**:
     - Nombre, Matrícula, Carrera, Grupo, Semestre, Parcial
   - **Tabla de Calificaciones**:
     - Columnas: Materia | Maestro | Calificación
     - Cada materia muestra: P1, P2, P3 y Promedio
   - **Pie de Página**: Fecha de generación

4. **Guardado del Archivo**:
   - **Ubicación Preferida**: `getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)/AppKardex/`
   - **Fallback**: `getFilesDir()/AppKardex/`
   - **Nombre del Archivo**: `Kardex_{matricula}_{timestamp}.pdf`

5. **Apertura del PDF**:
   - Usa `FileProvider` para Android 7.0+ (seguro)
   - Crea Intent con `ACTION_VIEW`
   - Permite al usuario abrir con cualquier app de PDF

**Manejo de Errores**:
- Si falla la generación → Toast con mensaje de error
- Si falla la apertura → Toast pero el archivo se guarda igualmente

#### 9.4. **Cerrar Sesión**
```java
logout()
```
- Cierra sesión de Firebase
- Cierra sesión de Google Sign-In
- Redirige a `SplashScreen` con flags que limpian el back stack
- No permite volver atrás después del logout

---

### 10. **MaestroPanelActivity**

**Ubicación**: `MaestroPanelActivity.java`  
**Layout**: `activity_maestro_panel.xml`

**Propósito**: Panel del maestro para ver grupos asignados y calificar alumnos.

**Elementos UI**:
- Nombre del maestro
- **Spinner Carrera** (Programación, Ingeniería Civil, Arquitectura)
- **Spinner Parcial** (Parcial 1, Parcial 2, Parcial 3)
- **Spinner Materia** (solo si tiene múltiples materias asignadas)
- Lista de grupos asignados
- Botón "Cerrar Sesión"

**Funcionalidades**:

#### 10.1. **Carga de Datos del Maestro**
```java
loadMaestroData()
```
- Consulta `maestros/{uid}`
- Extrae con compatibilidad hacia atrás:
  - `gruposAsignados` (puede venir como String o List<String>)
  - `materiasAsignadas` (puede venir como String o List<String>)
  - `carreraAsignada`

**Lógica de Compatibilidad**:
- Si `gruposAsignados` es String → Convierte a List
- Si `materiasAsignadas` es String → Convierte a List
- Todo se convierte a `String` para manejo uniforme

#### 10.2. **Configuración de UI**
```java
setupUI()
```

**Spinner de Carrera**:
- Permite seleccionar cualquier carrera (aunque el maestro esté asignado a una específica)
- Al cambiar, recarga los grupos

**Spinner de Parcial**:
- Permite seleccionar Parcial 1, 2 o 3
- Al cambiar, actualiza la visualización de calificaciones
- Guarda el valor como: `"parcial1"`, `"parcial2"`, `"parcial3"`

**Spinner de Materia**:
- Solo se muestra si el maestro tiene más de 1 materia asignada
- Si tiene solo 1, se oculta y se usa automáticamente esa materia

#### 10.3. **Visualización de Grupos**
```java
loadGruposAsignados()
```
- Muestra solo los grupos asignados al maestro
- Cada grupo muestra: "Grupo X - [Materia seleccionada]"
- Al hacer click en un grupo → `mostrarAlumnosGrupo()`

#### 10.4. **Visualización de Alumnos**
```java
mostrarAlumnosGrupo(String grupo, ViewGroup container)
```

**Lógica**:
1. Limpia el contenedor
2. Crea botón "Volver" para regresar a la lista de grupos
3. Consulta `alumnos` donde:
   - `grupo == grupo`
   - `carrera == carreraSeleccionada`
   - `estado == "aprobado"`
4. Filtra alumnos que tengan la `materiaSeleccionada` en su lista de materias
5. Para cada alumno:
   - Muestra nombre y matrícula
   - **Muestra calificación del parcial seleccionado** (si existe)
   - Ejemplo: "Nombre: Juan - P1: 85" o "Nombre: Juan - P1: Sin calificación"
6. Al hacer click en un alumno → `mostrarDialogCalificaciones()`

#### 10.5. **Diálogo de Calificaciones**
```java
mostrarDialogCalificaciones(String uid, DocumentSnapshot doc, String grupo)
```

**Funcionalidades**:
1. **Título Dinámico**:
   - "Poner Calificación - Parcial X" (si no hay calificación)
   - "Modificar Parcial X (Actual: 85)" (si hay calificación)

2. **Obtención de Calificación Existente**:
   - Obtiene la calificación del parcial seleccionado
   - Con compatibilidad hacia atrás (si viene estructura antigua, convierte)

3. **Campo de Edición**:
   - Pre-llena con la calificación existente (si hay)
   - Selecciona todo el texto para facilitar edición

4. **Validación**:
   - Calificación debe estar entre 0 y 100
   - Debe ser un número válido

5. **Guardado**:
   ```java
   guardarCalificaciones(String uid, Map<String, Map<String, Long>> calificaciones, String grupo)
   ```
   - Actualiza SOLO el parcial seleccionado
   - Mantiene los otros parciales intactos
   - Estructura guardada:
     ```json
     {
       "Materia1": {
         "parcial1": 85,
         "parcial2": 90,
         "parcial3": null
       }
     }
     ```
   - Después de guardar, recarga la lista de alumnos para mostrar la calificación actualizada

---

## 🧠 Lógica de Negocio

### Sistema de Semestres

**Responsable**: `MateriasHelper.java` y `RegisterAlumnoActivity.java`

**Lógica**:
1. Al registrar un alumno nuevo → `semestre = 1`
2. Si el alumno ya existe:
   - Se obtiene el semestre actual del documento
   - El siguiente semestre = `semestreActual + 1`
   - Se valida que no sea mayor a 4

**Validación de Aprobación**:
```java
MateriasHelper.validarAprobacionSemestreAnterior(carrera, semestreActual, calificaciones)
```
- Para cada materia del semestre anterior:
  - Obtiene los 3 parciales
  - Calcula el promedio: `(parcial1 + parcial2 + parcial3) / cantidad de parciales`
  - Si el promedio < 6.0 → Retorna `false` (no puede avanzar)
- Si todas las materias tienen promedio >= 6 → Retorna `true` (puede avanzar)

### Sistema de Materias por Semestre

**Responsable**: `MateriasHelper.java`

**Estructura**:
```
Map<String, Map<Integer, List<String>>>
  └─ "Programación" → {
       1 → ["Materia1", "Materia2", ...],
       2 → ["Materia1", "Materia2", ...],
       3 → [...],
       4 → [...]
     }
  └─ "Ingeniería Civil" → {...}
  └─ "Arquitectura" → {...}
```

**Materias Definidas**:

#### Programación:
- **Semestre 1**: 4 materias
- **Semestre 2**: 5 materias (POO, Cálculo Avanzado, Estructuras Discretas, Física General, Bases de Datos I)
- **Semestre 3**: 5 materias (Análisis y Diseño de Algoritmos, Bases de Datos II, Cálculo III, Arquitectura de Computadoras, Ingeniería de Software I)
- **Semestre 4**: 5 materias (Sistemas Operativos, Redes de Computadoras, Estructuras de Datos II, Álgebra Lineal, Desarrollo Web)

#### Ingeniería Civil:
- **Semestre 1**: 4 materias
- **Semestre 2**: 5 materias
- **Semestre 3**: 5 materias
- **Semestre 4**: 5 materias

#### Arquitectura:
- **Semestre 1**: 4 materias
- **Semestre 2**: 5 materias
- **Semestre 3**: 5 materias
- **Semestre 4**: 5 materias

### Sistema de Calificaciones

**Estructura de Datos**:
```json
{
  "calificaciones": {
    "Materia1": {
      "parcial1": 85,
      "parcial2": 90,
      "parcial3": 88
    },
    "Materia2": {
      "parcial1": 92,
      "parcial2": null,
      "parcial3": null
    }
  }
}
```

**Compatibilidad hacia atrás**:
- Si viene estructura antigua `{"Materia1": 85}`:
  - Se convierte automáticamente a `{"Materia1": {"parcial1": 85, "parcial2": null, "parcial3": null}}`

**Cálculo de Promedio**:
```java
promedio = (parcial1 + parcial2 + parcial3) / cantidad_de_parciales_con_valor
```
- Solo se promedian los parciales que tienen valor (no null)
- Si hay 2 parciales → promedio de 2
- Si hay 3 parciales → promedio de 3

---

## 💾 Estructura de Datos

### Colección: `usuarios`

**Propósito**: Tabla maestra de usuarios (todos los tipos).

```json
{
  "id": "uid_firebase",
  "nombre": "Juan Pérez",
  "email": "juan@email.com",
  "rol": "alumno|maestro|admin",
  "estado": "pendiente|aprobado|rechazado"
}
```

### Colección: `alumnos`

```json
{
  "id": "uid_firebase",
  "nombre": "Juan Pérez",
  "email": "juan@email.com",
  "matricula": 101,
  "carrera": "Programación|Ingeniería Civil|Arquitectura",
  "semestre": "1|2|3|4",
  "grupo": "1|2|3",  // Asignado por admin, null si pendiente
  "grupoSolicitado": "1",  // El grupo que el alumno solicitó
  "rol": "alumno",
  "estado": "pendiente|aprobado|rechazado",
  "parcial": "1|2|3",  // Parcial actual (generalmente 1)
  "materias": ["Materia1", "Materia2", ...],  // Materias del semestre actual
  "calificaciones": {
    "Materia1": {
      "parcial1": 85,
      "parcial2": 90,
      "parcial3": 88
    },
    ...
  },
  "maestrosAsignados": {
    "Materia1": "Nombre Maestro (uid)",
    "Materia2": "No hay maestro aún",
    ...
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
  "materias": ["Materia1", "Materia2", ...],  // Materias que puede impartir
  "rol": "maestro",
  "estado": "pendiente|aprobado|rechazado",
  
  // Si está asignado por admin:
  "gruposAsignados": ["1", "2"],
  "materiasAsignadas": ["Materia1", "Materia2"],  // Materias que efectivamente imparte
  "carreraAsignada": "Programación",
  "asignaciones": [
    {"grupo": "1", "materia": "Materia1"},
    {"grupo": "2", "materia": "Materia1"}
  ]
}
```

---

## ✅ Validaciones

### Registro de Alumno

1. **Nombre**: No puede estar vacío
2. **Carrera**: Debe estar seleccionada
3. **Grupo**: Debe estar seleccionado
4. **Matrícula**: Debe ser un número válido
5. **Semestre Anterior**: Todas las materias deben tener promedio >= 6
6. **Materias**: Debe haber al menos una materia para el semestre (si no, error)

### Registro de Maestro

1. **Nombre**: No puede estar vacío
2. **Área Académica**: Debe estar seleccionada
3. **Materias**: Debe seleccionar al menos una materia

### Admin - Aceptar Alumno

1. **Capacidad del Grupo**: Máximo 30 alumnos por grupo
   - Consulta `alumnos` donde `carrera == X` y `grupo == Y` y `estado == "aprobado"`
   - Si count >= 30 → Error

### Admin - Asignar Maestro

1. **Conflicto de Asignación**: No puede haber 2 maestros asignados a la misma combinación (grupo + materia + carrera)
   - Consulta todos los maestros aprobados de esa carrera
   - Verifica si algún maestro ya tiene asignada esa combinación
   - Si hay conflicto → Muestra lista y no permite

### Maestro - Calificar

1. **Rango de Calificación**: 0 a 100
2. **Formato**: Debe ser un número válido
3. **Materia**: Debe tener una materia seleccionada

---

## 🔐 Autenticación y Seguridad

### Google Sign-In

1. **Configuración**:
   - Web Client ID debe estar en `strings.xml` (`default_web_client_id`)
   - Se configura en Firebase Console

2. **Flujo**:
   - `GoogleSignInClient.signInIntent()` abre selector de cuenta
   - Se obtiene `GoogleSignInAccount`
   - Se autentica con Firebase: `FirebaseAuth.getInstance().signInWithCredential()`

3. **Forzar Selección de Cuenta**:
   - En `SignInScreen.onCreate()` se hace `signOut()` para limpiar sesión previa
   - Esto fuerza que siempre se muestre el selector

### Contraseñas

- **Maestro**: `123456` (hardcodeada, definida en `strings.xml`)
- **Administrador**: `7890124` (hardcodeada, definida en `strings.xml`)

### Estados de Usuario

- **pendiente**: Usuario registrado pero no autorizado
- **aprobado**: Usuario autorizado, puede usar su panel
- **rechazado**: Usuario rechazado por admin

---

## 📱 Características Adicionales

### Generación de PDF

- **Librería**: iTextPDF 5.5.13.2
- **Ubicación**: Directorio específico de la app (no requiere permisos en Android 10+)
- **Compatibilidad**: Usa `FileProvider` para compartir archivos de forma segura

### Manejo de Permisos

- **Almacenamiento**: Solo para Android 9 e inferiores (API <= 28)
- **Android 10+**: Usa directorios específicos de la app (no requiere permisos)

### Compatibilidad hacia atrás

- El sistema maneja estructuras de datos antiguas:
  - Calificaciones antiguas (una sola calificación) se convierten automáticamente
  - Grupos/materias asignados (String) se convierten a List
  - Todo se normaliza a la estructura nueva

---

## 🎨 Interfaz de Usuario

### Colores

- **Fondo**: `@color/fondo` (#feebdc - beige claro)
- **Primary**: Color primario (azul)
- **Accent**: Rosa
- **Error**: Rojo
- **Success Dark**: Verde oscuro (#FF2E7D32)
- **Gray Button**: Gris claro (#FFE0E0E0)

### Temas

- **Parent Theme**: `Theme.MaterialComponents.Light.NoActionBar`
- Todos los layouts usan Material Components para consistencia

### Tipografías

- Títulos grandes: 28sp - 64sp
- Texto normal: 16sp - 18sp
- Texto pequeño: 14sp

---

## 🔧 Helper Classes

### MateriasHelper

**Propósito**: Centraliza la gestión de materias por carrera y semestre.

**Métodos Principales**:

1. `inicializar(Context)`:
   - Inicializa el mapa de materias
   - Debe llamarse antes de usar otros métodos

2. `getMateriasPorSemestre(String carrera, int semestre)`:
   - Retorna la lista de materias para un semestre específico
   - Retorna lista vacía si no existe

3. `validarAprobacionSemestreAnterior(...)`:
   - Valida que todas las materias del semestre anterior tengan promedio >= 6
   - Retorna `true` si puede avanzar, `false` si no

---

## 📝 Notas Importantes

1. **Matrícula**: Empieza en 100 y se auto-incrementa
2. **Grupos**: Máximo 30 alumnos por grupo (validación en Admin)
3. **Semestres**: Solo hay 4 semestres disponibles
4. **Parciales**: 3 parciales por semestre
5. **Promedio Mínimo**: 6.0 para avanzar de semestre
6. **Compatibilidad**: El sistema maneja datos antiguos y nuevos automáticamente

---

## 🐛 Manejo de Errores

### Errores Comunes y Soluciones

1. **Error de Compilación - TextView no encontrado**:
   - Verificar imports en `RegisterAlumnoActivity.java`

2. **Error de Autenticación**:
   - Verificar Web Client ID en `strings.xml`
   - Verificar SHA-1 en Firebase Console

3. **Error al cargar datos**:
   - Verificar conexión a internet
   - Verificar reglas de Firestore
   - Verificar que el documento exista

4. **Error al generar PDF**:
   - Verificar permisos (solo Android <= 9)
   - Verificar espacio en almacenamiento

---

## 📚 Recursos Adicionales

- **Strings**: `app/src/main/res/values/strings.xml`
- **Colores**: `app/src/main/res/values/colors.xml`
- **Temas**: `app/src/main/res/values/themes.xml`
- **Layouts**: `app/src/main/res/layout/`

---

**Versión del Documento**: 1.0  
**Última Actualización**: 2024  
**Autor**: Sistema AppKardex

