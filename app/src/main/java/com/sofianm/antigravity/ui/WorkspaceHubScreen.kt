package com.sofianm.antigravity.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DevicesFold
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sofianm.antigravity.data.AppSettings
import com.sofianm.antigravity.data.RenderEngine
import com.sofianm.antigravity.data.WorkspaceProfile
import com.sofianm.antigravity.foldable.FoldUiState
import com.sofianm.antigravity.web.PaneSlot

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceHubScreen(
    settings: AppSettings,
    foldState: FoldUiState,
    onAddUrlClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onOpenProfile: (WorkspaceProfile, RenderEngine?, PaneSlot, Boolean?) -> Unit,
    onLaunchAdjacentSplit: (WorkspaceProfile) -> Unit,
    onEditProfile: (WorkspaceProfile) -> Unit,
    onDeleteProfile: (WorkspaceProfile) -> Unit,
    onSetDefaultProfile: (WorkspaceProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpanded = foldState.isExpandedScreen
    val columns = if (isExpanded) 2 else 1

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(
                start = if (isExpanded) 24.dp else 16.dp,
                end = if (isExpanded) 24.dp else 16.dp,
                top = 12.dp,
                bottom = 32.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header bar
            item(span = { GridItemSpan(columns) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Antigravity",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.DevicesFold else Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${foldState.postureLabelNl} • ${foldState.windowWidthDp}dp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onScanQrClick) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "QR-code scannen"
                            )
                        }
                        IconButton(onClick = onAddUrlClick) {
                            Icon(
                                imageVector = Icons.Default.AddLink,
                                contentDescription = "URL toevoegen"
                            )
                        }
                        IconButton(onClick = onOpenSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Instellingen"
                            )
                        }
                    }
                }
            }

            if (settings.profiles.isEmpty()) {
                // Onboarding welcome card
                item(span = { GridItemSpan(columns) }) {
                    OnboardingCard(
                        isExpanded = isExpanded,
                        onScanQrClick = onScanQrClick,
                        onAddUrlClick = onAddUrlClick
                    )
                }
            } else {
                // Quick action strip
                item(span = { GridItemSpan(columns) }) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onScanQrClick) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("QR-code scannen")
                        }

                        FilledTonalButton(onClick = onAddUrlClick) {
                            Icon(
                                imageVector = Icons.Default.AddLink,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("URL toevoegen")
                        }

                        if (isExpanded) {
                            settings.defaultProfile?.let { defaultProf ->
                                OutlinedButton(
                                    onClick = {
                                        onOpenProfile(
                                            defaultProf,
                                            RenderEngine.NATIVE_WEBVIEW,
                                            PaneSlot.PRIMARY,
                                            false
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Splitscreen,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Dual-Pane WebView openen")
                                }
                            }
                        }
                    }
                }

                // Saved Workspace Profile Cards
                items(settings.profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        isExpanded = isExpanded,
                        onOpenPrimary = {
                            onOpenProfile(profile, null, PaneSlot.PRIMARY, null)
                        },
                        onOpenSafeSideSheet = {
                            onOpenProfile(profile, RenderEngine.SAFE_CUSTOM_TAB, PaneSlot.PRIMARY, true)
                        },
                        onOpenSafeFullScreen = {
                            onOpenProfile(profile, RenderEngine.SAFE_CUSTOM_TAB, PaneSlot.PRIMARY, false)
                        },
                        onOpenAdjacentSplit = {
                            onLaunchAdjacentSplit(profile)
                        },
                        onOpenWebPane1 = {
                            onOpenProfile(profile, RenderEngine.NATIVE_WEBVIEW, PaneSlot.PRIMARY, false)
                        },
                        onOpenWebPane2 = {
                            onOpenProfile(profile, RenderEngine.NATIVE_WEBVIEW, PaneSlot.SECONDARY, false)
                        },
                        onEdit = { onEditProfile(profile) },
                        onSetDefault = { onSetDefaultProfile(profile) },
                        onDelete = { onDeleteProfile(profile) }
                    )
                }

                // Fold tip banner
                item(span = { GridItemSpan(columns) }) {
                    FoldTipsCard(isExpanded = isExpanded)
                }
            }
        }
    }
}

