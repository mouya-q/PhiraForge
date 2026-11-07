package dev.mouya.phiraforge.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import top.yukonga.miuix.kmp.basic.MiuixTheme
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * PhiraForge theme using Miuix KMP with HyperOS-style visual design.
 * Supports light/dark/system theme modes with blur and glass effects.
 */
@Composable
fun PhiraForgeTheme(
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) darkColorScheme() else lightColorScheme()

    MiuixTheme(
        colors = colors,
        content = content
    )
}