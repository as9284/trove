package com.astrove.ui.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.astrove.ui.components.TroveMark

/** Shows [content] only with full media access; otherwise the request/remediation screen. */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(MediaAccess.current(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state = MediaAccess.current(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { state = MediaAccess.current(context) }

    if (state == AccessState.FULL) {
        content()
    } else {
        PermissionScreen(
            state = state,
            onRequest = { launcher.launch(MediaAccess.requiredPermissions()) },
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )
    }
}

@Composable
private fun PermissionScreen(
    state: AccessState,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val partial = state == AccessState.PARTIAL
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TroveMark(size = 56.dp)
            Spacer(28.dp)
            Text(
                text = if (partial) "Trove needs all your screenshots" else "Find any screenshot",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(12.dp)
            Text(
                text = if (partial) {
                    "You've shared only a few photos. Trove reads your whole screenshot " +
                        "library right on your phone so search can find any of them. " +
                        "Nothing ever leaves your device."
                } else {
                    "Trove reads the text inside your screenshots so you can search them. " +
                        "It works fully offline, with no account and no network."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(32.dp)
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text(if (partial) "Allow all screenshots" else "Allow access")
            }
            if (partial) {
                Spacer(8.dp)
                TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Open settings")
                }
            }
        }
    }
}

@Composable
private fun Spacer(size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(Modifier.height(size))
}
