# 🚀 Guía Rápida: Compilar APK de StreamPay

## ⚡ Pasos Rápidos

### 1️⃣ Preparación Inicial (Solo una vez)

```bash
# Instala EAS CLI globalmente
npm install -g eas-cli

# Navega a la carpeta del proyecto
cd frontend

# Inicia sesión en Expo (crea cuenta en expo.dev si no tienes)
eas login

# Inicializa EAS en el proyecto
eas init
```

### 2️⃣ Compilar el APK

```bash
# Desde la carpeta frontend/
eas build --platform android --profile preview
```

Esto iniciará el build en los servidores de Expo. Tomará entre 10-15 minutos.

### 3️⃣ Descargar el APK

Una vez completado, recibirás:
- ✉️ Un email con el enlace de descarga
- 🔗 Un link directo en la terminal
- 📱 Podrás verlo en: https://expo.dev

---

## 🤖 Automatizar con GitHub Actions

### 1️⃣ Configura el Token de Expo

```bash
# Genera un token en: https://expo.dev/accounts/[tu-usuario]/settings/access-tokens
# Copia el token
```

### 2️⃣ Agrega el Secret en GitHub

1. Ve a tu repositorio en GitHub
2. **Settings** → **Secrets and variables** → **Actions**
3. Click en **New repository secret**
4. Nombre: `EXPO_TOKEN`
5. Valor: [pega tu token]
6. Click en **Add secret**

### 3️⃣ Ejecuta el Workflow

1. Ve a la pestaña **Actions** en GitHub
2. Selecciona **Build Android APK**
3. Click en **Run workflow** → **Run workflow**
4. Espera ~10-15 minutos
5. Descarga el APK desde expo.dev

---

## 📋 Checklist Antes de Compilar

- [ ] Cuenta creada en expo.dev
- [ ] EAS CLI instalado (`npm install -g eas-cli`)
- [ ] Sesión iniciada (`eas login`)
- [ ] Proyecto inicializado (`eas init`)
- [ ] (Opcional) Token de Expo en GitHub Secrets

---

## ✅ Perfiles de Compilación

El archivo `eas.json` tiene 3 perfiles:

- **preview**: APK para testing (recomendado para distribución manual)
- **production**: APK optimizado para Play Store
- **development**: Build con herramientas de desarrollo

---

## 🔍 Comandos Útiles

```bash
# Ver estado de builds
eas build:list

# Ver builds en progreso
eas build:list --status in-progress

# Compilar para producción
eas build --platform android --profile production

# Compilar sin esperar (ejecuta en segundo plano)
eas build --platform android --profile preview --non-interactive
```

---

## 🐛 Problemas Comunes

### "No project ID found"
```bash
cd frontend
eas init
```

### "Not logged in"
```bash
eas login
```

### "Build failed"
Revisa los logs en:
```bash
eas build:list
# Click en el build fallido para ver detalles
```

---

## 📱 Instalar el APK

1. Descarga el archivo `.apk` en tu teléfono Android
2. Abre el archivo
3. Si aparece "Instalar de fuentes desconocidas", actívalo
4. Presiona **Instalar**
5. ¡Listo! Abre StreamPay

---

## 🎯 Próximos Pasos

Una vez instalada la app:

1. **Primera vez**: Configura la IP del servidor
   - URL: `http://192.168.43.101` (o tu IP)
   - Puerto: `3001`

2. **Navegar**: La app se comporta como el sitio web

3. **Cambiar servidor**: Toca ⚙️ → Cambiar Servidor

---

**¿Necesitas ayuda?** Revisa el README_CLIENT.md completo para más detalles.
