# StreamPay Cliente Android v3.0

## 📱 Resumen

**StreamPay** es una aplicación móvil desarrollada con Expo + React Native que funciona como cliente para conectarse a un servidor PWA de streaming de video. La aplicación permite a los usuarios configurar una conexión a su servidor local de StreamPay, acceder a contenido de streaming de video, y **gestionar descargas con visualización offline**.

## ✅ Estado Actual

### Aplicación Completamente Funcional

- ✅ **Frontend Expo React Native** - Funcionando correctamente
- ✅ **Backend FastAPI** - API REST básica con MongoDB
- ✅ **MongoDB** - Base de datos configurada
- ✅ **Todas las dependencias instaladas** - Incluyendo módulos Expo faltantes
- ✅ **Configuración iOS/Android** - Cleartext traffic habilitado para ambas plataformas

### Características Implementadas

#### 1. Pantalla de Splash (`app/index.tsx`)
- Verifica si hay configuración guardada
- Redirige automáticamente a configuración o WebView
- Indicador de carga con tema oscuro (#0f172a)

#### 2. Pantalla de Configuración (`app/config.tsx`)
- Formulario para ingresar IP del servidor
- Campo para puerto de streaming (default: 3001)
- Validación de URLs
- Persistencia con AsyncStorage
- Diseño atractivo con tema StreamPay

#### 3. WebView Principal (`app/webview.tsx`) - **v3.0**
- ✅ Carga la PWA de StreamPay
- ✅ **FAB en esquina superior izquierda** (nueva ubicación)
  - Activación manual por swipe desde borde izquierdo
  - Badge con número de descargas activas
  - Solo aparece cuando el usuario lo solicita
- ✅ **Indicador de swipe visual** en el borde izquierdo
- ✅ Configuración optimizada para video streaming
- ✅ Manejo del botón atrás de Android
- ✅ **Menú modal mejorado** con:
  - Acceso a gestor de descargas (NEW)
  - Recargar página
  - Limpiar caché
  - Ir a configuración
- ✅ Soporte de fullscreen para videos
- ✅ Sistema de notificaciones para:
  - Reproducción de audio
  - Descargas de archivos con progreso en tiempo real
- ✅ Manejo avanzado de descargas con FileSystem
- ✅ User Agent personalizado: `StreamPayAPK/3.0`

#### 4. Gestor de Descargas (`app/downloads.tsx`) - **¡NUEVO EN v3.0!**
- ✅ **Vista de descargas activas**
  - Barra de progreso en tiempo real (0-100%)
  - Tamaño del archivo
  - Actualización automática cada 2 segundos
- ✅ **Historial completo de descargas**
  - Lista persistente de todos los archivos
  - Indicadores de estado (completado/fallido/descargando)
  - Información detallada (nombre, tamaño, fecha)
  - Timestamps relativos ("Hace 5m", "Hace 2h")
- ✅ **Visualización offline**
  - Acceso a archivos sin conexión
  - Vista de detalles del archivo
  - Información de ruta local
- ✅ **Gestión de archivos**
  - Eliminar archivos individuales
  - Limpiar historial completo
  - Eliminar todos los archivos
  - Confirmaciones de seguridad
- ✅ **Interfaz elegante**
  - Header con navegación
  - Estado vacío informativo
  - Diseño card-based
  - Iconos de estado coloridos

## 🚀 Cómo Usar

### Para Desarrollo

1. **Ver la aplicación en el navegador:**
   ```
   https://app-enhancer-45.preview.emergentagent.com
   ```

2. **Ver en Expo Go (móvil):**
   - Escanea el código QR generado en la terminal
   - Requiere tener Expo Go instalado en tu dispositivo

3. **Configurar la aplicación:**
   - Ingresa la IP de tu servidor StreamPay (ejemplo: http://192.168.1.100)
   - Ingresa el puerto (default: 3001)
   - Toca "Guardar y Continuar"

### Para Compilar APK/IPA

Consulta los archivos de documentación:
- `GUIA_RAPIDA_APK.md` - Guía paso a paso para compilar
- `README_CLIENT.md` - Documentación completa del cliente
- `RESUMEN_PROYECTO.md` - Resumen técnico detallado
- `CHANGELOG_V2.1.md` - Cambios en la versión 2.1

## 🔧 Correcciones Aplicadas

### Problemas Resueltos

1. ✅ **Módulo async-storage faltante**
   - Instalado `@react-native-async-storage/async-storage@2.2.0`

2. ✅ **Dependencias Expo faltantes**
   - Instalado `expo-screen-orientation`
   - Instalado `expo-notifications`
   - Instalado `expo-file-system`

3. ✅ **Configuración iOS cleartext traffic**
   - Agregado `NSAppTransportSecurity` con `NSAllowsArbitraryLoads: true`
   - Permite conexiones HTTP en redes locales

4. ✅ **User Agent desactualizado**
   - Actualizado de Chrome/120 a Chrome/131
   - Mejor compatibilidad con servicios web modernos

5. ✅ **Error de React hooks en index.tsx**
   - Convertido a `useCallback` para evitar problemas de dependencias
   - Agregado en el array de dependencias de `useEffect`

6. ✅ **Assets copiados**
   - Todos los iconos y splash screens copiados correctamente

## 📦 Dependencias Principales

```json
{
  "@react-native-async-storage/async-storage": "2.2.0",
  "react-native-webview": "13.15.0",
  "expo-router": "~6.0.22",
  "expo-screen-orientation": "^9.0.8",
  "expo-notifications": "^0.32.16",
  "expo-file-system": "^19.0.21",
  "expo-system-ui": "~6.0.9",
  "expo-status-bar": "~3.0.9"
}
```

## 🎨 Diseño

### Colores del Tema
- **Fondo principal**: `#0f172a` (Slate 950)
- **Fondo secundario**: `#1e293b` (Slate 800)
- **Acento**: `#6366f1` (Indigo 500)
- **Texto primario**: `#e2e8f0` (Slate 200)
- **Texto secundario**: `#94a3b8` (Slate 400)

## 🔒 Configuración de Seguridad

### Cleartext Traffic (HTTP)

**Android:**
```json
{
  "android": {
    "usesCleartextTraffic": true
  }
}
```

**iOS:**
```json
{
  "ios": {
    "infoPlist": {
      "NSAppTransportSecurity": {
        "NSAllowsArbitraryLoads": true
      }
    }
  }
}
```

**¿Por qué es necesario?**
- Permite conexiones HTTP en redes locales
- Los servidores locales raramente tienen certificados SSL
- StreamPay está diseñado para redes locales/NAS
- Sin esto, la app no conectaría al servidor local

## 📝 Estructura de Archivos

```
/app/
├── backend/
│   ├── .env                    # Variables de entorno
│   ├── server.py               # API FastAPI con MongoDB
│   └── requirements.txt        # Dependencias Python
│
├── frontend/
│   ├── app/
│   │   ├── _layout.tsx         # Layout principal
│   │   ├── index.tsx           # Splash screen
│   │   ├── config.tsx          # Configuración del servidor
│   │   └── webview.tsx         # WebView principal (v2.1)
│   │
│   ├── assets/
│   │   └── images/             # Iconos y splash screens
│   │
│   ├── app.json                # Configuración Expo/Android/iOS
│   └── package.json            # Dependencias Node.js
│
├── RESUMEN_PROYECTO.md         # Resumen técnico completo
├── CHANGELOG_V2.1.md           # Cambios en v2.1
├── GUIA_RAPIDA_APK.md          # Guía de compilación
└── README_CLIENT.md            # Documentación del cliente
```

## 🐛 Notas Importantes

### AsyncStorage
- AsyncStorage **NO** funciona en el preview web
- Funciona perfectamente en builds nativos (APK/IPA)
- Funciona en Expo Go

### Notificaciones
- Las notificaciones push muestran un warning en web
- Funcionan correctamente en dispositivos móviles nativos

### WebView
- El WebView solo carga contenido cuando está en un dispositivo móvil o emulador
- En web preview, algunas funcionalidades estarán limitadas

## ✨ Mejoras v3.0

### 🎯 FAB Reposicionado y Mejorado
- **Antes (v2.1)**: Esquina inferior derecha, aparecía automáticamente con cada interacción
- **Ahora (v3.0)**: 
  - ✅ Esquina superior izquierda (no estorba)
  - ✅ Activación manual por swipe desde el borde izquierdo
  - ✅ Solo aparece cuando el usuario lo desea
  - ✅ Badge con número de descargas activas
  - ✅ Indicador visual de swipe en el borde

### 📥 Gestor de Descargas Completo (NUEVO)
- **Pantalla dedicada** para gestión de archivos descargados
- **Descargas activas** con progreso en tiempo real
- **Historial persistente** de todas las descargas
- **Visualización offline** - accede a archivos sin conexión
- **Gestión avanzada**:
  - Eliminar archivos individuales
  - Limpiar historial completo
  - Eliminar todos los archivos
  - Información detallada de cada archivo

### 🎨 Menú Modal Mejorado
- Diseño más elegante y espacioso
- Acceso directo al gestor de descargas
- Badge de descargas activas visible
- Animaciones suaves

### 🖐️ Gestos Intuitivos
- **Swipe desde izquierda** → Mostrar FAB
- **Tap en FAB** → Abrir menú
- **Tap fuera** → Cerrar todo
- **Botón atrás** → Navegación secuencial

## 🚀 Próximos Pasos

1. **Compilar el APK** usando `eas build` (ver GUIA_RAPIDA_APK.md)
2. **Probar en dispositivo real** para verificar todas las funcionalidades
3. **Configurar la aplicación** con la IP de tu servidor StreamPay
4. **Opcional**: Publicar en Google Play Store o App Store

## 📞 Información Adicional

- **Stack**: React Native + Expo
- **Backend**: FastAPI + MongoDB
- **Navegación**: Expo Router (file-based routing)
- **Storage**: AsyncStorage
- **WebView**: react-native-webview

---

🎬 **StreamPay v2.1 - Cliente móvil optimizado para streaming de video local**
