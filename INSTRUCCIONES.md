# ✅ INSTRUCCIONES FINALES - AppKardex

## 🎉 ¡Proyecto Completo!

Tu aplicación **AppKardex** está lista con todas las funcionalidades implementadas. Solo falta configurar Firebase.

---

## 📋 QUÉ HACER AHORA

### **PASO 1: Configurar Firebase (20 minutos)**

**⚠️ SIGUE LA GUÍA PASO A PASO:** Abre el archivo **[FIREBASE_CONFIGURATION.md](FIREBASE_CONFIGURATION.md)**

Esta guía incluye:
- ✅ Cómo crear proyecto en Firebase
- ✅ Cómo agregar la app Android
- ✅ Cómo obtener SHA-1
- ✅ Cómo habilitar Google Sign-In
- ✅ Cómo crear Firestore
- ✅ Cómo configurar reglas
- ✅ Cómo obtener Web Client ID
- ✅ Solución de problemas

### **Resumen rápido de pasos:**

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Crea proyecto "AppKardex"
3. Agrega app Android con package: `com.mhrc.appkardex`
4. Descarga `google-services.json` → colócalo en `app/google-services.json`
5. Obtén SHA-1 (ver guía) y agrégalo a Firebase
6. Habilita Firebase Authentication con Google
7. Crea Firestore Database en modo prueba
8. Configura reglas de Firestore (ver guía)
9. Copia Web Client ID en `app/src/main/res/values/strings.xml`
10. Reemplaza `TU_WEB_CLIENT_ID_AQUI` con el ID real

---

## 🚀 PASO 2: Compilar y Ejecutar

### **Opción A: Android Studio (RECOMENDADO)**

1. Abre Android Studio
2. **File → Open** → Selecciona la carpeta `Mamada`
3. Espera a que sincronice (Sync Project with Gradle Files)
4. Si aparece error sobre `google-services.json`, NO TE PREOCUPES - lo solucionarás después
5. Click en **Run** (botón verde ▶️) o presiona **Shift+F10**
6. Elige tu dispositivo/emulador

### **Opción B: Terminal**

```bash
# Ir a la carpeta del proyecto
cd C:\Users\User\Downloads\Mamada

# Sincronizar y compilar
./gradlew clean assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug
```

---

## 🧪 PASO 3: Probar la App

### **Probar Registro de Alumno:**

1. Abre la app
2. Espera la pantalla de splash (4 segundos)
3. Click en **"Inscribirse"**
4. Click en **"Alumno"**
5. Click en **"Continuar con Google"**
6. Selecciona tu cuenta de Google
7. Completa el formulario:
   - Nombre (editable)
   - Email (viene de Google, no editable)
   - Matrícula (auto-generada)
   - Carrera (selecciona una)
   - Grupo (selecciona 1, 2 o 3)
8. Click en **"Registrar"**
9. Deberías ver: "Registro exitoso. Espera autorización"
10. Cierra la app

### **Probar Registro de Admin:**

1. Abre la app
2. Click en **"Inscribirse"**
3. Click en **"Administrador"**
4. Ingresa contraseña: **7890124**
5. Click en **"Continuar con Google"**
6. Selecciona otra cuenta de Google (o la misma)
7. Serás redirigido automáticamente al **Panel de Administrador**

### **Probar Panel de Admin:**

1. En el Panel de Admin, deberías ver la pestaña **"Alumnos"**
2. Deberías ver tu solicitud de alumno que registraste antes
3. Click en **"Aceptar"**
4. Te pedirá asignar grupo final
5. Selecciona grupo (ej: "1")
6. Click **"Aceptar"**
7. Verás "Alumno aceptado"
8. La solicitud desaparece

### **Probar Login de Alumno:**

1. Cierra la app completamente
2. Abre la app nuevamente
3. Click en **"¿Ya estás inscrito?"**
4. Click en **"Continuar con Google"**
5. Selecciona la cuenta de Google que usaste como alumno
6. Serás redirigido al **Panel de Alumno**
7. Verás tu nombre, grupo, carrera y matrícula
8. Verás sección "Calificaciones del 1er Parcial" (vacía por ahora)

