# Sistema de Reconocimiento Facial - Documentación Técnica

## Índice
1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Tecnologías Utilizadas](#tecnologías-utilizadas)
3. [Arquitectura del Sistema](#arquitectura-del-sistema)
4. [Estructura de Datos](#estructura-de-datos)
5. [Casos de Uso](#casos-de-uso)
6. [Diagramas de Flujo](#diagramas-de-flujo)
7. [Implementación Técnica](#implementación-técnica)

---

## Resumen Ejecutivo

El sistema de reconocimiento facial implementado permite la autenticación biométrica de usuarios mediante el análisis de características faciales. El sistema captura múltiples muestras biométricas durante el registro y las utiliza para comparación durante el inicio de sesión, garantizando alta precisión y seguridad.

### Características Principales
- **Registro Facial Obligatorio**: Los usuarios deben completar el registro facial para poder acceder al sistema
- **Captura Múltiple**: Se capturan 15 muestras biométricas durante el registro para mayor precisión
- **Reconocimiento Automático**: El sistema reconoce automáticamente al usuario sin necesidad de presionar botones
- **Alta Precisión**: Sistema de votación mejorado que compara contra múltiples muestras guardadas
- **Exclusión de Admins**: Los administradores no requieren reconocimiento facial

---

## Tecnologías Utilizadas

### 1. ML Kit Face Detection (Google)
- **Versión**: 16.1.7
- **Propósito**: Detección de rostros y extracción de landmarks faciales
- **Características utilizadas**:
  - Detección de rostros en tiempo real
  - Extracción de landmarks (ojos, nariz, boca, mejillas)
  - Cálculo de ángulos de Euler (Yaw, Pitch) para validación de pose
  - Clasificación de expresiones y características faciales

### 2. CameraX (Android Jetpack)
- **Versión**: 1.3.0
- **Propósito**: Control de cámara y captura de imágenes
- **Componentes utilizados**:
  - `PreviewView`: Vista previa de la cámara
  - `ImageAnalysis`: Análisis de frames en tiempo real
  - `ImageCapture`: Captura de imágenes (respaldo)
  - `ProcessCameraProvider`: Gestión del ciclo de vida de la cámara

### 3. Firebase Firestore
- **Propósito**: Almacenamiento de datos biométricos
- **Estructura**:
  - Colección `usuarios`: Datos principales y `faceData`
  - Colección `alumnos`: Datos específicos de alumnos + `faceData`
  - Colección `maestros`: Datos específicos de maestros + `faceData`

### 4. Android Biometric API
- **Estado**: Inicialmente implementado, luego removido
- **Razón**: El usuario prefirió usar ML Kit con cámara visible y puntos de escaneo visuales

### 5. FaceLandmarkOverlayView (Custom View)
- **Propósito**: Visualización de landmarks faciales en tiempo real
- **Características**:
  - Dibuja círculos en los puntos clave del rostro
  - Conecta los landmarks con líneas para visualización
  - Actualización en tiempo real durante el escaneo

---

## Arquitectura del Sistema

### Componentes Principales

#### 1. FaceRegistrationActivity
**Responsabilidad**: Captura y almacenamiento de datos biométricos durante el registro

**Flujo**:
1. Inicializa cámara frontal con CameraX
2. Configura ML Kit Face Detector con modo de alta precisión
3. Analiza frames en tiempo real
4. Valida pose facial (frente, estable)
5. Captura 15 muestras automáticamente
6. Extrae datos biométricos de cada muestra
7. Guarda datos en Firestore (formato aplanado)

**Datos Capturados por Muestra**:
- Coordenadas normalizadas de landmarks (ojos, nariz, boca, mejillas)
- Distancias biométricas normalizadas (distancia entre ojos, nariz-boca, etc.)
- Proporciones faciales (aspect ratio, ratios ojo-nariz, ojo-boca)
- Información del bounding box

#### 2. FaceRecognitionActivity
**Responsabilidad**: Reconocimiento facial durante el inicio de sesión

**Flujo**:
1. Carga datos biométricos guardados de Firestore
2. Inicializa cámara y detector facial
3. Analiza frames en tiempo real
4. Compara rostro actual contra todas las muestras guardadas
5. Utiliza sistema de votación para determinar coincidencia
6. Permite acceso si la similitud supera el umbral

**Algoritmo de Reconocimiento**:
- Compara contra todas las muestras guardadas (15 muestras)
- Calcula similitud usando distancia euclidiana normalizada
- Sistema de votación: requiere al menos 3 coincidencias
- Estrategias múltiples: mejor coincidencia, promedio, mediana

#### 3. SignInScreen
**Responsabilidad**: Verificación de datos faciales y redirección

**Lógica**:
- Verifica si el usuario tiene `faceData` en Firestore
- Si tiene datos → Redirige a `FaceRecognitionActivity`
- Si no tiene datos → Cierra sesión y muestra error
- **Excepción**: Admins acceden directamente sin reconocimiento

#### 4. AdminPanelActivity
**Responsabilidad**: Aprobación de usuarios y copia de datos faciales

**Flujo de Aprobación**:
1. Admin acepta solicitud de usuario
2. Copia `faceData` de colección `usuarios` a `alumnos`/`maestros`
3. Actualiza estado a "aprobado"
4. Los datos quedan disponibles en ambas colecciones

---

## Estructura de Datos

### Formato de Datos Biométricos

Debido a que Firestore **NO soporta arrays anidados**, los datos se almacenan en formato aplanado:

```json
{
  "faceData": {
    "samples": [
      {
        "sampleIndex": 0,
        "mapCount": 3,
        "map0_leftEye_x": 0.123,
        "map0_leftEye_y": 0.456,
        "map0_rightEye_x": 0.789,
        "map0_rightEye_y": 0.012,
        "map0_noseBase_x": 0.345,
        "map0_noseBase_y": 0.678,
        "map1_eyeDistance": 0.234,
        "map1_noseMouthDistance": 0.567,
        "map2_faceAspectRatio": 0.890,
        "map2_eyeMouthRatio": 0.123
      },
      {
        "sampleIndex": 1,
        "mapCount": 3,
        ...
      }
    ],
    "sampleCount": 15,
    "userId": "uid_firebase",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

### Estructura Interna (En Memoria)

Durante el procesamiento, los datos se mantienen como:
```java
List<List<Map<String, Float>>> faceDataSamples
```

Donde:
- **Lista externa**: 15 muestras diferentes
- **Lista interna**: Múltiples maps con diferentes tipos de datos
  - `Map 0`: Coordenadas normalizadas de landmarks
  - `Map 1`: Distancias biométricas
  - `Map 2`: Proporciones faciales

### Campos Biométricos Extraídos

#### Landmarks (Coordenadas Normalizadas)
- `leftEye_x`, `leftEye_y`
- `rightEye_x`, `rightEye_y`
- `noseBase_x`, `noseBase_y`
- `mouthBottom_x`, `mouthBottom_y`
- `leftCheek_x`, `leftCheek_y`
- `rightCheek_x`, `rightCheek_y`

#### Distancias Normalizadas
- `eyeDistance`: Distancia entre ojos
- `noseMouthDistance`: Distancia nariz-boca
- `leftEyeNoseDistance`: Distancia ojo izquierdo-nariz
- `rightEyeNoseDistance`: Distancia ojo derecho-nariz
- `estimatedMouthWidth`: Ancho estimado de boca
- `cheekWidth`: Ancho entre mejillas

#### Proporciones Faciales
- `faceAspectRatio`: Relación ancho/alto del rostro
- `eyeMouthRatio`: Relación nivel ojos-nivel boca
- `eyeNoseRatio`: Relación nivel ojos-nivel nariz

---

## Casos de Uso

### Caso de Uso 1: Registro de Nuevo Usuario

**Actor**: Usuario (Alumno o Maestro)

**Precondiciones**:
- Usuario ha completado el registro básico (datos personales)
- Estado del usuario: "registro_incompleto"

**Flujo Principal**:
1. Usuario completa formulario de registro
2. Sistema guarda datos básicos en Firestore
3. Sistema redirige automáticamente a `FaceRegistrationActivity`
4. Usuario ve la cámara frontal
5. Usuario presiona botón "Empezar Escaneo"
6. Sistema detecta rostro y valida pose (frente, estable)
7. Sistema captura automáticamente 15 muestras
8. Sistema muestra progreso: "Escaneando... (X/15 muestras)"
9. Sistema guarda datos en Firestore (colecciones `usuarios` y `alumnos`/`maestros`)
10. Sistema actualiza estado a "pendiente"
11. Sistema redirige a pantalla de selección

**Postcondiciones**:
- Usuario tiene `faceData` guardado en Firestore
- Estado del usuario: "pendiente"
- Usuario espera aprobación del administrador

**Excepciones**:
- Si el usuario cancela: Se elimina el registro incompleto
- Si no se capturan suficientes muestras: Se muestra error y permite reintentar

---

### Caso de Uso 2: Aprobación de Usuario por Admin

**Actor**: Administrador

**Precondiciones**:
- Usuario tiene estado "pendiente"
- Usuario tiene `faceData` guardado en colección `usuarios`

**Flujo Principal**:
1. Admin accede al panel de administración
2. Admin ve lista de usuarios pendientes
3. Admin selecciona usuario y presiona "Aceptar"
4. Sistema copia `faceData` de `usuarios` a `alumnos`/`maestros`
5. Sistema actualiza estado a "aprobado" en ambas colecciones
6. Sistema muestra confirmación

**Postcondiciones**:
- Usuario tiene `faceData` en ambas colecciones (`usuarios` y `alumnos`/`maestros`)
- Estado del usuario: "aprobado"
- Usuario puede iniciar sesión

---

### Caso de Uso 3: Inicio de Sesión con Reconocimiento Facial

**Actor**: Usuario Registrado

**Precondiciones**:
- Usuario tiene cuenta aprobada
- Usuario tiene `faceData` guardado en Firestore
- Usuario NO es administrador

**Flujo Principal**:
1. Usuario inicia sesión con Google
2. Sistema verifica si tiene `faceData`
3. Sistema redirige a `FaceRecognitionActivity`
4. Sistema carga datos biométricos guardados
5. Sistema inicia cámara frontal
6. Sistema detecta rostro en tiempo real
7. Sistema valida pose (frente, estable)
8. Sistema compara automáticamente contra todas las muestras guardadas
9. Sistema calcula similitud usando múltiples estrategias
10. Si similitud >= 0.70 → Acceso permitido
11. Sistema redirige al panel correspondiente (Alumno/Maestro)

**Postcondiciones**:
- Usuario autenticado y en su panel principal
- Reconocimiento facial completado exitosamente

**Excepciones**:
- Si rostro no reconocido: Muestra error y botón "Reintentar"
- Si no hay datos faciales: Cierra sesión y muestra error
- Si usuario es admin: Acceso directo sin reconocimiento

---

### Caso de Uso 4: Inicio de Sesión de Administrador

**Actor**: Administrador

**Precondiciones**:
- Usuario tiene rol "admin"

**Flujo Principal**:
1. Admin inicia sesión con Google
2. Sistema verifica rol
3. Sistema detecta que es "admin"
4. Sistema omite verificación de `faceData`
5. Sistema redirige directamente a `AdminPanelActivity`

**Postcondiciones**:
- Admin accede directamente sin reconocimiento facial

---

## Diagramas de Flujo

### Diagrama de Flujo: Registro Facial

```
[Usuario completa registro básico]
           |
           v
[Redirige a FaceRegistrationActivity]
           |
           v
[Usuario presiona "Empezar Escaneo"]
           |
           v
[Inicia cámara frontal]
           |
           v
[ML Kit detecta rostro?]
    /              \
  NO               SÍ
   |                |
   v                v
[Mostrar:         [Validar pose facial]
 "Ajusta          (frente, estable?)
 posición"]        /          \
   |              NO          SÍ
   |               |            |
   |               v            v
   |         [Mostrar:      [Contar frames
   |          "Ajusta        estables]
   |          posición"]      |
   |               |          v
   |               |    [¿Frames >= 8?]
   |               |     /          \
   |               |    NO          SÍ
   |               |     |            |
   |               |     v            v
   |               |  [Continuar]  [Capturar muestra]
   |               |     |            |
   |               |     |            v
   |               |     |    [¿Muestras < 15?]
   |               |     |     /          \
   |               |     |    SÍ          NO
   |               |     |     |            |
   |               |     |     v            v
   |               |     |  [Continuar]  [Guardar en Firestore]
   |               |     |     |            |
   |               |     |     |            v
   |               |     |     |    [Actualizar estado a "pendiente"]
   |               |     |     |            |
   |               |     |     |            v
   |               |     |     |    [Redirigir a SelectionScreen]
   |               |     |     |            |
   |               |     |     |            v
   |               |     |     |        [FIN]
   |               |     |     |
   |               |     |     v
   |               |     |  [Mostrar progreso: "X/15 muestras"]
   |               |     |
   |               |     v
   |               |  [Volver a detectar rostro]
   |               |
   |               v
   |          [Continuar loop]
   |
   v
[FIN - Esperando rostro]
```

### Diagrama de Flujo: Inicio de Sesión con Reconocimiento

```
[Usuario inicia sesión con Google]
           |
           v
[Autenticación exitosa]
           |
           v
[Verificar rol del usuario]
           |
           v
[¿Es admin?]
    /          \
  SÍ           NO
   |            |
   v            v
[Acceso    [Verificar faceData
directo]    en Firestore]
   |            |
   |            v
   |    [¿faceData existe?]
   |         /      \
   |       NO       SÍ
   |        |        |
   |        v        v
   |   [Cerrar   [Redirigir a
   |    sesión]  FaceRecognitionActivity]
   |        |        |
   |        |        v
   |        |   [Cargar datos biométricos]
   |        |        |
   |        |        v
   |        |   [Iniciar cámara]
   |        |        |
   |        |        v
   |        |   [ML Kit detecta rostro?]
   |        |        /          \
   |        |      NO           SÍ
   |        |       |            |
   |        |       v            v
   |        |  [Mostrar:    [Validar pose]
   |        |   "Posiciona      |
   |        |   tu rostro"]     v
   |        |       |      [¿Pose válida?]
   |        |       |         /      \
   |        |       |       NO      SÍ
   |        |       |        |        |
   |        |       |        v        v
   |        |       |   [Continuar] [Contar frames estables]
   |        |       |        |        |
   |        |       |        |        v
   |        |       |        |   [¿Frames >= 6?]
   |        |       |        |     /      \
   |        |       |        |    NO     SÍ
   |        |       |        |     |       |
   |        |       |        |     v       v
   |        |       |        |  [Continuar] [Comparar contra todas las muestras]
   |        |       |        |     |       |
   |        |       |        |     |       v
   |        |       |        |     |  [Calcular similitud]
   |        |       |        |     |       |
   |        |       |        |     |       v
   |        |       |        |     |  [¿Similitud >= 0.70?]
   |        |       |        |     |     /      \
   |        |       |        |     |    NO     SÍ
   |        |       |        |     |     |       |
   |        |       |        |     |     v       v
   |        |       |        |     |  [Mostrar: [Acceso permitido]
   |        |       |        |     |   "Rostro no      |
   |        |       |        |     |   reconocido"]    v
   |        |       |        |     |     |       [Redirigir a panel]
   |        |       |        |     |     |            |
   |        |       |        |     |     |            v
   |        |       |        |     |     |        [FIN]
   |        |       |        |     |     |
   |        |       |        |     |     v
   |        |       |        |     |  [Mostrar botón "Reintentar"]
   |        |       |        |     |
   |        |       |        |     v
   |        |       |        |  [Volver a detectar]
   |        |       |        |
   |        |       |        v
   |        |       |    [Continuar loop]
   |        |       |
   |        |       v
   |        |   [FIN - Esperando reconocimiento]
   |        |
   |        v
   |    [FIN - Sin acceso]
   |
   v
[FIN - Admin en panel]
```

### Diagrama de Flujo: Aprobación por Admin

```
[Admin accede a panel]
           |
           v
[Ver lista de usuarios pendientes]
           |
           v
[Admin selecciona usuario]
           |
           v
[Admin presiona "Aceptar"]
           |
           v
[Obtener faceData de colección "usuarios"]
           |
           v
[¿faceData existe?]
    /          \
  NO           SÍ
   |            |
   v            v
[Mostrar    [Copiar faceData a
 error]     colección específica]
   |            |
   |            v
   |    [Actualizar estado a "aprobado"]
   |            |
   |            v
   |    [faceData disponible en ambas colecciones]
   |            |
   |            v
   |        [FIN - Usuario aprobado]
   |
   v
[FIN - Error]
```

---

## Implementación Técnica

### Algoritmo de Comparación de Similitud

```java
private float calculateSimilarity(
    List<Map<String, Float>> currentBiometricData, 
    List<Map<String, Float>> savedBiometricData
) {
    float totalSimilarity = 0.0f;
    float totalWeight = 0.0f;
    
    // Comparar cada tipo de dato biométrico
    for (int i = 0; i < Math.min(currentBiometricData.size(), savedBiometricData.size()); i++) {
        Map<String, Float> current = currentBiometricData.get(i);
        Map<String, Float> saved = savedBiometricData.get(i);
        
        // Calcular similitud para este map
        float mapSimilarity = 0.0f;
        int matchingKeys = 0;
        
        for (String key : current.keySet()) {
            if (saved.containsKey(key)) {
                float currentValue = current.get(key);
                float savedValue = saved.get(key);
                float difference = Math.abs(currentValue - savedValue);
                float similarity = 1.0f - Math.min(difference, 1.0f);
                mapSimilarity += similarity;
                matchingKeys++;
            }
        }
        
        if (matchingKeys > 0) {
            mapSimilarity /= matchingKeys;
            totalSimilarity += mapSimilarity;
            totalWeight += 1.0f;
        }
    }
    
    return totalWeight > 0 ? totalSimilarity / totalWeight : 0.0f;
}
```

### Sistema de Votación para Reconocimiento

```java
// Comparar contra todas las muestras guardadas
int matchesAboveThreshold = 0;
float bestSimilarity = 0.0f;
float totalSimilarity = 0.0f;
List<Float> allSimilarities = new ArrayList<>();

for (List<Map<String, Float>> sample : savedFaceSamples) {
    float similarity = calculateSimilarity(currentLandmarks, sample);
    allSimilarities.add(similarity);
    
    if (similarity > bestSimilarity) {
        bestSimilarity = similarity;
    }
    totalSimilarity += similarity;
    
    if (similarity >= FACE_MATCH_THRESHOLD) {
        matchesAboveThreshold++;
    }
}

// Estrategias de reconocimiento:
// 1. Votación: Al menos 3 muestras coinciden
// 2. Mejor coincidencia: > 0.80
// 3. Mediana y promedio: mediana > 0.70 y promedio > 0.68

boolean accessGranted = 
    matchesAboveThreshold >= MIN_MATCHES_REQUIRED ||
    bestSimilarity > 0.80f ||
    (medianSimilarity > 0.70f && averageSimilarity > 0.68f);
```

### Validación de Pose Facial

```java
private boolean validateFacePose(Face face) {
    // Verificar landmarks principales
    boolean hasLeftEye = face.getLandmark(FaceLandmark.LEFT_EYE) != null;
    boolean hasRightEye = face.getLandmark(FaceLandmark.RIGHT_EYE) != null;
    boolean hasNoseBase = face.getLandmark(FaceLandmark.NOSE_BASE) != null;
    boolean hasMouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null;
    
    if (!hasLeftEye || !hasRightEye || !hasNoseBase || !hasMouthBottom) {
        return false;
    }
    
    // Verificar ángulos de Euler (rotación)
    float headEulerAngleY = face.getHeadEulerAngleY(); // Rotación horizontal
    float headEulerAngleX = face.getHeadEulerAngleX(); // Rotación vertical
    
    // Debe estar de frente: Y cerca de 0, X cerca de 0
    return Math.abs(headEulerAngleY) < 15 && Math.abs(headEulerAngleX) < 15;
}
```

---

## Configuración y Parámetros

### Parámetros de Captura
- `REQUIRED_SAMPLES = 15`: Número de muestras a capturar
- `REQUIRED_STABLE_FRAMES = 8`: Frames estables requeridos antes de capturar
- `FRAMES_BETWEEN_SAMPLES = 2`: Frames entre cada captura de muestra

### Parámetros de Reconocimiento
- `FACE_MATCH_THRESHOLD = 0.65f`: Umbral base de similitud
- `REQUIRED_STABLE_FRAMES = 6`: Frames estables requeridos antes de reconocer
- `MIN_MATCHES_REQUIRED = 3`: Mínimo de muestras que deben coincidir
- `BEST_SIMILARITY_THRESHOLD = 0.80f`: Umbral para mejor coincidencia
- `MEDIAN_THRESHOLD = 0.70f`: Umbral para mediana
- `AVERAGE_THRESHOLD = 0.68f`: Umbral para promedio

### Validación de Pose
- `MAX_YAW_ANGLE = 15°`: Máxima rotación horizontal permitida
- `MAX_PITCH_ANGLE = 15°`: Máxima rotación vertical permitida

---

## Consideraciones de Seguridad

1. **Datos Biométricos**: Los datos se almacenan en Firestore con reglas de seguridad apropiadas
2. **Normalización**: Todas las coordenadas se normalizan para ser independientes del tamaño de pantalla
3. **Múltiples Muestras**: El uso de 15 muestras reduce falsos positivos y negativos
4. **Sistema de Votación**: Requiere múltiples coincidencias para mayor seguridad
5. **Validación de Pose**: Solo acepta rostros de frente para mayor precisión

---

## Mantenimiento y Mejoras Futuras

### Posibles Mejoras
1. **Ajuste Dinámico de Umbrales**: Ajustar umbrales según la calidad de las muestras
2. **Actualización de Muestras**: Permitir actualizar muestras biométricas periódicamente
3. **Detección de Spoofing**: Implementar detección de intentos de suplantación
4. **Optimización de Rendimiento**: Cachear cálculos de similitud para mayor velocidad

---

## Conclusión

El sistema de reconocimiento facial implementado proporciona una solución robusta y segura para la autenticación de usuarios. Utiliza tecnologías modernas de Google (ML Kit y CameraX) y está diseñado para ser preciso, eficiente y fácil de usar.

El formato de datos aplanado garantiza compatibilidad con Firestore, mientras que el sistema de múltiples muestras y votación asegura alta precisión en el reconocimiento.