@Composable
private fun OnboardingCard(
    isExpanded: Boolean,
    onScanQrClick: () -> Unit,
    onAddUrlClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Koppel je Antigravity-omgeving",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Scan een QR-code vanaf je laptop/desktop of typ direct je URL in. " +
                    "Kies de Veilige Modus voor direct inloggen met je Google-account, of de Native WebView-modus voor Dual-Pane op je Pixel 9 Pro Fold.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onScanQrClick,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("QR-code scannen")
                    }
                    FilledTonalButton(
                        onClick = onAddUrlClick,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zelf URL invoeren")
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onScanQrClick,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("QR-code scannen")
                    }
                    FilledTonalButton(
                        onClick = onAddUrlClick,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zelf URL invoeren")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(
    profile: WorkspaceProfile,
    isExpanded: Boolean,
    onOpenPrimary: () -> Unit,
    onOpenSafeSideSheet: () -> Unit,
    onOpenSafeFullScreen: () -> Unit,
    onOpenAdjacentSplit: () -> Unit,
    onOpenWebPane1: () -> Unit,
    onOpenWebPane2: () -> Unit,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = if (profile.isDefault) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (profile.preferredEngine == RenderEngine.SAFE_CUSTOM_TAB) {
                            Icons.Default.VerifiedUser
                        } else {
                            Icons.Default.Language
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (profile.isDefault) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Standaard",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = profile.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Meer opties")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Openen in Veilige Modus (volledig)") },
                            leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenSafeFullScreen()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Openen in Native WebView (Pane 1)") },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenWebPane1()
                            }
                        )
                        if (isExpanded) {
                            DropdownMenuItem(
                                text = { Text("Openen in Dual-Pane rechts (Pane 2)") },
                                leadingIcon = { Icon(Icons.Default.Splitscreen, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenWebPane2()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Instellen als standaard") },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSetDefault()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bewerken") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Verwijderen") },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Primary action buttons
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenPrimary) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (profile.preferredEngine == RenderEngine.SAFE_CUSTOM_TAB) {
                            "Open (Veilige Modus)"
                        } else {
                            "Open (WebView)"
                        }
                    )
                }

                if (isExpanded) {
                    AssistChip(
                        onClick = onOpenSafeSideSheet,
                        label = { Text("Side-Sheet") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ViewSidebar,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    AssistChip(
                        onClick = onOpenAdjacentSplit,
                        label = { Text("Split-Screen") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Splitscreen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    AssistChip(
                        onClick = onOpenWebPane2,
                        label = { Text("Pane 2") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                } else {
                    AssistChip(
                        onClick = if (profile.preferredEngine == RenderEngine.SAFE_CUSTOM_TAB) {
                            onOpenWebPane1
                        } else {
                            onOpenSafeFullScreen
                        },
                        label = {
                            Text(
                                if (profile.preferredEngine == RenderEngine.SAFE_CUSTOM_TAB) {
                                    "WebView"
                                } else {
                                    "Veilige Modus"
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FoldTipsCard(isExpanded: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isExpanded) {
                    "Pixel 9 Pro Fold tips (binnenscherm actief)"
                } else {
                    "Tip voor telefoon & Pixel 9 Pro Fold"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isExpanded) {
                    "• Gebruik 'Side-Sheet' om Antigravity (met Google-login) naast deze Hub te openen.\n" +
                        "• Gebruik 'Split-Screen' om twee Google-sessies naast elkaar te zetten.\n" +
                        "• In de Native WebView-modus klap je naadloos open en dicht zonder te herladen."
                } else {
                    "• Klap je Fold open voor Dual-Pane, Side-Sheet en een aparte binnenscherm-zoom.\n" +
                        "• Deel een link vanuit Chrome naar 'Antigravity' om snel een nieuw profiel op te slaan."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