### **Probar Registro de Maestro:**

1. Cierra la app
2. Abre la app
3. Click en **"Inscribirse"**
4. Click en **"Maestro"**
5. Ingresa contraseña: **123456**
6. Click en **"Continuar con Google"**
7. Selecciona cuenta de Google
8. Completa formulario de maestro
9. Click **"Registrar"**
10. Verás: "Atento, próximamente estaremos comunicando"

### **Probar Panel de Admin - Aprobar Maestro:**

1. Si sigues en el Panel de Admin, click en pestaña **"Maestros"**
2. Verás la solicitud de maestro
3. Click en **"Aceptar"**
4. Verás "Maestro aceptado"

### **Probar Login de Maestro:**

1. Cierra la app
2. Abre la app
3. Click **"¿Ya estás inscrito?"**
4. Login con Google (cuenta de maestro)
5. Serás redirigido al **Panel de Maestro**
6. Verás "Grupos asignados"
7. Verás los grupos que tienen alumnos de tu área académica

### **Probar Poner Calificaciones:**

1. En Panel de Maestro, click en un grupo
2. Verás lista de alumnos
3. Click en un alumno
4. Se abre diálogo con materias del alumno
5. Ingresa calificaciones (0-100)
6. Click **"Guardar"**
7. Verás "Calificaciones guardadas"

### **Ver Calificaciones en Panel de Alumno:**

1. Cierra la app
2. Abre la app
3. Login como alumno
4. En Panel de Alumno, verás tus calificaciones

---

## 📁 ESTRUCTURA FINAL DEL PROYECTO

```
Mamada/
├── app/
│   ├── google-services.json          ⚠️ DESCARGAR DE FIREBASE
│   ├── build.gradle                  ✅ Configurado
│   └── src/main/
│       ├── java/com/mhrc/appkardex/
│       │   ├── MainActivity.java                 ✅ Entry point
│       │   ├── SplashScreen.java                 ✅ Splash 4s
│       │   ├── SelectionScreen.java              ✅ Inscribirse/Login
│       │   ├── UserTypeSelectionActivity.java   ✅ Tipo usuario
│       │   ├── SignInScreen.java                ✅ Google Sign-In
│       │   ├── RegisterAlumnoActivity.java      ✅ Registro alumno
│       │   ├── RegisterMaestroActivity.java     ✅ Registro maestro
│       │   ├── AdminPanelActivity.java          ✅ Panel admin
│       │   ├── AlumnoPanelActivity.java         ✅ Panel alumno
│       │   └── MaestroPanelActivity.java        ✅ Panel maestro
│       ├── res/
│       │   ├── layout/                          ✅ 17 layouts
│       │   └── values/
│       │       ├── strings.xml                  ⚠️ Configurar Web Client ID
│       │       ├── colors.xml                   ✅ Colores
│       │       └── themes.xml                   ✅ Tema
│       └── AndroidManifest.xml                  ✅ Configurado
├── gradle/
│   └── libs.versions.toml                      ✅ Versiones
├── build.gradle                                ✅ Configurado
├── settings.gradle                             ✅ Configurado
├── README.md                                   ✅ Documentación
├── FIREBASE_CONFIGURATION.md                   ✅ Guía Firebase
└── INSTRUCCIONES.md                            ✅ Este archivo
```

---

## ✅ CHECKLIST FINAL

Antes de considerar el proyecto terminado:

