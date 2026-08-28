# Seguimiento de mejoras de funcionalidad — Ayuno

Documento de seguimiento vivo. Se actualiza tras completar cada mejora.
Origen: propuesta de análisis del 2026-08-28 (ver contexto en la conversación
con Claude Code).

Leyenda: ⬜ pendiente · 🔄 en curso · ✅ hecho · ⏸️ pausado a propósito

## ⚙️ Infraestructura de build — a tener en cuenta antes de tocar Gradle

Estado tras el arreglo del 2026-08-28. **No volver al stack experimental.**

- **Stack fijado:** AGP 8.13.0 · Gradle 8.14.3 · Kotlin 2.1.20 · Java 17.
  Estas versiones van juntas; si se sube una, verificar la matriz de
  compatibilidad AGP↔Gradle↔Kotlin antes de commitear.
- **NO subir a AGP 9.x** por ahora: AGP 9 integra el soporte de Kotlin en el
  propio plugin y entra en conflicto con `org.jetbrains.kotlin.android`
  (`Cannot add extension with name 'kotlin'`), además de que el soporte del
  IDE aún es inmaduro (síntoma: sync roto → "todo el código en rojo").
- **`gradle.properties` debe quedarse mínimo** (solo `org.gradle.jvmargs`,
  `android.useAndroidX`, `kotlin.code.style`, `android.nonTransitiveRClass`).
  No re-añadir flags como `android.builtInKotlin`, `android.newDsl`,
  `android.enableAppCompileTimeRClass`, `android.r8.*`, etc.: rompen el build
  con AGP 8.x (`compileDebugJavaWithJavac ... has no value available`).
- **No versionar** `gradle/gradle-daemon-jvm.properties` (feature incubando).
- **API 36:** `compileSdk = 36` + `compileSdkMinor = 1` en
  `app/build.gradle.kts`. El `compileSdkMinor` es un **workaround temporal**:
  la plataforma local `platforms/android-36` está incompleta (faltan
  `android.jar` y `core-for-system-modules.jar`, probablemente cuarentena del
  antivirus corporativo); solo `android-36.1` está íntegra. Cuando se
  reinstale "Android SDK Platform 36" desde el SDK Manager (y se excluya la
  carpeta del SDK en el antivirus), se puede borrar `compileSdkMinor`.
- **Java 17** es obligatorio (Kotlin 2.x + AGP 8.13). El Gradle JDK de Android
  Studio debe apuntar a un JDK 17+ (el JBR incluido, "jbr-21", sirve).
- `kotlinOptions { }` está migrado a `kotlin { compilerOptions { } }`; usar
  esa DSL para cualquier opción nueva del compilador.
- Dependencias en el version catalog (`gradle/libs.versions.toml`), no como
  strings sueltos en `dependencies { }`.

## Funcionalidades core de ayuno

- [x] ✅ 1. Planes de ayuno con nombre y metodología (16:8, 18:6, 20:4, OMAD,
      5:2, ADF), con descripción, dificultad y horario sugerido.
      → `data/FastingPlan.kt` (nuevo), `PlanSelectorSection`/`PlanCard` en
      `Components.kt`, `FastingSession.planId` en `FastingStorage.kt`.
      Ajuste 2026-08-28 (a): la pantalla principal vuelve a su diseño
      original (sin el selector de planes en línea); ahora muestra una
      tarjeta compacta (`PlanEntryCard`) que abre una pantalla dedicada
      "Planes de ayuno" (`PlanPickerScreen`), igual que "Guía del ayuno" o
      "Registro de peso".
      Ajuste 2026-08-28 (b): al elegir un plan en `PlanPickerScreen` el
      ayuno se inicia al instante (ya no hay estado de "plan seleccionado"
      previo, `PlanCard` deja de tener estado `selected`). El botón
      genérico de inicio pasa a llamarse "Comenzar ayuno libre" y arranca
      un ayuno sin plan asociado (`planId = null`,
      `FREE_FASTING_GOAL_HOURS = 24` en `data/FastingPlan.kt`); el diálogo
      "Ya llevo horas en ayuno" también usa este objetivo libre.
- [x] ✅ 2. Registro de peso integrado (entrada manual + gráfica de evolución).
      → `data/WeightStorage.kt` (nuevo), `WeightScreen`/`WeightChart` en
      `Components.kt`, accesible desde el icono de báscula en la TopAppBar.
- [x] ✅ 3. Registro de hidratación (agua/café/té) durante el ayuno.
      → `data/HydrationStorage.kt` (nuevo), `HydrationSection` en
      `Components.kt`, visible durante el ayuno activo (+250 ml por toque).
- [x] ✅ 4. Historial mejorado: sin cap de 30, vista de calendario mensual,
      filtro por plan/duración.
      → `FastingHistorySection` en `Components.kt`: quitado el `.take(30)`,
      añadido toggle Lista/Calendario (`HistoryCalendar`) y `FilterChip`s
      por plan y por duración mínima.
- [x] ✅ 5. Gráficas de progreso (duración por semana/mes, tendencia de racha).
      → `ProgressChartSection`/`DurationBarChart` en `Components.kt`, gráfico
      de barras con la duración de los últimos 14 ayunos completados, debajo
      de `StatsRow` en la pantalla principal.
- [ ] ⏸️ 6. Migrar historial a Room. **Pausado a propósito** — se deja
      pendiente por ahora, no se aborda en esta ronda.

