package com.example.practica3.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

@Composable
fun GestorTheme(
    brand: BrandTheme,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when (brand) {
        BrandTheme.GUINDA ->
            if (useDarkTheme) DarkGuindaScheme else LightGuindaScheme
        BrandTheme.AZUL ->
            if (useDarkTheme) DarkAzulScheme else LightAzulScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),   // usa la default M3; si tienes tu Typography custom, colócala aquí
        content = content
    )
}
