# 🎯 StreamPay v2.1 - FAB Auto-Ocultable

## ✅ Problema Resuelto

**Antes (v2.0):**
- FAB siempre visible en esquina inferior derecha
- Podía tapar botones de la página web
- Molesto para la experiencia de usuario

**Ahora (v2.1):**
- ✅ FAB se oculta automáticamente
- ✅ Solo aparece cuando el usuario interactúa
- ✅ Desaparece después de 3 segundos
- ✅ No interfiere con el contenido web

---

## 🔧 Cómo Funciona

### Detección de Interacciones

El FAB aparece cuando el usuario:

1. **Hace scroll** (más de 50px)
2. **Toca la pantalla**
3. **Interactúa con cualquier elemento**

### Auto-Ocultación

- ⏱️ Se oculta automáticamente después de **3 segundos** de inactividad
- 🔄 Si abres el menú, se mantiene visible
- 👆 Si tocas fuera del menú, se cierra todo
- 📺 En modo fullscreen, está completamente oculto

---

## 💡 Comportamiento del Usuario

### Caso 1: Navegación Normal
```
1. Usuario hace scroll en la página
2. FAB aparece (⋮)
3. Usuario sigue navegando
4. Después de 3 segundos → FAB desaparece
5. Usuario hace scroll de nuevo
6. FAB vuelve a aparecer
```

### Caso 2: Usar el Menú
```
1. Usuario hace scroll
2. FAB aparece
3. Usuario toca el FAB
4. Menú se abre
5. FAB permanece visible mientras el menú está abierto
6. Usuario toca fuera del menú
7. Menú y FAB desaparecen
```

### Caso 3: Ver Video en Fullscreen
```
1. Usuario entra en modo fullscreen
2. FAB se oculta completamente
3. StatusBar también se oculta
4. Usuario sale de fullscreen
5. FAB vuelve a su comportamiento normal
```

---

## 🎨 Características Visuales

### Posición
- **Bottom**: 24px desde el borde inferior
- **Right**: 16px desde el borde derecho
- **Z-index**: Por encima del WebView pero sin interferir

### Diseño
- **Color**: #6366f1 (Indigo 500)
- **Tamaño**: 48x48px
- **Border radius**: 24px (perfectamente circular)
- **Elevación**: 8 (sombra marcada)
- **Ícono**: ⋮ (tres puntos verticales)

### Animación
- Aparece/desaparece suavemente
- Sin animación brusca
- Transición natural

---

## 🔧 Código Técnico

### JavaScript Injection

```javascript
// Detectar scroll (> 50px cambio)
let lastScroll = 0;
window.addEventListener('scroll', () => {
  const currentScroll = window.pageYOffset;
  if (Math.abs(currentScroll - lastScroll) > 50) {
    notifyInteraction(); // Muestra FAB
    lastScroll = currentScroll;
  }
}, { passive: true });

// Detectar touch
document.addEventListener('touchstart', notifyInteraction, { passive: true });
```

### React Native

```typescript
const [showFab, setShowFab] = useState(false);
const hideTimeout = useRef<NodeJS.Timeout | null>(null);

const showFabTemporarily = () => {
  setShowFab(true);
  
  // Limpiar timeout anterior
  if (hideTimeout.current) {
    clearTimeout(hideTimeout.current);
  }
  
  // Ocultar después de 3 segundos
  hideTimeout.current = setTimeout(() => {
    setShowFab(false);
    setShowMenu(false);
  }, 3000);
};
```

---

## 📱 Experiencia Mejorada

### Ventajas

1. **No Obstruye**: El contenido web siempre es visible
2. **Intuitivo**: Aparece cuando lo necesitas
3. **Discreto**: Se oculta cuando no lo usas
4. **Rápido**: Aparece instantáneamente al interactuar
5. **No Molesta**: No está siempre visible

### Casos de Uso Reales

**Navegando un listado de videos:**
- El FAB no está visible
- Puedes ver todos los videos sin obstrucciones
- Si necesitas recargar, haces scroll → FAB aparece

**Viendo un video:**
- Entras en fullscreen → FAB oculto
- Sales de fullscreen → FAB vuelve si interactúas
- Si no necesitas el menú, no lo ves

**Leyendo contenido:**
- El FAB no interfiere con el texto
- Solo aparece si haces scroll
- Se oculta solo mientras lees

---

## ⚙️ Configuración

### Tiempo de Auto-Ocultación

Por defecto: **3000ms (3 segundos)**

Para cambiar:
```typescript
// En webview.tsx
hideTimeout.current = setTimeout(() => {
  setShowFab(false);
  setShowMenu(false);
}, 5000); // Cambiar a 5 segundos
```

### Sensibilidad del Scroll

Por defecto: **50px**

Para cambiar:
```javascript
// En injectedJavaScript
if (Math.abs(currentScroll - lastScroll) > 100) {
  // Cambiar a 100px para menos sensibilidad
  notifyInteraction();
  lastScroll = currentScroll;
}
```

---

## 🐛 Casos Especiales

### FAB No Aparece
**Causa**: No se detectan interacciones
**Solución**: Verificar JavaScript injection en WebView

### FAB Siempre Visible
**Causa**: Timeout no funciona
**Solución**: Verificar que `hideTimeout.current` se limpie correctamente

### FAB Aparece Demasiado
**Causa**: Sensibilidad muy alta
**Solución**: Aumentar umbral de scroll de 50px a 100px

---

## 🔒 Cleartext Traffic

### Configuración HTTP (Entornos Locales)

**Importancia:** Permite conexión HTTP sin certificados SSL en redes locales.

**Ya Configurado en app.json:**
```json
{
  "android": {
    "usesCleartextTraffic": true
  },
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
- Servidores locales raramente tienen certificados SSL
- IPs locales (192.168.x.x) no pueden usar HTTPS fácilmente
- StreamPay está diseñado para redes locales/NAS
- Sin esto, la app no conectaría al servidor

**Seguridad:**
- Solo afecta a conexiones dentro de la app
- El sistema operativo aún protege otras apps
- Recomendado solo para uso en red local
- Para producción pública, usar HTTPS

---

## 📊 Comparación de Versiones

| Característica | v2.0 | v2.1 |
|----------------|------|------|
| FAB visible | Siempre | Auto-oculta |
| Obstrucción | Posible | Nunca |
| Tiempo de ocultación | N/A | 3 segundos |
| Detecta interacciones | No | Sí |
| Interfiere con web | A veces | Nunca |

---

## ✅ Checklist de Mejoras v2.1

- [x] FAB auto-ocultable implementado
- [x] Detección de scroll añadida
- [x] Detección de touch añadida
- [x] Timeout configurable
- [x] Limpieza de timeouts
- [x] Mantener visible con menú abierto
- [x] Ocultar completamente en fullscreen
- [x] usesCleartextTraffic verificado
- [x] Iconos creados/verificados
- [x] Documentación actualizada

---

## 🚀 Siguiente Versión (v2.2)

Posibles mejoras:

1. **Posición Configurable**
   - Permitir mover el FAB
   - Izquierda o derecha
   - Arriba o abajo

2. **Gestos**
   - Swipe para mostrar FAB
   - Long press para acceso rápido

3. **Mini FAB**
   - Versión más pequeña (32x32px)
   - Menos obstructiva aún

4. **Feedback Háptico**
   - Vibración al aparecer
   - Confirmar interacción

---

**🎬 StreamPay v2.1 - Menos Obstrucción, Mejor Experiencia**
