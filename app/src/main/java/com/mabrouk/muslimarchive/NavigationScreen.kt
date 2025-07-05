package com.mabrouk.muslimarchive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun NavigationScreen() {
    val navHostController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize(),
        bottomBar = {
                    AnimatedNavigationBar(
                        navHostController,
                        barColor = Color.Red,
                        circleColor = MaterialTheme.colorScheme.primary,
                        selectedColor = MaterialTheme.colorScheme.onSurface,
                        unselectedColor = MaterialTheme.colorScheme.secondary,
                    )
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            NavHost(navController = navHostController, startDestination = QuranNavItem.screenRoute){
                radioGraph(navHostController)

                storyGraph(navHostController)

                quranGraph(navHostController)

                azkarGraph(navHostController)

                hadecGraph(navHostController)
            }
        }
    }
}