package com.sofianm.antigravity.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sofianm.antigravity.data.WorkspaceProfile
import com.sofianm.antigravity.web.PaneSlot
import com.sofianm.antigravity.web.WebPaneState
import com.sofianm.antigravity.web.WebSessionHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPaneView(
    slot: PaneSlot,
    state: WebPaneState,
    profiles: List<WorkspaceProfile>,
    activeProfile: WorkspaceProfile?,
    webSessionHolder: WebSessionHolder,
    pullToRefreshEnabled: Boolean,
    showHomeButton: Boolean,
    showSettingsButton: Boolean,
    paneLabel: String? = null,
    onSelectProfile: (WorkspaceProfile) -> Unit,
    onOpenInSafeMode: (String) -> Unit,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(activeProfile?.id, activeProfile?.url) {
        if (activeProfile != null) {
            webSessionHolder.loadProfileIfChanged(slot, activeProfile.id, activeProfile.url)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Slim native browser chrome bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            tonalElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showHomeButton) {
                        IconButton(
                            onClick = onHomeClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Hub",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { webSessionHolder.goBack(slot) },
                        enabled = state.canGoBack,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Terug",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { webSessionHolder.goForward(slot) },
                        enabled = state.canGoForward,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Vooruit",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Profile & page title switcher
                    Box(modifier = Modifier.weight(1f)) {
                        TextButton(
                            onClick = { profileMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = buildString {
                                        if (!paneLabel.isNullOrBlank()) {
                                            append(paneLabel)
                                            append(" • ")
                                        }
                                        append(activeProfile?.name ?: state.pageTitle)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (state.currentUrl.isNotBlank()) {
                                    Text(
                                        text = state.currentUrl.removePrefix("https://").removePrefix("http://"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "URL wisselen",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = profileMenuExpanded,
                            onDismissRequest = { profileMenuExpanded = false }
                        ) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(profile.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                profile.url,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onSelectProfile(profile)
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { webSessionHolder.reload(slot) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Vernieuwen",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val target = state.currentUrl.ifBlank { activeProfile?.url.orEmpty() }
                            if (target.isNotBlank()) onOpenInSafeMode(target)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Openen in Veilige Modus (Chrome)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showSettingsButton) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Instellingen",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = state.isLoading && state.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }
            }
        }

        // Main WebView content area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val content: @Composable () -> Unit = {
                AndroidView(
                    factory = { ctx ->
                        val webView = webSessionHolder.getOrCreateWebView(slot, ctx)
                        val initialUrl = state.currentUrl.ifBlank { activeProfile?.url.orEmpty() }
                        if (initialUrl.isNotBlank() && webView.url.isNullOrBlank()) {
                            webSessionHolder.loadUrl(slot, initialUrl, activeProfile?.id)
                        }
                        webView
                    },
                    update = { webView ->
                        if (webView.url.isNullOrBlank() && state.currentUrl.isNotBlank()) {
                            webView.loadUrl(state.currentUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (pullToRefreshEnabled) {
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { webSessionHolder.reload(slot) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    content()
                }
            } else {
                content()
            }

            // Google OAuth safety card — never spoofs or bypasses Google security policy
            if (state.detectedGoogleOAuthUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Google-inloggen gedetecteerd",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Google staat inloggen om veiligheidsredenen niet toe in een ingebedde WebView. " +
                                    "Open deze sessie in de Veilige Modus (Chrome Custom Tab) om direct met je Google-account in te loggen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { webSessionHolder.dismissOAuthBanner(slot) }) {
                                    Text("Sluiten")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val target = activeProfile?.url ?: state.currentUrl
                                        webSessionHolder.dismissOAuthBanner(slot)
                                        if (target.isNotBlank()) onOpenInSafeMode(target)
                                    }
                                ) {
                                    Text("Open in Veilige Modus")
                                }
                            }
                        }
                    }
                }
            }

            // Offline / load error overlay
            if (state.errorMessage != null && state.detectedGoogleOAuthUrl == null) {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Pagina kon niet worden geladen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { webSessionHolder.reload(slot) }) {
                                Text("Opnieuw proberen")
                            }
                            OutlinedButton(
                                onClick = {
                                    val target = state.currentUrl.ifBlank { activeProfile?.url.orEmpty() }
                                    if (target.isNotBlank()) onOpenInSafeMode(target)
                                }
                            ) {
                                Text("Veilige Modus")
                            }
                        }
                    }
                }
            }
        }
    }
}
