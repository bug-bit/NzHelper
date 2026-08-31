package me.neko.nzhelper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private data class TagColorPair(val light: Color, val dark: Color)

private val palette = mapOf(
    "rose" to TagColorPair(Color(0xFFBA1A1A), Color(0xFFFFB4AB)),
    "red" to TagColorPair(Color(0xFFC00100), Color(0xFFFFA8A0)),
    "pink" to TagColorPair(Color(0xFFB94073), Color(0xFFFFAFC8)),
    "fuchsia" to TagColorPair(Color(0xFF8E4585), Color(0xFFFFB0EF)),
    "violet" to TagColorPair(Color(0xFF6750A4), Color(0xFFCFBCFF)),
    "indigo" to TagColorPair(Color(0xFF4355B9), Color(0xFFBAC3FF)),
    "blue" to TagColorPair(Color(0xFF005AC1), Color(0xFFADC6FF)),
    "sky" to TagColorPair(Color(0xFF00639B), Color(0xFF9ACDFF)),
    "cyan" to TagColorPair(Color(0xFF006A6A), Color(0xFF7DF4F0)),
    "teal" to TagColorPair(Color(0xFF006874), Color(0xFF4FD8E8)),
    "emerald" to TagColorPair(Color(0xFF006D39), Color(0xFF7CDAA0)),
    "lime" to TagColorPair(Color(0xFF4B662A), Color(0xFFC9EE8F)),
    "amber" to TagColorPair(Color(0xFF8C5300), Color(0xFFF7C154)),
    "orange" to TagColorPair(Color(0xFF944A00), Color(0xFFFFB777)),
    "brown" to TagColorPair(Color(0xFF6E5A2D), Color(0xFFE4CE95)),
    "slate" to TagColorPair(Color(0xFF575D7E), Color(0xFFC0C5E5))
)

val names: List<String> = palette.keys.toList()

@Composable
@ReadOnlyComposable
private fun resolve(name: String): Color {
    val pair = palette[name.lowercase()]
    return if (LocalDarkMode.current) pair?.dark ?: MaterialTheme.colorScheme.primary
    else pair?.light ?: MaterialTheme.colorScheme.primary
}

object TagColors {

    val names: List<String> get() = me.neko.nzhelper.ui.theme.names

    @Composable
    fun colorFor(name: String): Color {
        val resolved = resolve(name)
        return remember(name, resolved) { resolved }
    }

    @Composable
    fun containerColor(name: String): Color = colorFor(name).copy(alpha = 0.16f)

    @Composable
    fun contentColor(name: String): Color = colorFor(name)
}
