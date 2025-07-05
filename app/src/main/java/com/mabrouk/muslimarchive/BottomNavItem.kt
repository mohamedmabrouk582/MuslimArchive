package com.mabrouk.muslimarchive

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import drawable.Azqar

val bottomNavItems = listOf(
    RadioNavItem,
    StoryNavItem,
    QuranNavItem,
    AzkarNavItem,
    HadeNavItem
)

sealed class BottomNavItem(
    val icon: ImageVector,
    val screenRoute: String
)



data object RadioNavItem : BottomNavItem(
    Radio ,
    "RadioRoute"
)

data object StoryNavItem : BottomNavItem(
    Story ,
    "StoryRoute"
)

data object QuranNavItem : BottomNavItem(
    Quran ,
    "QuranRoute"
)

data object AzkarNavItem : BottomNavItem(
    Azqar ,
    "AzkarRoute"
)

data object HadeNavItem : BottomNavItem(
    Hadec ,
    "HadecRoute"
)





