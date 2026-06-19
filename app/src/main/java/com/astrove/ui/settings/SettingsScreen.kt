package com.astrove.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrove.data.prefs.ThemeMode
import com.astrove.ui.troveViewModel
import com.astrove.ui.util.formatCount

@Composable
fun SettingsRoute() {
    val vm = troveViewModel { SettingsViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    SettingsScreen(state, vm::setChargingOnly, vm::setThemeMode)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    onToggleCharging: (Boolean) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp))
            Spacer(Modifier.height(10.dp))
            val options = listOf(
                ThemeMode.SYSTEM to "System",
                ThemeMode.LIGHT to "Light",
                ThemeMode.DARK to "Dark",
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = state.themeMode == mode,
                        onClick = { onThemeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.onSurface,
                            activeContentColor = MaterialTheme.colorScheme.surface,
                            activeBorderColor = MaterialTheme.colorScheme.onSurface,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Indexing", style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp))
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatCount(state.read)} of ${formatCount(state.total)} screenshots read" +
                    if (state.pending > 0) " · ${formatCount(state.pending)} left" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Only index while charging", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Saves battery by reading screenshots only while you're plugged in. " +
                            "Otherwise Trove reads them while the app is open.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(checked = state.chargingOnly, onCheckedChange = onToggleCharging)
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "Trove works fully offline. Your screenshots and their text never leave your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
