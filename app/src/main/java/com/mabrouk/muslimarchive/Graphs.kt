package com.mabrouk.muslimarchive

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mabrouk.quran.presentaion.ui.QuranListUi
import com.mabrouk.quran.presentaion.ui.QuranScreen

fun NavGraphBuilder.quranGraph(
    nanController: NavController
) {
    composable(route = QuranNavItem.screenRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popExitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None }) {
        QuranScreen()
    }
}


fun NavGraphBuilder.radioGraph(
    nanController: NavController
) {
    composable(route = RadioNavItem.screenRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popExitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Text(RadioNavItem.screenRoute)
        }
    }
}



fun NavGraphBuilder.storyGraph(
    nanController: NavController
) {
    composable(route = StoryNavItem.screenRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popExitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Text(StoryNavItem.screenRoute)
        }
    }
}



fun NavGraphBuilder.azkarGraph(
    nanController: NavController
) {
    composable(route = AzkarNavItem.screenRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popExitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Text(AzkarNavItem.screenRoute)
        }
    }
}


fun NavGraphBuilder.hadecGraph(
    nanController: NavController
) {
    composable(route = HadeNavItem.screenRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popExitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Text(HadeNavItem.screenRoute)
        }
    }
}