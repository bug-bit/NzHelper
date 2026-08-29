package me.neko.nzhelper.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.neko.nzhelper.core.datastore.ThemeSettings
import me.neko.nzhelper.core.util.BackgroundImageManager
import java.io.File

val LocalDarkMode = compositionLocalOf { false }
val LocalThemeState = compositionLocalOf<ThemeState> {
    error("ThemeState not provided")
}

class ThemeState(
    initialMode: ThemeSettings.ThemeMode,
    initialAmoledDark: Boolean,
    initialDynamicColor: Boolean,
    initialThemeColorIndex: Int,
    initialBackgroundImagePath: String?,
    initialBackgroundOpacity: Float,
    initialBackgroundBlur: Float,
    initialCardOpacity: Float,
    initialDialogOpacity: Float
) {
    var themeMode by mutableStateOf(initialMode)
    var amoledDark by mutableStateOf(initialAmoledDark)
    var dynamicColor by mutableStateOf(initialDynamicColor)
    var themeColorIndex by mutableStateOf(initialThemeColorIndex)
    var backgroundImagePath by mutableStateOf(initialBackgroundImagePath)
    var backgroundOpacity by mutableStateOf(initialBackgroundOpacity)
    var backgroundBlur by mutableStateOf(initialBackgroundBlur)
    var cardOpacity by mutableStateOf(initialCardOpacity)
    var dialogOpacity by mutableStateOf(initialDialogOpacity)
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
            initialDynamicColor = ThemeSettings.isDynamicColor(context),
            initialThemeColorIndex = ThemeSettings.getThemeColorIndex(context),
            initialBackgroundImagePath = ThemeSettings.getBackgroundImagePath(context),
            initialBackgroundOpacity = ThemeSettings.getBackgroundOpacity(context),
            initialBackgroundBlur = ThemeSettings.getBackgroundBlur(context),
            initialCardOpacity = ThemeSettings.getCardOpacity(context),
            initialDialogOpacity = ThemeSettings.getDialogOpacity(context)
        )
    }

    // 背景图片文件被外部删除时自动清理设置
    LaunchedEffect(themeState.backgroundImagePath) {
        val path = themeState.backgroundImagePath
        if (path != null && !File(path).exists()) {
            themeState.backgroundImagePath = null
            ThemeSettings.setBackgroundImagePath(context, null)
        }
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

        else -> {
            val option = ThemeColorOptions.getOrElse(themeState.themeColorIndex) {
                ThemeColorOptions.first()
            }
            if (darkTheme) option.darkScheme else option.lightScheme
        }
    }

    val hasBackgroundImage = themeState.backgroundImagePath != null

    val resolvedColorScheme = remember(
        darkTheme,
        themeState.amoledDark,
        hasBackgroundImage,
        themeState.cardOpacity,
        themeState.dialogOpacity,
        colorScheme
    ) {
        val base = if (darkTheme && themeState.amoledDark) {
            colorScheme.copy(
                background = AmoledBlack,
                surface = AmoledBlack,
                surfaceContainer = AmoledBlack
            )
        } else {
            colorScheme
        }
        if (hasBackgroundImage) {
            base.copy(
                surfaceContainer = Color.Transparent,
                surfaceBright = base.surfaceBright.copy(alpha = themeState.cardOpacity),
                surfaceContainerHigh = base.surfaceContainerHigh.copy(alpha = themeState.dialogOpacity),
                surfaceContainerHighest = base.surfaceContainerHighest.copy(alpha = themeState.dialogOpacity)
            )
        } else {
            base
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
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(resolvedColorScheme.background)
            ) {
                val bgPath = themeState.backgroundImagePath
                if (bgPath != null) {
                    val bgBitmap by produceState<ImageBitmap?>(initialValue = null, bgPath) {
                        value = withContext(Dispatchers.IO) {
                            BackgroundImageManager.loadImageBitmap(bgPath)?.asImageBitmap()
                        }
                    }
                    bgBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.1f)
                                .blur(themeState.backgroundBlur.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                resolvedColorScheme.background.copy(
                                    alpha = (1f - themeState.backgroundOpacity).coerceIn(0f, 1f)
                                )
                            )
                    )
                }
                content()
            }
        }
    }
}