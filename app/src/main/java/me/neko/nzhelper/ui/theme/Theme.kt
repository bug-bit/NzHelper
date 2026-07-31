package me.neko.nzhelper.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import me.neko.nzhelper.core.datastore.ThemeSettings

val LocalDarkMode = compositionLocalOf { false }
val LocalThemeState = compositionLocalOf<ThemeState> {
    error("ThemeState not provided")
}

class ThemeState(
    initialMode: ThemeSettings.ThemeMode,
    initialAmoledDark: Boolean,
    initialDynamicColor: Boolean
) {
    var themeMode by mutableStateOf(initialMode)
    var amoledDark by mutableStateOf(initialAmoledDark)
    var dynamicColor by mutableStateOf(initialDynamicColor)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NzHelperTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val themeState = remember {
        ThemeState(
            initialMode = ThemeSettings.getThemeMode(context),
            initialAmoledDark = ThemeSettings.isAmoledDark(context),
            initialDynamicColor = ThemeSettings.isDynamicColor(context)
        )
    }

    val darkTheme = when (themeState.themeMode) {
        ThemeSettings.ThemeMode.LIGHT -> false
        ThemeSettings.ThemeMode.DARK -> true
        ThemeSettings.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        themeState.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val resolvedColorScheme = remember(darkTheme, themeState.amoledDark, colorScheme) {
        if (darkTheme && themeState.amoledDark) {
            colorScheme.copy(
                background = AmoledBlack,
                surface = AmoledBlack,
                surfaceContainer = AmoledBlack
            )
        } else {
            colorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkMode provides darkTheme,
        LocalThemeState provides themeState,
        LocalOverscrollFactory provides null
    ) {
        MaterialExpressiveTheme(
            colorScheme = resolvedColorScheme,
            shapes = Shapes,
            motionScheme = MotionScheme.expressive(),
            typography = Typography,
            content = content
        )
    }
}