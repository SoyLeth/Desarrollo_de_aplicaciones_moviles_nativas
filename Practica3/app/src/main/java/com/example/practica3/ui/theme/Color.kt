package com.example.practica3.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =====================
//  GUINDA (IPN #6B2E5F)
// =====================
private val GuindaPrimary = Color(0xFF6B2E5F)
private val GuindaPrimaryContainerLight = Color(0xFFF2D7EC)
private val GuindaPrimaryContainerDark  = Color(0xFF4B1741)

val LightGuindaScheme = lightColorScheme(
    primary = GuindaPrimary,
    onPrimary = Color.White,
    primaryContainer = GuindaPrimaryContainerLight,
    onPrimaryContainer = Color(0xFF2B0C27),

    secondary = Color(0xFF8B3D74),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6D9EA),
    onSecondaryContainer = Color(0xFF310B28),

    tertiary = Color(0xFFB03A5B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E0),
    onTertiaryContainer = Color(0xFF3F0014),

    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF7D757F),
    outlineVariant = Color(0xFFCAC4D0)
)

val DarkGuindaScheme = darkColorScheme(
    primary = GuindaPrimary,
    onPrimary = Color.White,
    primaryContainer = GuindaPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFEFCDED),

    secondary = Color(0xFFDB9AC7),
    onSecondary = Color(0xFF3B0F2E),
    secondaryContainer = Color(0xFF5A2347),
    onSecondaryContainer = Color(0xFFF6D9EA),

    tertiary = Color(0xFFFFB1C0),
    onTertiary = Color(0xFF4A0A1E),
    tertiaryContainer = Color(0xFF6A2437),
    onTertiaryContainer = Color(0xFFFFD9E0),

    background = Color(0xFF101114),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF141316),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454E)
)

// =======================
//  AZUL (ESCOM #003D6D)
// =======================
private val AzulPrimary = Color(0xFF003D6D)
private val AzulPrimaryContainerLight = Color(0xFFCFE5FF)
private val AzulPrimaryContainerDark  = Color(0xFF0D2A44)

val LightAzulScheme = lightColorScheme(
    primary = AzulPrimary,
    onPrimary = Color.White,
    primaryContainer = AzulPrimaryContainerLight,
    onPrimaryContainer = Color(0xFF001D36),

    secondary = Color(0xFF2C6AA6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E7FF),
    onSecondaryContainer = Color(0xFF07233D),

    tertiary = Color(0xFF007A8C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBDEFFF),
    onTertiaryContainer = Color(0xFF00292F),

    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6CF)
)

val DarkAzulScheme = darkColorScheme(
    primary = AzulPrimary,
    onPrimary = Color.White,
    primaryContainer = AzulPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFCFE5FF),

    secondary = Color(0xFFA6CCF2),
    onSecondary = Color(0xFF0E2B49),
    secondaryContainer = Color(0xFF234B74),
    onSecondaryContainer = Color(0xFFD5E7FF),

    tertiary = Color(0xFF87D7E6),
    onTertiary = Color(0xFF00353D),
    tertiaryContainer = Color(0xFF004E59),
    onTertiaryContainer = Color(0xFFBDEFFF),

    background = Color(0xFF101114),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF141316),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF454751),
    onSurfaceVariant = Color(0xFFC4C6CF),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF454751)
)
