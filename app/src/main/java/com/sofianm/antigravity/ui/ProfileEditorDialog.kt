package com.sofianm.antigravity.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sofianm.antigravity.ProfileDialogState
import com.sofianm.antigravity.data.RenderEngine
import com.sofianm.antigravity.data.UrlValidator

@Composable
fun ProfileEditorDialog(
    state: ProfileDialogState,
    onScanQrClick: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (
        existingId: String?,
        name: String,
        normalizedUrl: String,
        engine: RenderEngine,
        setAsDefault: Boolean,
        openImmediately: Boolean
    ) -> Unit
) {
    var urlInput by remember(state) { mutableStateOf(state.initialUrl) }
    var nameInput by remember(state) { mutableStateOf(state.initialName) }
    var engine by remember(state) {
        mutableStateOf(state.editingProfile?.preferredEngine ?: RenderEngine.SAFE_CUSTOM_TAB)
    }
    var setAsDefault by remember(state) {
        mutableStateOf(state.editingProfile?.isDefault ?: true)
    }
    var urlError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    state.editingProfile != null -> "Workspace bewerken"
                    state.fromQrScan -> "QR-code herkend"
                    else -> "Workspace toevoegen"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { next ->
                            urlInput = next
                            urlError = null
                            if (nameInput.isBlank()) {
                                UrlValidator.normalizeUrl(next)?.let { valid ->
                                    nameInput = UrlValidator.suggestProfileName(valid)
                                }
                            }
                        },
                        label = { Text("Website URL") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        isError = urlError != null,
                        supportingText = urlError?.let { err -> { Text(err) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.weight(1f)
                    )

                    FilledTonalIconButton(onClick = onScanQrClick) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR-code scannen"
                        )
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Naam (bijv. Antigravity)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Weergavemodus",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                EngineOptionCard(
                    selected = engine == RenderEngine.SAFE_CUSTOM_TAB,
                    title = "Veilige Modus (Chrome Custom Tab)",
                    subtitle = "Aanbevolen voor Google Sign-In, Passkeys, Web Push & Fold Side-Sheet.",
                    icon = Icons.Default.VerifiedUser,
                    onClick = { engine = RenderEngine.SAFE_CUSTOM_TAB }
                )

                EngineOptionCard(
                    selected = engine == RenderEngine.NATIVE_WEBVIEW,
                    title = "Native WebView Modus",
                    subtitle = "Volledig scherm zonder adresbalk, in-app Dual-Pane op Fold & eigen zoom.",
                    icon = Icons.Default.Language,
                    onClick = { engine = RenderEngine.NATIVE_WEBVIEW }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = setAsDefault,
                        onCheckedChange = { setAsDefault = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Instellen als standaard workspace",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalized = UrlValidator.normalizeUrl(urlInput)
                    if (normalized == null) {
                        urlError = "Voer een geldige http(s) URL in"
                    } else {
                        onConfirm(
                            state.editingProfile?.id,
                            nameInput,
                            normalized,
                            engine,
                            setAsDefault,
                            true
                        )
                    }
                }
            ) {
                Text("Opslaan & openen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuleren")
            }
        }
    )
}

@Composable
private fun EngineOptionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
