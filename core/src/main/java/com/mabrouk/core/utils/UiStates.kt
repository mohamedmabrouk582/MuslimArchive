@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.mabrouk.core.utils


import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable

@Composable
fun isTablet(windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass): Boolean {
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> false
        else -> true
    }
}

fun getClassFromMetaData(context: Context, className: String): Class<*> {
    val appInfo = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA
    )

    val bundle = appInfo.metaData

    val mainValue = bundle.getString(className) ?: ""
    return Class.forName(mainValue)
}