- [ ] Firebase Console creado
- [ ] App Android agregada en Firebase
- [ ] `google-services.json` descargado y colocado
- [ ] SHA-1 agregado en Firebase
- [ ] `google-services.json` actualizado
- [ ] Firebase Auth habilitado con Google
- [ ] Firestore creado
- [ ] Reglas de Firestore configuradas
- [ ] Web Client ID copiado en `strings.xml`
- [ ] App compila sin errores
- [ ] App ejecuta en dispositivo/emulador
- [ ] Login con Google funciona
- [ ] Registro de alumno funciona
- [ ] Registro de maestro funciona
- [ ] Registro de admin funciona
- [ ] Panel admin muestra solicitudes
- [ ] Admin puede aceptar/rechazar
- [ ] Panel maestro muestra grupos
- [ ] Maestro puede poner calificaciones
- [ ] Panel alumno muestra calificaciones

---

## 🔍 SOLUCIÓN DE PROBLEMAS

### "google-services.json not found"
→ Descarga el archivo desde Firebase Console y colócalo en `app/google-services.json`

### "default_web_client_id not found"
→ Verifica que `strings.xml` tenga el Web Client ID correcto (sin espacios)

### "App compila pero al ejecutar se cierra"
→ Verifica que Firebase esté configurado correctamente
→ Verifica el SHA-1
→ Verifica que Authentication esté habilitado

### "Login con Google no funciona"
→ Verifica SHA-1 en Firebase Console
→ Descarga nuevo `google-services.json`
→ Verifica que Google esté habilitado en Authentication

### "No se ven los datos en Firestore"
→ Verifica que las reglas estén publicadas
→ Verifica conexión a internet
→ Verifica que estés usando reglas permisivas (solo para desarrollo)

### Para más ayuda:
→ Lee **FIREBASE_CONFIGURATION.md** sección "Solución de Problemas"
→ Revisa console de Firebase
→ Revisa logcat en Android Studio

---

## 📚 DOCUMENTACIÓN

- **README.md** - Documentación general del proyecto
- **FIREBASE_CONFIGURATION.md** - Guía completa de Firebase (⚠️ MUY IMPORTANTE)
- **INSTRUCCIONES.md** - Este archivo

---

## 🎓 CONTENIDO TÉCNICO

### Tecnologías Usadas:
- Java 8
- Android API 24+ (Android 7.0+)
- Material Design Components
- Firebase Firestore
- Firebase Authentication
- Google Sign-In

### Dependencias Principales:
- Firebase BOM: 32.7.0
- Firebase Auth
- Firebase Firestore  
- Google Play Services Auth: 20.7.0
- Material Components: 1.10.0
- AppCompat: 1.6.1

### Contraseñas:
- Maestro: `123456`
- Administrador: `7890124`

### Estructura de Datos:
Ver **README.md** sección "Estructura de Datos (Firestore)"

---

## 🚨 IMPORTANTE - PRÓXIMOS PASOS

### Para Producción:
1. ✅ Cambia reglas de Firestore por reglas seguras
2. ✅ Activa App Check
3. ✅ Configura índices compuestos en Firestore
4. ✅ Implementa recuperación de contraseña
5. ✅ Agrega validación de email
6. ✅ Implementa generación de PDF
7. ✅ Agrega notificaciones push
8. ✅ Implementa backup de datos

### Mejoras Sugeridas:
- [ ] Recuperación de contraseña
- [ ] Validación de email
- [ ] Cambio de contraseña
- [ ] Perfil de usuario editable
- [ ] Foto de perfil
- [ ] Notificaciones push
- [ ] Dashboard de estadísticas
- [ ] Exportación a Excel
- [ ] Historial de parciales
- [ ] Múltiples semestres

---

## 📞 CONTACTO Y SOPORTE

Si tienes problemas:
1. ✅ Revisa esta guía completa
2. ✅ Lee FIREBASE_CONFIGURATION.md
3. ✅ Revisa logcat en Android Studio
4. ✅ Verifica Firebase Console
5. ✅ Verifica que todos los pasos se hayan completado

---

## 🎉 ¡LISTO!

Tu proyecto AppKardex está completo y listo para usar. Solo falta configurar Firebase siguiendo la guía **FIREBASE_CONFIGURATION.md**.

**¡Éxito con tu proyecto! 🚀**

