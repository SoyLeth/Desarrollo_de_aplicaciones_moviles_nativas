# Memorama (Actividad 4) — Android (Jetpack Compose)

> **Nota:** Las capturas/GIFs las agrego con los espacios indicados abajo.

## Descripción del juego

**Memorama** es un juego de cartas por parejas. El objetivo es encontrar todas las parejas volteando cartas de dos en dos.  
Este proyecto incluye **tres modos**:

- **Juego local (PvP):** 2 jugadores en el mismo dispositivo.
- **Jugador contra IA:** juegas contra una CPU que comete errores configurables.
- **Juego por Bluetooth:** 2 dispositivos conectados por Bluetooth (Host/Cliente) para jugar en red.

El tablero puede configurarse en distintos tamaños:
- **4×4** (8 parejas)
- **4×5** (10 parejas)
- **4×6** (12 parejas)

Las cartas usan imágenes (drawables) como “frente” y una imagen para el “dorso”.

---

## Reglas del juego

1. El tablero inicia con todas las cartas **boca abajo** y **mezcladas**.
2. En cada turno, el jugador activa voltea **dos cartas**:
   - Si las cartas son **la misma pareja**, se quedan visibles (marcadas como **match**) y el jugador **gana 1 punto**.
   - Si las cartas **no coinciden**, se vuelven a voltear y el turno pasa al otro jugador (según la configuración de turnos).
3. El juego termina cuando **todas las parejas** han sido encontradas.
4. Gana quien tenga **más puntos**:
   - Si empatan, el resultado es **empate**.

### Modo IA (CPU)
- La CPU juega cuando es su turno.
- Tiene un **índice de error** configurable (porcentaje) que determina qué tanto “se equivoca” (por ejemplo, ignorar una pareja conocida).

### Modo Bluetooth (Host/Cliente)
- Un jugador crea sala (**Host**) y el otro se conecta (**Cliente**).
- Se vota el tamaño de tablero; el Host decide en caso de empate según reglas del flujo.
- Los movimientos se sincronizan entre dispositivos durante la partida.

---

## Instrucciones para ejecutar el proyecto (paso a paso)

### 1) Requisitos previos
- **Android Studio** instalado (recomendado: versión estable más reciente).
- **JDK** instalado (Android Studio normalmente lo incluye).
- Conexión a internet solo para descargar dependencias la primera vez.

### 2) Clonar el repositorio
```bash
git clone <URL_DEL_REPOSITORIO>
cd <NOMBRE_DEL_REPO>
```

### 3) Abrir en Android Studio
1. Abrir **Android Studio**.
2. Seleccionar **Open** y elegir la carpeta del proyecto.
3. Esperar a que Gradle termine de sincronizar dependencias (**Gradle Sync**).

### 4) Ejecutar
1. Conectar un dispositivo Android por USB con **depuración USB** habilitada **o** iniciar un emulador.
2. Presionar **Run ▶** en Android Studio.
3. Seleccionar el dispositivo objetivo.

---

## Requisitos del sistema

### Dispositivo Android
- **Versión mínima de Android:** *(coloca aquí tu minSdk real, por ejemplo Android 8.0 / API 26 o el que tenga tu proyecto)*  
  - Recomendación: revisa `app/build.gradle(.kts)` → `defaultConfig { minSdk = ... }`.

### Dependencias / tecnologías principales
- **Kotlin**
- **Jetpack Compose** (UI)
- **Material 3** (componentes UI)
- **Coroutines** (tareas asíncronas / delays de turnos)
- Bluetooth:
  - Permisos típicos (Android 12+): `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`  
  - En versiones anteriores: `BLUETOOTH`, `BLUETOOTH_ADMIN` (según configuración)

> Si el proyecto usa versiones específicas de Compose / Kotlin / Gradle, anótalas aquí desde el `build.gradle(.kts)`.

---

## Cómo jugar

1. Abre la app y elige un modo:
   - **Juego local**
   - **Jugador contra IA**
   - **Juego por Bluetooth**
2. Configura nombres, colores y tamaño del tablero (según el modo).
3. Durante el juego:
   - Toca una carta para voltearla.
   - Toca una segunda carta para intentar formar una pareja.
4. Repite hasta que se completen todas las parejas.

---

## Guardado y carga de partidas (XML)

- El juego puede **guardar** una partida en formato **XML** (según la implementación del proyecto).
- La pantalla de **Cargar partida** permite seleccionar un archivo `partida_*.xml` para reproducirla/cargarla (según el flujo del proyecto).

> Si tu implementación guarda en una carpeta específica, documenta aquí la ruta exacta (por ejemplo, `Android/data/<paquete>/files/Download`).

---