## Motivación y retención

- [x] ✅ 1. Widget de pantalla de inicio.
      → `widget/FastingWidgetProvider.kt` (AppWidgetProvider clásico con
      RemoteViews, ya que Glance no estaba entre las dependencias),
      `res/layout/widget_fasting.xml`, `res/xml/fasting_widget_info.xml`.
      Muestra fase actual, cuenta atrás y plan; toca para abrir la app.
      Se refresca al iniciar/terminar/cancelar un ayuno y cada minuto
      mientras hay un ayuno activo vía `widget/WidgetTickWorker.kt`
      (WorkManager con reencolado propio, ya que los periodos periódicos
      de WorkManager no bajan de 15 min). Registrado en
      `AndroidManifest.xml`.
- [ ] ⬜ 2. Recordatorios personalizables (inicio/fin de ayuno, pesarse).
- [x] ✅ 3. Sistema de logros/badges.
      → `data/Achievement.kt` (nuevo): 10 logros calculados en vivo a
      partir del historial y la racha (sin storage propio). Pantalla
      `AchievementsScreen`/`AchievementRow` en `Components.kt`, accesible
      desde un botón de texto "Logros" junto a "Guía del ayuno" (misma
      fila, sin añadir iconos nuevos a la barra superior).
- [x] ✅ 4. Calendario de racha visual (estilo GitHub/Duolingo).
      → `StreakStrip` en `Components.kt`: franja compacta de los últimos
      14 días (cuadrados verdes = día con ayuno completado), colocada
      justo debajo de `StatsRow` en la pantalla principal sin añadir
      botones ni pantallas nuevas. El calendario mensual detallado ya
      existente en el historial (punto 4 del bloque "core") complementa
      esta vista rápida.
- [x] ✅ 5. Corregir el permiso de notificaciones (se pide en cada arranque).
      → `MainActivity.kt` usa ahora `registerForActivityResult` +
      `ContextCompat.checkSelfPermission`, y solo pide el permiso una vez
      por instalación (`NotificationHelper.hasAskedForPermission`/
      `markPermissionAsked` en `data/NotificationHelper.kt`), en vez de
      en cada arranque.
- [ ] ⬜ 6. Onboarding inicial (2-3 pantallas).
- [ ] ⬜ 7. Frases motivacionales contextuales por fase.

## Historial de cambios

- 2026-08-28: Documento creado. Se acuerda empezar por el bloque "core de
  ayuno" excepto el punto 6 (Room), que queda pendiente/pausado.
- 2026-08-28: Implementados los puntos 1-5 del bloque "core de ayuno"
  (planes con metodología, peso, hidratación, historial con filtros/
  calendario, gráfica de progreso). Verificado con `assembleDebug` sin
  errores. Punto 6 (Room) queda pendiente, sin abordar.
- 2026-08-28: A petición del usuario, se restaura el diseño original de la
  pantalla principal. El selector de planes deja de mostrarse en línea y
  pasa a una pantalla dedicada "Planes de ayuno", accesible desde una
  tarjeta compacta en el inicio. Verificado con `assembleDebug`.
- 2026-08-28: A petición del usuario, elegir un plan en "Planes de ayuno"
  arranca el ayuno de inmediato (sin volver a la pantalla de inicio a
  confirmar). El botón "Comenzar ayuno" pasa a llamarse "Comenzar ayuno
  libre" y ya no depende de ningún plan preseleccionado. Verificado con
  `assembleDebug`.
- 2026-08-28: Arreglo de infraestructura de build. El proyecto estaba en un
  stack experimental inestable (AGP 9.0.1 + Gradle 9.2.1 + ~10 flags
  experimentales en `gradle.properties` + `gradle-daemon-jvm.properties`),
  lo que rompía el sync de Gradle en Android Studio ("todo el código en
  rojo") y un build limpio. Además, la plataforma `platforms/android-36` del
  SDK está incompleta (faltan `android.jar` y `core-for-system-modules.jar`;
  probablemente cuarentena del antivirus corporativo). Cambios:
  AGP 8.13.0, Gradle 8.14.3, Kotlin 2.1.20, Java 17; `compileSdk = 36` con
  `compileSdkMinor = 1` (usa la plataforma 36.1, que sí está íntegra);
  `gradle.properties` reducido a lo estándar; eliminado
  `gradle-daemon-jvm.properties`; `kotlinOptions` migrado a `compilerOptions`;
  `work-runtime-ktx` movido al version catalog. Verificado con
  `assembleDebug` (BUILD SUCCESSFUL). Pendiente por parte del usuario:
  reinstalar "Android SDK Platform 36" desde el SDK Manager (y excluir la
  carpeta del SDK en el antivirus) para poder quitar `compileSdkMinor`.
- 2026-08-28: Implementados los puntos 1, 3, 4 y 5 del bloque "motivación y
  retención" (widget, logros, racha visual, fix del permiso de
  notificaciones), cuidando que la pantalla principal no se sobrecargue de
  iconos: los accesos nuevos son un botón de texto ("Logros", junto a "Guía
  del ayuno") y una franja compacta de racha, sin tocar la barra superior.
  Verificado con `assembleDebug` sin errores. Pendientes: recordatorios
  personalizables (2), onboarding (6) y frases contextuales por fase (7).
