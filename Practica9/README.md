# Juego de Gato – Wear OS

## Descripción del proyecto

Este proyecto consiste en el desarrollo de un juego de Gato (Tic-Tac-Toe) diseñado específicamente para dispositivos Wear OS (relojes inteligentes).
El juego permite a dos jugadores alternar turnos en un tablero de 3x3, mostrando de forma clara el turno actual y detectando automáticamente cuando hay un ganador o un empate.

La aplicación está optimizada para pantallas pequeñas y circulares, utilizando Jetpack Compose para Wear OS, manteniendo una interfaz simple, clara y funcional.

## Funcionalidades principales

- Tablero de juego 3x3 adaptado a pantallas de Wear OS.
- Turnos alternados entre jugador X y jugador O.
- Detección automática de victoria o empate.
- Indicador visual del turno actual.
- Botón “Nueva partida” visible solo cuando la partida finaliza.
- Interfaz optimizada para relojes circulares.
- Bajo consumo de recursos y batería.

## Requisitos del sistema

### Hardware
- Dispositivo con Wear OS o emulador de Wear OS.

### Software
- Android Studio (recomendado Flamingo o superior).
- SDK mínimo: 30
- Kotlin
- Gradle incluido en el proyecto.

## Instrucciones para ejecutar el proyecto

1. Clonar el repositorio:
   git clone https://github.com/SoyLeth/Desarrollo_de_aplicaciones_moviles_nativas.git

2. Abrir Android Studio.

3. Seleccionar “Open an existing project”.

4. Abrir la carpeta:
   Practica9

5. Esperar la sincronización de Gradle.

6. Ejecutar el proyecto en un emulador o dispositivo Wear OS.

## Uso de la aplicación

1. Al iniciar la app se muestra el tablero vacío y el turno del jugador X.
2. Los jugadores seleccionan una casilla tocando la pantalla.
3. El juego termina cuando hay un ganador o empate.
4. Al finalizar aparece el botón “Nueva partida”.
5. Al presionar el botón se reinicia el tablero.

## Tecnologías utilizadas

- Kotlin
- Jetpack Compose
- Wear OS
- Android Studio

## Autor

Proyecto desarrollado como práctica académica para la materia de Desarrollo de Aplicaciones Móviles Nativas.
