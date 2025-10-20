package com.example.practica3.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

enum class BrandTheme { GUINDA, AZUL }

/** Permite inyectar el tema actual (Guinda/Azul) desde la Activity */
val LocalBrandTheme = staticCompositionLocalOf { BrandTheme.GUINDA }
