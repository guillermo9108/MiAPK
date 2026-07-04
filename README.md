# StreamPay 🚀

StreamPay es una aplicación de Android moderna y de alto rendimiento diseñada para la descarga y reproducción fluida de contenidos en streaming. Desarrollada completamente en **Kotlin** y **Jetpack Compose**, combina una interfaz de usuario fluida con la potencia del reproductor multimedia ExoPlayer.

---

## 📥 Descarga de la Aplicación

> [!TIP]
> Esta sección se actualiza automáticamente con cada cambio subido al repositorio a través de GitHub Actions. ¡Siempre obtendrás la última versión compilada!

### **Última Compilación Disponible**

- **Nombre de la APK:** `StreamPay-v1.0.0.apk`
- **Versión:** <!-- APK_VERSION_TEXT_START -->`v1.0.3`<!-- APK_VERSION_TEXT_END -->
- **Repositorio:** Compilado desde la rama principal.

---

### 🚀 **[¡DESCARGAR APK ACTUALIZADA!]**

<!-- DOWNLOAD_BUTTON_START -->
[![Descargar APK](https://img.shields.io/badge/Download-APK%20v1.0.3-green?style=for-the-badge&logo=android&logoColor=white)](https://github.com/guillermo910802/StreamPay/releases/download/v1.0.3/StreamPay-v1.0.3.apk)
<!-- DOWNLOAD_BUTTON_END -->

*Enlace directo al archivo compilado:*
<!-- DOWNLOAD_URL_START -->
🔗 [StreamPay-v1.0.3.apk](https://github.com/guillermo910802/StreamPay/releases/download/v1.0.3/StreamPay-v1.0.3.apk)
<!-- DOWNLOAD_URL_END -->

> [!IMPORTANT]
> **¿El enlace directo da error 404?**
> Si el enlace anterior da un error 404, puedes acceder y descargar el archivo APK compilado de forma 100% garantizada e inmediata en la sección de lanzamientos de tu repositorio de GitHub:
> - 📦 **[Ir a todos los Lanzamientos / Releases de tu Repositorio](../../releases)**
> - 🚀 **[Ver el Último Lanzamiento Disponible](../../releases/latest)**
>
> *(Nota: Sigue las instrucciones de configuración a continuación para dar permisos de escritura a GitHub Actions y que el README se actualice solo).*

---

## ✨ Características Principales

- **Gestor de Descargas Avanzado:** Descarga tus vídeos de streaming y guárdalos en la ruta que prefieras (almacenamiento interno seguro o ruta externa personalizada).
- **Control Inteligente de Permisos:** Manejo proactivo y moderno de permisos de almacenamiento en tiempo real (ajustado a Android 13+ SDK 34 / Tiramisu).
- **Reproductor ExoPlayer Integrado:** Reproducción offline directa y ultra fluida de tus archivos descargados, con controles nativos e intuitivos.
- **Visuales Material 3:** Temas e interfaces modernos, navegación fluida, indicadores de progreso optimizados y accesibilidad nativa.
- **CI/CD Automatizado:** Pipeline integrado con GitHub Actions que compila, incrementa el número de versión e inyecta la descarga directamente en este archivo en cada cambio.

---

## 🛠️ Tecnologías y Librerías Utilizadas

- **Kotlin** y **Jetpack Compose** para la interfaz declarativa moderna.
- **AndroidX Media3 ExoPlayer** para la reproducción de alto rendimiento.
- **Room Database** para la persistencia local de descargas y configuraciones.
- **Retrofit & Coil** para operaciones de red y carga dinámica de imágenes.
- **Navigation Compose** para ruteo tipado y navegación fluida tipo Single-Activity.

---

## 💻 Configuración de Compilación Local

Si prefieres compilar la aplicación localmente:

1. Clona este repositorio:
   ```bash
   git clone https://github.com/guillermo910802/StreamPay.git
   ```
2. Abre el proyecto en **Android Studio**.
3. Asegúrate de tener configurado **JDK 17**.
4. Sincroniza Gradle y ejecuta la tarea de compilación:
   ```bash
   ./gradlew assembleDebug
   ```
5. El APK compilado se generará en la ruta:
   `app/build/outputs/apk/debug/StreamPay-v1.0.[versión].apk`
