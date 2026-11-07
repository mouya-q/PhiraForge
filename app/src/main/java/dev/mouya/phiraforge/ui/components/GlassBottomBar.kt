package dev.mouya.phiraforge.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem

/**
 * Glass-effect bottom navigation bar with blur background.
 */
@Composable
fun GlassBottomBar(
    items: List<NavigationItem>,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        items = items,
        modifier = modifier
    )
}