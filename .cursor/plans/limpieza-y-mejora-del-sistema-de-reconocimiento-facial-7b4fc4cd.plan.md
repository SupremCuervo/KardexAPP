<!-- 7b4fc4cd-2570-406d-8362-98485b9b95cb f407a28a-0667-463e-9f8f-20534779f0d9 -->
# Plan: Limpieza y Mejora del Sistema de Reconocimiento Facial

## Objetivo

Simplificar y mejorar el sistema de reconocimiento facial para usar solo ML Kit con una fase de captura (frente), mostrar visualización de landmarks en tiempo real, y eliminar toda la lógica de BiometricPrompt.

## Análisis del Sistema Actual

### Flujo de Registro:

1. `RegisterAlumnoActivity` / `RegisterMaestroActivity` → Guarda datos básicos con `faceDataPending: true` y `estado: "registro_incompleto"`
2. Redirige a `FaceRegistrationActivity` con `fromRegistration: true`
3. `FaceRegistrationActivity` captura 3 fases (frente, derecha, izquierda)
4. Guarda en Firebase: `faceData` con `phase1_front`, `phase2_right`, `phase3_left`, `nativeBiometric: false`
5. Actualiza `estado: "pendiente"` y `faceDataPending: false`

### Flujo de Login:

1. `SignInScreen` → Google Sign-In exitoso
2. `checkFaceDataAndProceed()` verifica si existe `faceData` en Firebase
3. Si existe, redirige a `FaceRecognitionActivity`
4. `FaceRecognitionActivity` compara el rostro actual con los datos guardados
5. Si coincide, procede a `checkUserRole()` → pantalla principal

### Estructura de Datos en Firebase:

```json
{
  "faceData": {
    "phase1_front": [/* landmarks, distances, proportions */],
    "phase2_right": [/* ... */],
    "phase3_left": [/* ... */],
    "nativeBiometric": false,
    "userId": "...",
    "totalPhases": 3,
    "timestamp": "..."
  }
}
```

## Cambios a Realizar

### 1. Eliminar BiometricPrompt Completamente

- **Archivos**: `FaceRegistrationActivity.java`, `FaceRecognitionActivity.java`, `SignInScreen.java`
- Eliminar imports de `androidx.biometric`
- Eliminar variables: `biometricPrompt`, `useNativeBiometric`, `waitingForBiometric`
- Eliminar métodos: `checkBiometricAvailable()`, `setupNativeBiometric()`, `validatePhaseWithBiometric()`
- Eliminar dependencia `androidx.biometric` de `build.gradle` y `libs.versions.toml`

### 2. Simplificar a 1 Fase (Solo Frente)

- **Archivo**: `FaceRegistrationActivity.java`
- Eliminar sistema de fases múltiples: `currentPhase`, `TOTAL_PHASES`, `phaseData` (Map con múltiples fases)
- Simplificar `phaseData` a solo guardar una lista de landmarks
- Eliminar `moveToNextPhase()`, `getPhaseName()`, `validatePose()` (ya no necesitamos validar giros)
- Simplificar `saveFaceData()` para guardar solo `faceData.landmarks` (sin fases)
- Actualizar UI: eliminar indicadores de fases, simplificar mensajes

### 3. Crear Visualización de Landmarks en Tiempo Real

- **Nuevo archivo**: `FaceLandmarkOverlayView.java` (Custom View)
- Dibujar círculos/puntos sobre los landmarks detectados:
  - Ojos (izquierdo y derecho)
  - Base de nariz
  - Boca (inferior)
  - Mejillas (si están disponibles)
- Usar `Canvas` y `Paint` para dibujar sobre el `PreviewView`
- Actualizar en tiempo real con cada frame de detección
- **Archivo**: `activity_face_registration.xml` y `activity_face_recognition.xml`
- Agregar `FaceLandmarkOverlayView` sobre el `PreviewView` con `android:elevation` mayor

### 4. Simplificar Extracción de Landmarks

- **Archivo**: `FaceRegistrationActivity.java` y `FaceRecognitionActivity.java`
- Mantener `extractFaceLandmarks()` pero simplificar (ya no necesitamos validar poses de giro)
- Extraer: landmarks normalizados, distancias, proporciones, bounding box
- Guardar estructura más simple en Firebase

### 5. Actualizar Estructura de Datos en Firebase

- **Nueva estructura**:
```json
{
  "faceData": {
    "landmarks": [/* Lista de Map con landmarks, distances, proportions */],
    "userId": "...",
    "timestamp": "..."
  }
}
```

