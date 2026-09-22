# Requisitos de calidad de Google Play (2027)

> Fuente: post oficial de Google Play "Elevating app quality: Reducing memory usage and improving device migration" (Raghavendra Hareesh Pottamsetty, GM Google Play Developer & Monetization).

Google Play introduce dos nuevos requisitos de calidad que afectarán la visibilidad y capacidad de publicación de Ayuno si no se cumplen.

## 1. Reducción de uso de memoria y optimización de código

**Fecha límite de cumplimiento: febrero de 2027.**

Se establecen umbrales de "mal comportamiento" (bad behavior thresholds) en tres áreas:

- **Uso de memoria dinámica (Anonymous RSS + Swap):** memoria privada de la app, activa y comprimida (no incluye archivos en disco como código o assets). Se evalúa por estado de la app (foreground/background) y por categoría de rendimiento del dispositivo (RAM bucket).
- **Uso de memoria de bitmaps:** los bitmaps pueden ocupar memoria en foreground, pero no deben permanecer en memoria mucho tiempo en estados no visibles (background, cached).
- **Optimización de código DEX:** el bundle publicado debe tener un **mínimo de 25% de cobertura** combinada entre optimización, shrinking y ofuscación (R8 u otra herramienta de shrinking equivalente).

Si no se cumplen los umbrales: **reducción de visibilidad y capacidades de publicación** en Play Store.

### Buenas prácticas a aplicar en Ayuno

- **Habilitar R8 en modo release** con `isMinifyEnabled = true` y `isShrinkResources = true` en `app/build.gradle.kts` (verificar que ya esté activo; revisar reglas en `proguard-rules.pro` para no romper nada con reflection/serialización).
- **Evitar retener Bitmaps** en memoria cuando la app pasa a background: liberar referencias a `Bitmap` grandes, usar `Bitmap.recycle()` cuando aplique, o delegar la carga de imágenes a una librería (Coil/Glide) que gestione caché y liberación automáticamente en vez de mantener bitmaps propios en memoria.
- **Revisar tamaño y densidad de bitmaps cargados**: no cargar imágenes a resolución mayor que la necesaria en pantalla (usar `inSampleSize`, `Coil.size()`, etc.).
- **Evitar memory leaks** de `Activity`/`Context`/`ViewModel` (listeners, coroutines sin cancelar, singletons reteniendo contexto).
- **Medir con Android Vitals** en Play Console (nuevas métricas de memoria dinámica y bitmaps) antes de cada subida grande.
- **Revisar crashes "out of memory"** con el nuevo filtro de Android Vitals tras cada release.
- **Comprobar el DEX optimization insight** que Play Console genera en cada subida de bundle, para verificar que se mantiene por encima del 25% de cobertura de shrinking/obfuscation.

## 2. Migración de dispositivo segura y sin fricción (Zero-Tap Sign-In)

**Fecha límite de cumplimiento: abril de 2027.**

Cualquier app con inicio de sesión (opcional u obligatorio) deberá implementar la **Android Restore Credentials API** para que, al abrir la app por primera vez en un dispositivo nuevo, el usuario quede reconocido y logueado automáticamente, sin pasos adicionales.

- Los juegos están exentos por ahora (pero se recomienda igualmente para apps de un solo tipo de cuenta).

### Aplicabilidad en Ayuno

- **Estado actual: Ayuno no tiene sistema de login/cuentas de usuario**, por lo que este requisito no aplica mientras no se añada autenticación.
- ⚠️ **Si en el futuro se añade login** (cuenta de usuario, sincronización en la nube, etc.), debe implementarse la Restore Credentials API desde el diseño inicial para cumplir este requisito antes de abril de 2027.

## Checklist antes de subir una versión a Play Store

- [ ] R8/minify y shrinkResources activos en el build de release.
- [ ] Sin bitmaps grandes retenidos innecesariamente en memoria (background/cached).
- [ ] Sin memory leaks conocidos (revisar LeakCanary si está integrado, o Profiler de Android Studio).
- [ ] versionCode/versionName incrementados (ver memoria del proyecto sobre esto).
- [ ] Revisar Android Vitals tras la subida anterior por si hay regresiones de memoria o crashes OOM.

## Recordatorio de proceso

📌 **Este documento debe revisarse al comenzar cualquier tarea sobre la app Ayuno**, para tener en cuenta estas buenas prácticas de memoria/optimización (y el requisito de login si aplica) al proponer o implementar cambios.
