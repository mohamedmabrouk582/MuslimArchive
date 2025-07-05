package com.mabrouk.core.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.ActivityCompat

// i want create fun to handle permissions
@Composable
fun RequestMultiplePermissions(
    permissions: List<String>,
    onResult: (Boolean) -> Unit,
    dialogTitle: String = "Permission Required",
    dialogText: String = "This app needs the following permissions to function properly.",
    alwaysRequest: Boolean = false
) {
    val context = LocalContext.current

    var showDialog by rememberSaveable { mutableStateOf(false) }
    // Use remember to avoid re-creating the map on recomposition unless permissions change
    val permissionsMap = remember(permissions) { permissions.associateWith { false }.toMutableMap() }

    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Check permissions again after returning from settings
        val allGranted = permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        onResult(allGranted)
        if (alwaysRequest && !allGranted){
            showDialog = true
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Update the permissionsMap with the results
        result.forEach { (permission, isGranted) ->
            permissionsMap[permission] = isGranted
        }

        val allGranted = permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            showDialog = true
        } else {
            onResult(true)
        }
    }

    // Trigger permission request when the composable is first launched
    LaunchedEffect(Unit) {
        // Check if permissions are already granted
        val allGranted = permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!allGranted) {
            launcher.launch(permissions.toTypedArray())
        } else {
            onResult(true)
        }
    }

    if (showDialog) {
        ShowPermissionDialog(
            onDismissRequest = {
                showDialog = false
                onResult(false)
            },
            dialogTitle = dialogTitle,
            dialogText = dialogText,
            onConfirmation = {
                activityLauncher.launch(getPermissionsSettingsIntent(context))
                showDialog = false
            }
        )
    }
}

fun getPermissionsSettingsIntent(context: Context): Intent {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    intent.data = android.net.Uri.fromParts("package", context.packageName, null)
    return intent
}


@Composable
fun ShowPermissionDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector = Icons.Default.Settings,
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.background,
        iconContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        icon = {
            Icon(icon, contentDescription = "Example Icon")
        },
        title = {
            Text(text = dialogTitle, textAlign = TextAlign.Center)
        },
        text = {
            Text(text = dialogText, textAlign = TextAlign.Center)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("ok", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("cancel", textAlign = TextAlign.Center,color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}