- Eliminar: `phase1_front`, `phase2_right`, `phase3_left`, `nativeBiometric`, `totalPhases`
- **Archivo**: `FaceRegistrationActivity.java` → `saveFaceData()`

### 6. Simplificar Reconocimiento en Login

- **Archivo**: `FaceRecognitionActivity.java`
- Eliminar lógica de comparación con múltiples fases
- Comparar solo con `faceData.landmarks` (una sola fase)
- Simplificar `recognizeFace()` y `calculateSimilarity()`
- Eliminar `convertLandmarks()` si no es necesario

### 7. Actualizar Layouts

- **Archivo**: `activity_face_registration.xml`
- Eliminar `llProgress` con indicadores de fases múltiples
- Eliminar `tvPhaseTitle` (ya no hay fases)
- Eliminar `btnNextPhase`
- Actualizar textos: "Registro Facial - Escaneo de Rostro"
- Agregar `FaceLandmarkOverlayView` sobre `previewView`
- **Archivo**: `activity_face_recognition.xml`
- Agregar `FaceLandmarkOverlayView` sobre `previewView`

### 8. Limpiar SignInScreen

- **Archivo**: `SignInScreen.java`
- Eliminar verificación de `nativeBiometric`
- Simplificar `checkFaceDataAndProceed()`: solo verificar si existe `faceData.landmarks`
- Si existe, redirigir a `FaceRecognitionActivity`
- Si no existe, proceder con login normal (aunque esto no debería pasar si el registro es obligatorio)

### 9. Actualizar Flujo de Registro

- **Archivos**: `RegisterAlumnoActivity.java`, `RegisterMaestroActivity.java`
- Mantener redirección a `FaceRegistrationActivity` después de guardar datos básicos
- El registro facial sigue siendo obligatorio

### 10. Manejar Usuarios con Registro Antiguo

- **Archivo**: `FaceRecognitionActivity.java`
- Si `faceData` tiene `nativeBiometric: true` o estructura antigua (fases múltiples):
  - Mostrar mensaje: "Tu registro facial necesita actualizarse. Por favor, re-registra tu rostro."
  - No permitir acceso hasta re-registro
  - Opcional: redirigir a `FaceRegistrationActivity` para re-registro

## Archivos a Modificar

1. `app/src/main/java/com/mhrc/appkardex/FaceRegistrationActivity.java` - Refactor completo
2. `app/src/main/java/com/mhrc/appkardex/FaceRecognitionActivity.java` - Simplificar reconocimiento
3. `app/src/main/java/com/mhrc/appkardex/SignInScreen.java` - Limpiar verificación
4. `app/src/main/java/com/mhrc/appkardex/FaceLandmarkOverlayView.java` - NUEVO: Custom View para landmarks
5. `app/src/main/res/layout/activity_face_registration.xml` - Actualizar UI
6. `app/src/main/res/layout/activity_face_recognition.xml` - Agregar overlay
7. `app/build.gradle` - Eliminar dependencia `androidx.biometric`
8. `gradle/libs.versions.toml` - Eliminar versión y librería de biometric

## Orden de Implementación

1. Crear `FaceLandmarkOverlayView.java` para visualización
2. Actualizar layouts para incluir overlay
3. Refactorizar `FaceRegistrationActivity.java` (1 fase, sin BiometricPrompt)
4. Simplificar `FaceRecognitionActivity.java` (sin BiometricPrompt, 1 fase)
5. Limpiar `SignInScreen.java`
6. Eliminar dependencias de BiometricPrompt
7. Probar flujo completo: registro → login

### To-dos

- [ ] Crear FaceLandmarkOverlayView.java - Custom View para dibujar puntos/círculos sobre landmarks en tiempo real
- [ ] Actualizar activity_face_registration.xml - Eliminar indicadores de fases, agregar overlay view, simplificar UI
- [ ] Actualizar activity_face_recognition.xml - Agregar overlay view para visualización de landmarks
- [ ] Refactorizar FaceRegistrationActivity.java - Eliminar BiometricPrompt, simplificar a 1 fase, integrar overlay view, actualizar saveFaceData()
- [ ] Simplificar FaceRecognitionActivity.java - Eliminar BiometricPrompt, comparar solo con una fase, integrar overlay view
- [ ] Limpiar SignInScreen.java - Eliminar verificación de nativeBiometric, simplificar checkFaceDataAndProceed()
- [ ] Eliminar dependencias de BiometricPrompt - Remover de build.gradle y libs.versions.toml
- [ ] Probar flujo completo - Registro de alumno/maestro → Captura facial → Login → Reconocimiento facial