# 📱 Cliente Android StreamPay - Resumen del Proyecto

## ✅ Lo que se ha creado

### 1. Aplicación Expo React Native completa

**Pantallas implementadas:**

#### 🔄 Splash Screen (`app/index.tsx`)
- Verifica si hay configuración guardada
- Redirige a configuración o WebView según corresponda
- Indicador de carga con tema dark

#### ⚙️ Configuración (`app/config.tsx`)
- Formulario para ingresar IP del servidor
- Campo para puerto de streaming (default: 3001)
- Validación de URLs
- Persistencia con AsyncStorage
- Diseño atractivo con tema StreamPay

#### 🌐 WebView Principal (`app/webview.tsx`)
- Carga la PWA de StreamPay
- Configuración optimizada para video streaming
- Manejo del botón atrás de Android
- Barra superior con logo y botón de ajustes
- Indicador de carga
- Pantalla de error con retry
- User Agent personalizado: `StreamPayAPK/1.0`

#### 🎨 Layout (`app/_layout.tsx`)
- Configuración de navegación
- Sistema de colores dark (#0f172a)
- Manejo de StatusBar

### 2. Configuración Android Completa

**app.json configurado con:**
- ✅ `usesCleartextTraffic: true` - Permite HTTP
- ✅ Orientación portrait fija
- ✅ Permisos: INTERNET, CAMERA, STORAGE, NETWORK_STATE
- ✅ Tema dark (#0f172a)
- ✅ Package: `com.streampay.app`
- ✅ Iconos y splash screen configurados

### 3. GitHub Actions Workflow

**`.github/workflows/build-apk.yml`:**
- ✅ Build automático en cada push a main
- ✅ Build manual con `workflow_dispatch`
- ✅ Integración con EAS Build
- ✅ Notificaciones de estado

### 4. Archivos de Configuración

**`eas.json`:**
- Perfil `preview` - APK para distribución manual
- Perfil `production` - APK para Play Store
- Perfil `development` - APK con dev tools

### 5. Documentación Completa

**Archivos creados:**
- ✅ `README_CLIENT.md` - Documentación completa en inglés
- ✅ `GUIA_RAPIDA_APK.md` - Guía rápida en español
- ✅ `build-apk.sh` - Script interactivo de compilación

---

## 🚀 Cómo Compilar el APK

### Método 1: Script Automático

```bash
./build-apk.sh
```

Este script te guiará paso a paso.

### Método 2: Manual Rápido

```bash
# 1. Instalar EAS CLI
npm install -g eas-cli

# 2. Login
cd frontend
eas login

# 3. Inicializar
eas init

# 4. Compilar
eas build --platform android --profile preview
```

### Método 3: GitHub Actions

1. Crear cuenta en expo.dev
2. Generar token en: https://expo.dev/settings/access-tokens
3. Agregar token como secret `EXPO_TOKEN` en GitHub
4. En tu repo: Actions → Build Android APK → Run workflow
5. Descargar APK de expo.dev

---

## 📋 Características Implementadas

### ✅ Requisitos del README_APK.md

| Requisito | Estado | Implementación |
|-----------|--------|----------------|
| Cleartext traffic (HTTP) | ✅ | `app.json` → `usesCleartextTraffic: true` |
| Orientación portrait | ✅ | `app.json` → `orientation: "portrait"` |
| Tema dark (#0f172a) | ✅ | Todos los componentes + StatusBar |
| IP configurable | ✅ | Pantalla de configuración completa |
| Persistencia de sesión | ✅ | WebView con `domStorageEnabled: true` |
| Streaming sin interrupciones | ✅ | `mediaPlaybackRequiresUserAction: false` |
| Manejo botón atrás | ✅ | `webview.tsx` → `BackHandler` |
| Permisos Android | ✅ | INTERNET, CAMERA, STORAGE, etc. |
| StatusBar color | ✅ | `expo-system-ui` con color #0f172a |
| User Agent personalizado | ✅ | `StreamPayAPK/1.0` |

### ✅ Funcionalidades Extra

- 🎯 Pantalla de error con retry automático
- 🔄 Botón de recarga en ajustes
- 🎨 Logo animado (SP) en todos los screens
- 📱 Diseño responsive y nativo
- ⚡ Optimización de rendimiento
- 🔐 Validación de URLs
- 💾 Persistencia de configuración
- 🎨 UI/UX pulida y profesional

---

## 📁 Estructura de Archivos

```
/app/
├── .github/
│   └── workflows/
│       └── build-apk.yml         # GitHub Actions workflow
│
├── frontend/
│   ├── app/
│   │   ├── _layout.tsx           # Layout principal
│   │   ├── index.tsx             # Splash screen
│   │   ├── config.tsx            # Configuración de servidor
│   │   └── webview.tsx           # WebView principal
│   │
│   ├── app.json                  # Configuración Expo/Android
│   ├── eas.json                  # Configuración EAS Build
│   └── package.json              # Dependencias
│
├── README_CLIENT.md              # Documentación completa
├── GUIA_RAPIDA_APK.md           # Guía rápida español
└── build-apk.sh                 # Script de compilación
```

---

## 🔧 Configuración Técnica

### WebView Settings

```typescript
javaScriptEnabled: true              // Ejecutar JS
domStorageEnabled: true              // LocalStorage (sesión/carrito)
allowFileAccess: true                // Acceso a archivos
mediaPlaybackRequiresUserAction: false  // Autoplay videos
mixedContentMode: "always"           // HTTP + HTTPS
cacheEnabled: true                   // Cache de recursos
androidLayerType: "hardware"         // Aceleración hardware
```

### Android Permissions

```json
[
  "INTERNET",              // Conexión al servidor
  "ACCESS_NETWORK_STATE",  // Estado de red
  "CAMERA",                // Fotos marketplace
  "READ_EXTERNAL_STORAGE", // Leer archivos
  "WRITE_EXTERNAL_STORAGE" // Guardar archivos
]
```

---

## 🎨 Diseño y UX

### Colores del Tema

- **Fondo principal**: `#0f172a` (Slate 950)
- **Fondo secundario**: `#1e293b` (Slate 800)
- **Acento**: `#6366f1` (Indigo 500)
- **Texto primario**: `#e2e8f0` (Slate 200)
- **Texto secundario**: `#94a3b8` (Slate 400)

### Componentes UI

- Logo circular con iniciales "SP"
- Botones con border-radius de 12px
- Inputs con borde y padding generoso
- Indicadores de carga con el color de acento
- Íconos de Ionicons (Material Design)

---

## 🧪 Testing

### Para probar en desarrollo:

```bash
cd frontend
yarn start
```

Luego escanea el QR con Expo Go.

**Nota:** AsyncStorage no funciona en web preview, pero funcionará perfectamente en el APK compilado.

---

## 📦 Dependencias Principales

```json
{
  "react-native-webview": "13.15.0",
  "@react-native-async-storage/async-storage": "2.2.0",
  "expo-router": "~6.0.22",
  "expo-system-ui": "~6.0.9",
  "expo-status-bar": "~3.0.9"
}
```

---

## ⚠️ Notas Importantes

### 1. Primera Compilación

La primera vez que compiles, debes:
```bash
cd frontend
eas init
```

Esto genera un `projectId` único en `app.json`.

### 2. Iconos

Los iconos por defecto están en `frontend/assets/images/`.
Para personalizar:
- `icon.png` - 1024x1024px
- `adaptive-icon.png` - 1024x1024px
- `splash-icon.png` - 400x400px

### 3. Package Name

Si cambias el package name en `app.json`:
- iOS: `bundleIdentifier`
- Android: `package`

Debe ser único (ejemplo: `com.tuempresa.streampay`).

### 4. Versiones

Para actualizar la versión:
- `version`: "1.0.0" → "1.0.1"
- `versionCode` (Android): 1 → 2

---

## 🎯 Próximos Pasos

### Para el Usuario:

1. **Compilar el APK**
   - Usa el script `build-apk.sh`
   - O sigue la guía en `GUIA_RAPIDA_APK.md`

2. **Instalar en tu dispositivo**
   - Descarga el APK de expo.dev
   - Transfiere al teléfono
   - Instala (habilita "fuentes desconocidas")

3. **Configurar la app**
   - Abre StreamPay
   - Ingresa la IP: `http://192.168.43.101`
   - Puerto: `3001`
   - ¡Disfruta!

### Para Personalizar:

1. **Cambiar colores**
   - Edita los estilos en cada archivo .tsx
   - Actualiza `app.json` → `backgroundColor`

2. **Agregar funcionalidades**
   - Push notifications
   - Deep linking
   - Biometric authentication
   - Offline mode

3. **Publicar en Play Store**
   - Usa el perfil `production`
   - Sigue la guía de Google Play Console

---

## 🐛 Troubleshooting

### Error: "Cannot find module async-storage"
**Solución:** Ya está instalado, funciona solo en builds nativos, no en web.

### Error: "Build failed"
**Solución:** 
```bash
cd frontend
rm -rf node_modules
yarn install
eas build --clear-cache --platform android
```

### Error: "No project ID"
**Solución:**
```bash
cd frontend
eas init
```

### La app no conecta al servidor
**Checklist:**
- [ ] Servidor activo
- [ ] Misma red WiFi
- [ ] URL correcta con `http://`
- [ ] Puerto 3001 abierto
- [ ] Firewall permite tráfico

---

## 📞 Soporte

Para más información:
- Documentación Expo: https://docs.expo.dev
- EAS Build: https://docs.expo.dev/build/introduction
- Repo StreamPay: https://github.com/guillermo9108/YouTube

---

## ✨ Créditos

Cliente APK desarrollado para **StreamPay**
Plataforma de Video Streaming y E-commerce Local

**Stack:**
- React Native + Expo
- TypeScript
- Expo Router
- React Native WebView

**Desarrollado con ❤️ para la comunidad StreamPay**

---

🎬 **¡Tu plataforma de streaming ahora en tu bolsillo!**
