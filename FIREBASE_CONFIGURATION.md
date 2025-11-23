# 🔥 Guía Completa - Configuración de Firebase para AppKardex

## 📋 Pasos a Seguir

### **Paso 1: Crear Proyecto en Firebase Console**

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Haz clic en **"Agregar proyecto"** o **"Add project"**
3. Ingresa el nombre del proyecto: **"AppKardex"** (o el que prefieras)
4. Desactiva Google Analytics (opcional)
5. Haz clic en **"Crear proyecto"** y espera a que termine la configuración

---

### **Paso 2: Agregar App Android a Firebase**

1. En Firebase Console, haz clic en el ícono **Android** (`</>`) o en **"Agregar app"** → **Android**
2. Completa los siguientes campos:
   - **Package name:** `com.mhrc.appkardex` ⚠️ **DEBE ser exactamente este valor**
   - **App nickname:** `AppKardex` (opcional)
   - **Debug signing certificate SHA-1:** Déjalo vacío por ahora
3. Haz clic en **"Registrar app"**

---

### **Paso 3: Descargar google-services.json**

1. Una vez registrada la app, se te mostrará el archivo **`google-services.json`**
2. Haz clic en **"Descargar google-services.json"**
3. **COPIA** el archivo descargado a la carpeta **`app/`** de tu proyecto
   - ⚠️ **IMPORTANTE:** Debe estar en `app/google-services.json` (no en la raíz)
   - Si usas Cursor/Android Studio, puedes arrastrar el archivo directamente

---

### **Paso 4: Obtener SHA-1 (IMPORTANTE)**

Necesitas obtener el SHA-1 de tu certificado de firma para que Google Sign-In funcione.

#### **Método 1: Usando Android Studio (MÁS FÁCIL)**

1. Abre Android Studio con tu proyecto
2. Ve a la pestaña lateral **"Gradle"** (o en View → Tool Windows → Gradle)
3. Expande: **AppKardex → Tasks → android**
4. Haz **doble clic** en **"signingReport"**
5. En la consola inferior verás algo como:
   ```
   SHA1: AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE
   ```
6. **Copia el SHA1**

#### **Método 2: Usando Terminal/CMD (Windows)**

```bash
cd C:\Users\User\.android
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Busca la línea que dice **"SHA1:"** y copia el valor.

#### **Método 3: Usando PowerShell**

```powershell
cd $env:USERPROFILE\.android
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```

---

### **Paso 5: Agregar SHA-1 a Firebase**

1. Ve a Firebase Console → **"Configuración del proyecto"** (ícono de engranaje)
2. Ve a la pestaña **"General"**
3. Baja hasta **"Tus apps"** → Selecciona tu app Android
4. Haz clic en **"Agregar huella digital"** o **"Add fingerprint"**
5. Pega el **SHA-1** que copiaste
6. Haz clic en **"Guardar"**
7. **IMPORTANTE:** Descarga nuevamente el archivo `google-services.json` y reemplaza el anterior
   - Ve a **"Tus apps"** → Tu app Android → **"Descargar google-services.json"**

---

### **Paso 6: Habilitar Firebase Authentication**

1. En Firebase Console, ve a **"Authentication"** (autenticación)
2. Haz clic en **"Comenzar"** o **"Get started"**
3. Ve a la pestaña **"Sign-in method"** (método de inicio de sesión)
4. Haz clic en **"Google"**
5. Activa el toggle **"Enable"** (habilitar)
6. Ingresa un email de soporte (puede ser tu email personal)
7. Haz clic en **"Guardar"** o **"Save"**

---

### **Paso 7: Crear Firestore Database**

1. En Firebase Console, ve a **"Firestore Database"** (base de datos de Firestore)
2. Haz clic en **"Crear base de datos"** o **"Create database"**
3. Selecciona **"Comenzar en modo de prueba"** (Start in test mode) ⚠️ Para desarrollo
4. Elige la ubicación: **"us-central"** o la más cercana a ti
5. Haz clic en **"Habilitar"** o **"Enable"**

---

### **Paso 8: Configurar Reglas de Firestore (SOLO PARA DESARROLLO)**

⚠️ **IMPORTANTE:** Estas reglas son **PERMISIVAS** y solo deben usarse en desarrollo.

1. En Firestore, ve a la pestaña **"Rules"** (reglas)
2. Reemplaza el contenido con:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true; // ⚠️ PERMISIVO - Solo para desarrollo
    }
  }
}
```

3. Haz clic en **"Publicar"** o **"Publish"**

---

### **Paso 9: Obtener Web Client ID de Firebase**

Este es el ID que necesitas para configurar Google Sign-In.

1. En Firebase Console → **"Configuración del proyecto"** (engranaje)
2. Ve a la pestaña **"General"**
3. Baja hasta **"Tus apps"** → Selecciona tu app Android
4. En **"OAuth 2.0 Client IDs"** encontrarás varios clientes
5. Busca el que tiene **`client_type: 1`** (WEB)
6. **Copia el "Web client ID"** (debe empezar con `xxxxx.apps.googleusercontent.com`)

