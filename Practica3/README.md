# Gestor de Archivos (Android · Kotlin + Jetpack Compose)

Un explorador de archivos con **Scoped Storage**, temas personalizables (IPN/ESCOM, claro/oscuro), **favoritos**, **recientes**, búsqueda y acciones básicas (crear, renombrar, eliminar). Abre archivos con **intents** del sistema.

---

## ✨ Funcionalidades

- **Navegación** por almacenamiento (Tree URI) con **breadcrumbs** y botón **Atrás**.
- **Vista lista/cuadrícula** con miniaturas para imágenes (Coil).
- **Detalles**: nombre, tipo, tamaño, fecha.
- **Búsqueda** por nombre o extensión.
- **Abrir con…** usando `Intent.ACTION_VIEW`.
- **Gestión**: crear carpetas, renombrar, eliminar.
- **Favoritos** persistentes (DataStore).
- **Recientes** persistentes y limpiables (DataStore).
- **Temas**:
  - **Guinda (IPN: #6B2E5F)** — claro/oscuro.
  - **Azul (ESCOM: #003D6D)** — claro/oscuro.
- **Compatibilidad** con Scoped Storage (Android 10+).

---

## 🗂 Estructura de directorios

```
app/
├─ src/main/
│  ├─ java/com/example/practica3/
│  │  ├─ MainActivity.kt
│  │  ├─ data/
│  │  │  ├─ FileItem.kt
│  │  │  ├─ FileRepository.kt
│  │  │  └─ SettingsDataStore.kt
│  │  ├─ ui/
│  │  │  ├─ browser/
│  │  │  │  ├─ BrowserScreen.kt
│  │  │  │  └─ BrowserViewModel.kt
│  │  │  ├─ theme/
│  │  │  │  ├─ Theme.kt
│  │  │  │  ├─ Color.kt
│  │  │  │  └─ Type.kt
│  │  │  └─ viewer/  (opcional; actualmente no se usa)
│  │  └─ utils/      (si necesitas helpers)
│  ├─ res/
│  │  ├─ layout/ (si tienes pantallas XML heredadas)
│  │  └─ values/ (colors.xml, strings.xml, themes.xml si aplica)
│  └─ AndroidManifest.xml
├─ build.gradle.kts
└─ gradle/libs.versions.toml
```

> **Nota:** El proyecto usa **Jetpack Compose**. Si estás en Kotlin 2.x, verifica que el **Compose Compiler** esté configurado correctamente (plugin + versión alineada).

---

## 🛠 Tecnologías

- **Kotlin**, **Coroutines** / Flow
- **Jetpack Compose** (Material 3)
- **Navigation Compose**
- **Coil** (miniaturas de imágenes)
- **DataStore Preferences** (favoritos, recientes, tema, raíz)
- **DocumentFile** + **ContentResolver** (Scoped Storage)

---

## 🚀 Cómo ejecutar

1. **Clona/abre** el proyecto en Android Studio (Ladybug o superior recomendado).
2. Asegúrate de tener:
   - `compileSdk` y `targetSdk` actualizados.
   - Compose habilitado y el **Compose Compiler** correcto.
3. **Sin permisos de READ/WRITE externos heredados**: se opera con **Storage Access Framework**.
4. Ejecuta la app en un emulador o dispositivo (Android 7.0+; recomendado Android 10+).

---

## 🧭 Primer uso

1. Pulsa **“Elegir/Cambiar carpeta raíz”** (FAB o en **Settings**).
2. Selecciona una carpeta con el **picker** del sistema.  
   La app tomará permiso **persistente** y recordará esta raíz.
3. Navega por las carpetas (tap) o usa **Atrás** para subir de nivel.
4. **Long press** sobre un ítem para **Renombrar** o **Eliminar**.
5. Marca/desmarca **Favoritos** con ⭐.
6. Consulta **Recientes** en su pestaña; puedes **limpiarlos** en el menú de Settings.
7. Cambia de **tema** en el menú de **Settings**.

---

## 🎨 Temas

- **Guinda (IPN):** `#6B2E5F`
- **Azul (ESCOM):** `#003D6D`
- Ambas paletas con adaptación **claro/oscuro**.  
Selecciona el tema en **Settings**; la opción activa aparece con ✔.

---

## 🔐 Scoped Storage y permisos

- No se usan permisos de almacenamiento heredados.  
- Se trabaja con **URI** de árbol (SAF) y **DocumentFile**.
- La app llama a `takePersistableUriPermission()` para conservar acceso tras reinicios.

---

## 🧩 Puntos clave de la implementación

- **BrowserViewModel**
  - Pila de directorios para **Atrás**.
  - `visibleItems` = `items` filtrados por `query`.
  - `favorites` (Set de `String` con URIs) y `favoriteItems`.
  - `recentItems` desde DataStore con nombres resueltos vía `DocumentFile`.
  - Acciones: `createFolder`, `renameItem`, `deleteItem`, `copyItem`, `moveItem`.
  - `trackOpened(uri)` para recientes.
- **BrowserScreen**
  - **Tabs**: Archivos / Favoritos / Recientes.
  - **Buscador** (nombre y extensión).
  - **Lista** o **Grid** con miniaturas (Coil).
  - **Long press** ⇒ Hoja de acciones (renombrar/eliminar).
  - **Abrir con…** siempre vía `ACTION_VIEW`.

---

## 🧪 Pruebas rápidas

- Crear/renombrar/eliminar carpeta en diferentes niveles.
- Abrir varios tipos de archivos con **Abrir con…** (imágenes, txt, pdf…).
- Marcar/desmarcar favoritos y verificar pestaña **Favoritos**.
- Abrir archivos para que queden en **Recientes**; limpiar desde menú.
- Cambiar entre **Guinda/Azul** y modo claro/oscuro del sistema.
- Alternar **Lista/Grid** y probar búsqueda con extensiones (`.jpg`, `.json`, etc.).

---

## 🐞 Problemas frecuentes

- **Miniaturas negras**: se desactiva hardware bitmap en Coil (`allowHardware(false)`).
- **No aparece “Atrás”**: sólo se habilita cuando `currentDir` ≠ `rootUri` y no estás en **Recientes**.
- **No ves archivos**: asegúrate de haber elegido una **carpeta raíz** válida (no una descarga temporal).

---

## 🗺️ Roadmap (ideas)

- Selector de **copiar/mover** con diálogo de destino.
- **Ordenamiento** (nombre, fecha, tamaño, tipo).
- **Detalles** de archivo (propiedades completas).
- Visores internos opcionales (texto, imágenes).
- **Compartir** archivo via `ACTION_SEND`.

---

## 📄 Licencia

Proyecto educativo / práctica. Úsalo y modifícalo libremente para tus necesidades académicas.

---

## 🙌 Créditos

- Jetpack Compose / Material 3.
- Coil para carga de imágenes.
- DataStore Preferences.
- Android SAF / DocumentFile.
