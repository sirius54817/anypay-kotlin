package com.idk.anypay.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

// ── shadcn/ui – Dark Color Scheme ───────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = ShadcnPrimaryDark,       // zinc-50
    onPrimary            = ShadcnPrimaryFgDark,     // zinc-900
    primaryContainer     = Zinc800,
    onPrimaryContainer   = Zinc100,
    secondary            = Zinc400,
    onSecondary          = Zinc900,
    secondaryContainer   = Zinc700,
    onSecondaryContainer = Zinc200,
    tertiary             = BalanceBlue,
    onTertiary           = White,
    tertiaryContainer    = Zinc800,
    onTertiaryContainer  = Zinc100,
    background           = Zinc950,                 // near-black
    onBackground         = Zinc50,
    surface              = Zinc900,                 // card bg
    onSurface            = Zinc50,
    surfaceVariant       = Zinc800,
    onSurfaceVariant     = Zinc400,
    surfaceContainerHighest = Zinc700,
    surfaceContainerHigh    = Zinc800,
    surfaceContainer        = Zinc900,
    surfaceContainerLow     = Zinc950,
    surfaceContainerLowest  = Zinc950,
    outline              = ShadcnBorderDark,        // zinc-800
    outlineVariant       = Zinc700,
    error                = ShadcnDestructive,
    onError              = White,
    errorContainer       = ShadcnDestructive.copy(alpha = 0.15f),
    onErrorContainer     = ShadcnDestructive
)

// ── shadcn/ui – Light Color Scheme ──────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = ShadcnPrimary,           // zinc-900
    onPrimary            = ShadcnPrimaryFg,         // zinc-50
    primaryContainer     = Zinc200,
    onPrimaryContainer   = Zinc900,
    secondary            = Zinc600,
    onSecondary          = White,
    secondaryContainer   = Zinc100,
    onSecondaryContainer = Zinc700,
    tertiary             = BalanceBlue,
    onTertiary           = White,
    tertiaryContainer    = Color(0xFFEFF6FF),       // blue-50
    onTertiaryContainer  = Color(0xFF1D4ED8),       // blue-700
    background           = White,                   // pure white page
    onBackground         = Zinc900,
    surface              = White,                   // card bg
    onSurface            = Zinc900,
    surfaceVariant       = Zinc100,
    onSurfaceVariant     = Zinc500,
    surfaceContainerHighest = Zinc200,
    surfaceContainerHigh    = Zinc100,
    surfaceContainer        = Zinc50,
    surfaceContainerLow     = White,
    surfaceContainerLowest  = White,
    outline              = ShadcnBorder,            // zinc-200
    outlineVariant       = Zinc300,
    error                = ShadcnDestructive,
    onError              = White,
    errorContainer       = Color(0xFFFEF2F2),       // red-50
    onErrorContainer     = ShadcnDestructive
)

@Composable
fun AnyPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,                  // off – keep shadcn neutrals
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = Shapes,
        content     = content
    )
}