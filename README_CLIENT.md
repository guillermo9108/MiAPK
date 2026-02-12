# StreamPay APK Client

Cliente Android nativo para StreamPay - Plataforma de Video Streaming y E-commerce.

## 📱 Características

- ✅ WebView nativo optimizado para StreamPay
- ✅ Configuración dinámica de IP del servidor
- ✅ Soporte para HTTP (cleartext traffic) en redes locales
- ✅ Persistencia de sesión y carrito de compras
- ✅ Reproducción de video sin interrupciones
- ✅ Orientación vertical fija
- ✅ Tema dark (#0f172a) consistente
- ✅ Manejo inteligente del botón atrás
- ✅ Pantalla de configuración integrada

## 🚀 Compilar el APK

### Opción 1: Usando GitHub Actions (Recomendado)

1. **Crea una cuenta en Expo**
   - Visita [https://expo.dev](https://expo.dev)
   - Crea una cuenta gratuita

2. **Obtén tu token de Expo**
   ```bash
   npx expo login
   npx eas login
   npx eas whoami
   # Genera un token en: https://expo.dev/accounts/[tu-usuario]/settings/access-tokens
   ```

3. **Configura el secreto en GitHub**
   - Ve a tu repositorio en GitHub
   - Settings → Secrets and variables → Actions
   - Crea un nuevo secret llamado `EXPO_TOKEN`
   - Pega tu token de Expo

4. **Actualiza el projectId en app.json**
   - Ejecuta en tu terminal:
     ```bash
     cd frontend
     npx eas init
     ```
   - Esto actualizará automáticamente el `projectId` en `app.json`

5. **Push al repositorio**
   ```bash
   git add .
   git commit -m "Add StreamPay Android client"
   git push origin main
   ```

6. **Inicia el build**
   - Ve a la pestaña "Actions" en GitHub
   - Selecciona "Build Android APK"
   - Click en "Run workflow"
   - Espera ~10-15 minutos
   - Descarga el APK desde Expo: [https://expo.dev](https://expo.dev)

### Opción 2: Build Local

1. **Instala EAS CLI**
   ```bash
   npm install -g eas-cli
   ```

2. **Inicia sesión en Expo**
   ```bash
   eas login
   ```

3. **Configura el proyecto**
   ```bash
   cd frontend
   eas init
   ```

4. **Construye el APK**
   ```bash
   eas build --platform android --profile preview
   ```

5. **Descarga el APK**
   - Una vez completado, recibirás un enlace para descargar
   - También puedes verlo en: https://expo.dev/accounts/[tu-usuario]/projects/streampay/builds

## 📦 Estructura del Proyecto

```
frontend/
├── app/
│   ├── _layout.tsx        # Layout principal con tema dark
│   ├── index.tsx          # Splash screen y router inicial
│   ├── config.tsx         # Pantalla de configuración de servidor
│   └── webview.tsx        # WebView principal de StreamPay
├── assets/
│   └── images/            # Iconos y splash screen
├── app.json               # Configuración de Expo
├── eas.json              # Configuración de builds
└── package.json

.github/
└── workflows/
    └── build-apk.yml      # GitHub Actions workflow
```

## 🔧 Configuración del Servidor

### Primera Vez

Cuando abras la app por primera vez, verás la pantalla de configuración:

1. **URL del Servidor**: Ingresa la IP de tu servidor
   - Ejemplo: `http://192.168.43.101`
   - Debe incluir `http://` o `https://`

2. **Puerto de Streaming**: El puerto del servicio de video
   - Por defecto: `3001`

3. Click en "Guardar y Continuar"

### Cambiar Configuración

Desde el WebView principal:
- Toca el ícono de ajustes (⚙️) en la esquina superior derecha
- Selecciona "Cambiar Servidor"
- Actualiza la configuración

## 🎨 Personalización

### Cambiar Colores

Edita los colores en los archivos:
- `app/config.tsx` - Pantalla de configuración
- `app/webview.tsx` - WebView principal
- `app.json` - Splash screen y colores del sistema

Color principal actual: `#0f172a` (Slate 950)
Color de acento: `#6366f1` (Indigo 500)

### Cambiar Iconos

Reemplaza las imágenes en `assets/images/`:
- `icon.png` - Ícono de la app (1024x1024)
- `adaptive-icon.png` - Ícono adaptativo Android (1024x1024)
- `splash-icon.png` - Logo del splash screen (400x400)
- `favicon.png` - Favicon para web (48x48)

## 📱 Requisitos de la App

### Permisos Android

La app solicita los siguientes permisos:
- `INTERNET` - Conexión al servidor
- `ACCESS_NETWORK_STATE` - Verificar estado de la red
- `CAMERA` - Para subir fotos en el marketplace
- `READ_EXTERNAL_STORAGE` - Leer archivos
- `WRITE_EXTERNAL_STORAGE` - Guardar archivos

### Requisitos del Servidor

El servidor StreamPay debe:
- Estar accesible en la red local
- Servir la PWA en el puerto principal (80 o especificado)
- Tener el servicio de streaming en el puerto 3001
- Permitir tráfico HTTP (cleartext)

## 🐛 Solución de Problemas

### La app no se conecta al servidor

1. Verifica que el servidor esté activo
2. Asegúrate de estar en la misma red WiFi
3. Verifica que la URL sea correcta (incluyendo `http://`)
4. Prueba la URL en el navegador del teléfono primero

### Error al compilar

1. Verifica que `EXPO_TOKEN` esté configurado en GitHub
2. Asegúrate de haber ejecutado `eas init` antes del push
3. Revisa los logs en la pestaña Actions de GitHub

### El video no se reproduce

1. Verifica que el puerto de streaming (3001) esté abierto
2. Asegúrate de que el firewall permita el tráfico
3. Prueba acceder a `http://[tu-ip]:3001` desde el navegador

## 📄 Configuración Técnica

### WebView Settings

La app está configurada con:
- `javaScriptEnabled: true` - Permite JavaScript
- `domStorageEnabled: true` - LocalStorage para sesiones
- `mediaPlaybackRequiresUserAction: false` - Autoplay de videos
- `mixedContentMode: "always"` - Permite HTTP y HTTPS
- User Agent personalizado: `StreamPayAPK/1.0`

### Android Manifest

Configuración especial en `app.json`:
- `usesCleartextTraffic: true` - Permite HTTP
- `orientation: "portrait"` - Solo vertical
- `backgroundColor: "#0f172a"` - Color del sistema

## 🤝 Contribuir

Este proyecto es parte del ecosistema StreamPay. Para contribuir:

1. Fork el repositorio
2. Crea una rama para tu feature
3. Commit tus cambios
4. Push a la rama
5. Abre un Pull Request

## 📝 Licencia

Este cliente es parte del proyecto StreamPay.

## 🔗 Enlaces

- Repositorio StreamPay: https://github.com/guillermo9108/YouTube
- Documentación Expo: https://docs.expo.dev
- EAS Build: https://docs.expo.dev/build/introduction

---

**Desarrollado para StreamPay** 🎬
Plataforma de Video Streaming y E-commerce Local
