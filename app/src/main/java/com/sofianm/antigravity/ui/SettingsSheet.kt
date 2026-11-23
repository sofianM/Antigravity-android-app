package com.sofianm.antigravity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sofianm.antigravity.data.AppSettings
import com.sofianm.antigravity.data.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDualPaneFoldChange: (Boolean) -> Unit,
    onUseSideSheetFoldChange: (Boolean) -> Unit,
    onCoverZoomChange: (Int) -> Unit,
    onExpandedZoomChange: (Int) -> Unit,
    onExpandedWideLayoutChange: (Boolean) -> Unit,
    onPullToRefreshChange: (Boolean) -> Unit,
    onAutoLaunchChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onBackgroundWatcherChange: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    onClearCache: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Instellingen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Theme selector
            SectionHeader("Weergave & Thema")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> "Systeem"
                        ThemeMode.DARK -> "Donker"
                        ThemeMode.LIGHT -> "Licht"
                    }
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(label) }
                    )
                }
            }

            HorizontalDivider()

            // Foldable settings
            SectionHeader("Pixel 9 Pro Fold & Schermen")

            SettingSwitchRow(
                title = "Dual-Pane op opengeklapt scherm",
                subtitle = "Toon twee WebView-sessies naast elkaar met scharnier-uitlijning.",
                checked = settings.dualPaneEnabledOnFold,
                onCheckedChange = onDualPaneFoldChange
            )

            SettingSwitchRow(
                title = "Side-Sheet in Veilige Modus op Fold",
                subtitle = "Open Antigravity naast de Workspace Hub op het binnenscherm.",
                checked = settings.useSideSheetOnFold,
                onCheckedChange = onUseSideSheetFoldChange
            )

            SettingSwitchRow(
                title = "Brede/tablet-layout op binnenscherm",
                subtitle = "Optimaliseert de WebView-viewport op het grote Fold-scherm.",
                checked = settings.expandedWideLayout,
                onCheckedChange = onExpandedWideLayoutChange
            )

            Column {
                Text(
                    text = "Zoom coverscherm / gewone telefoon: ${settings.coverZoomPercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = settings.coverZoomPercent.toFloat(),
                    onValueChange = { onCoverZoomChange((it / 5f).roundToInt() * 5) },
                    valueRange = 75f..150f,
                    steps = 14
                )
            }

            Column {
                Text(
                    text = "Zoom opengeklapt Fold-scherm: ${settings.expandedZoomPercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = settings.expandedZoomPercent.toFloat(),
                    onValueChange = { onExpandedZoomChange((it / 5f).roundToInt() * 5) },
                    valueRange = 75f..150f,
                    steps = 14
                )
            }

            HorizontalDivider()

            // Behavior
            SectionHeader("Bediening")

            SettingSwitchRow(
                title = "Pull-to-refresh in WebView",
                subtitle = "Veeg omlaag of gebruik de knop om een pagina te vernieuwen.",
                checked = settings.pullToRefreshEnabled,
                onCheckedChange = onPullToRefreshChange
            )

            SettingSwitchRow(
                title = "Standaard workspace direct openen",
                subtitle = "Opent je favoriete profiel meteen bij het starten van de app.",
                checked = settings.autoLaunchDefault,
                onCheckedChange = onAutoLaunchChange
            )

            HorizontalDivider()

            // Notifications
            SectionHeader("Meldingen")

            SettingSwitchRow(
                title = "Android-meldingen inschakelen",
                subtitle = "Stuurt web-notificaties en activiteit door als Android-melding.",
                checked = settings.notificationsEnabled,
                onCheckedChange = onNotificationsChange
            )

            SettingSwitchRow(
                title = "Achtergrond-bewaking (Foreground Service)",
                subtitle = "Houdt actieve WebView-sessies langer bereikbaar voor meldingen.",
                checked = settings.backgroundWatcherEnabled,
                onCheckedChange = onBackgroundWatcherChange
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSendTestNotification,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Testmelding")
                }
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cache wissen")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