---

### **Paso 10: Configurar Web Client ID en el Proyecto**

1. Abre el archivo: **`app/src/main/res/values/strings.xml`**
2. Busca la línea:
   ```xml
   <string name="default_web_client_id">TU_WEB_CLIENT_ID_AQUI</string>
   ```
3. Reemplaza `TU_WEB_CLIENT_ID_AQUI` con el **Web Client ID** que copiaste
4. Debe quedar algo así:
   ```xml
   <string name="default_web_client_id">123456789-abcdefghijk.apps.googleusercontent.com</string>
   ```
5. **Guarda el archivo**

---

## ✅ Checklist Final

Antes de ejecutar la app, verifica:

- [ ] Proyecto creado en Firebase Console
- [ ] App Android agregada con Package name: `com.mhrc.appkardex`
- [ ] Archivo `google-services.json` descargado y colocado en `app/google-services.json`
- [ ] SHA-1 obtenido y agregado en Firebase Console
- [ ] Archivo `google-services.json` actualizado después de agregar SHA-1
- [ ] Firebase Authentication habilitado con Google como método de inicio de sesión
- [ ] Firestore Database creado en modo de prueba
- [ ] Reglas de Firestore configuradas (permisivas para desarrollo)
- [ ] Web Client ID copiado y configurado en `strings.xml`
- [ ] Proyecto sincronizado con Gradle Files

---

## 🚀 Ejecutar la App

1. Abre Android Studio
2. Haz clic en **"Sync Project with Gradle Files"** (si aparece)
3. Ejecuta la app: **Run → Run 'app'**

---

## 🧪 Probar la Funcionalidad

### **Registro de Alumno:**
1. Inicia la app
2. Selecciona "Inscribirse"
3. Selecciona "Alumno"
4. Inicia sesión con Google
5. Completa el formulario
6. Verifica que la solicitud aparezca en el panel de admin

### **Registro de Maestro:**
1. Selecciona "Inscribirse"
2. Selecciona "Maestro"
3. Ingresa contraseña: **123456**
4. Inicia sesión con Google
5. Completa el formulario

### **Registro de Admin:**
1. Selecciona "Inscribirse"
2. Selecciona "Administrador"
3. Ingresa contraseña: **7890124**
4. Inicia sesión con Google
5. Serás redirigido automáticamente al panel de admin

### **Panel de Admin:**
1. Acepta o rechaza solicitudes de alumnos
2. Acepta o rechaza solicitudes de maestros

### **Panel de Maestro:**
1. Inicia sesión como maestro aprobado
2. Selecciona un grupo
3. Haz clic en un alumno
4. Pone calificaciones
5. Verifica que se guarden correctamente

### **Panel de Alumno:**
1. Inicia sesión como alumno aprobado
2. Verifica tus calificaciones
3. El botón de PDF está listo para implementar

---

## 🔒 Seguridad para Producción

**⚠️ IMPORTANTE:** Antes de lanzar a producción, debes:

1. **Cambiar las reglas de Firestore** por reglas seguras que verifiquen autenticación
2. **Activar App Check** para evitar abusos
3. **Configurar reglas de acceso** por colección
4. **Usar índices compuestos** para consultas eficientes
5. **Habilitar Firebase Security Rules** basadas en roles de usuario

---

## 🆘 Solución de Problemas

### **Error: "google-services.json not found"**
- Verifica que el archivo esté en `app/google-services.json` (no en la raíz)
- Sincroniza el proyecto: File → Sync Project with Gradle Files

### **Error: "default_web_client_id not found"**
- Verifica que `strings.xml` tenga el valor correcto del Web Client ID
- Asegúrate de que no haya espacios adicionales

### **Error: "Dependency requires at least JVM runtime version 11"**
- Verifica que `compileOptions` tenga `JavaVersion.VERSION_1_8`
- Verifica que Android Studio esté usando Java 8 o superior

### **Google Sign-In no funciona**
- Verifica que el SHA-1 esté correctamente agregado en Firebase
- Descarga nuevamente `google-services.json` después de agregar SHA-1
- Verifica que Firebase Authentication esté habilitado con Google

### **No puedo leer/escribir en Firestore**
- Verifica que las reglas de Firestore estén publicadas
- En desarrollo, usa las reglas permisivas de arriba
- Verifica la conexión a internet

---

## 📚 Recursos Adicionales

- [Documentación de Firebase](https://firebase.google.com/docs)
- [Guía de Google Sign-In](https://firebase.google.com/docs/auth/android/google-signin)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Android Studio Gradle Sync](https://developer.android.com/studio/build)

---

## 📞 Soporte

Si tienes problemas, verifica:
1. Que todos los pasos se hayan completado correctamente
2. Que el Package name coincida exactamente
3. Que el SHA-1 esté correcto
4. Que las reglas de Firestore estén publicadas

¡Éxito con tu proyecto! 🚀

