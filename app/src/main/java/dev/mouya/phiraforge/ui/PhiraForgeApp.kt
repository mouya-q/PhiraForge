package dev.mouya.phiraforge.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.mouya.phiraforge.R
import dev.mouya.phiraforge.chart.ChartGenerator
import dev.mouya.phiraforge.ui.components.GlassBottomBar
import dev.mouya.phiraforge.ui.screens.AboutScreen
import dev.mouya.phiraforge.ui.screens.GenerateScreen
import dev.mouya.phiraforge.ui.screens.PreviewScreen
import dev.mouya.phiraforge.ui.screens.SettingsScreen
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun PhiraForgeApp() {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf("generate") }

    // Shared state for generated chart
    var lastGenerationResult by remember { mutableStateOf<ChartGenerator.GenerationResult?>(null) }

    val navItems = listOf(
        NavigationItem(
            title = stringResource(R.string.nav_generate),
            isSelected = currentRoute == "generate",
            onClick = {
                currentRoute = "generate"
                navController.navigate("generate") { launchSingleTop = true }
            }
        ),
        NavigationItem(
            title = stringResource(R.string.nav_preview),
            isSelected = currentRoute == "preview",
            onClick = {
                currentRoute = "preview"
                navController.navigate("preview") { launchSingleTop = true }
            }
        ),
        NavigationItem(
            title = stringResource(R.string.nav_settings),
            isSelected = currentRoute == "settings",
            onClick = {
                currentRoute = "settings"
                navController.navigate("settings") { launchSingleTop = true }
            }
        ),
        NavigationItem(
            title = stringResource(R.string.nav_about),
            isSelected = currentRoute == "about",
            onClick = {
                currentRoute = "about"
                navController.navigate("about") { launchSingleTop = true }
            }
        )
    )

    Scaffold(
        bottomBar = {
            GlassBottomBar(items = navItems)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "generate",
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            composable("generate") {
                GenerateScreen(
                    onGenerationComplete = { result ->
                        lastGenerationResult = result
                    }
                )
            }
            composable("preview") {
                PreviewScreen(
                    generationResult = lastGenerationResult
                )
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("about") {
                AboutScreen()
            }
        }
    }
}