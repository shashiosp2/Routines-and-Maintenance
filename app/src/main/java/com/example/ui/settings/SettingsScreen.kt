package com.example.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.StorageMode
import com.example.data.VaultFileKind
import com.example.ui.components.rememberAllFilesAccessLauncher
import com.example.ui.theme.LocalRoutinaryColors

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onRefreshPermissionStatus: () -> Unit,
    onSetStorageMode: (StorageMode) -> Unit,
    onUpdateVaultRootPath: (String) -> Unit,
    onUpdateMaintenancePath: (String) -> Unit,
    onUpdateHistoryPath: (String) -> Unit,
    onUpdateRoutinesPath: (String) -> Unit,
    onVaultFolderPicked: (Uri) -> Unit,
    onMarkdownFilePicked: (VaultFileKind, Uri) -> Unit,
    onSetOvertimeSound: (Boolean) -> Unit,
    onSetOvertimeVibrate: (Boolean) -> Unit,
    onInitializeExternalFiles: () -> Unit,
    onResetSampleVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val requestAllFilesAccess = rememberAllFilesAccessLauncher(onRefreshPermissionStatus)

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let(onVaultFolderPicked)
    }

    val tasksFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onMarkdownFilePicked(VaultFileKind.MAINTENANCE_TASKS, it) }
    }

    val historyFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onMarkdownFilePicked(VaultFileKind.MAINTENANCE_HISTORY, it) }
    }

    val routinesFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onMarkdownFilePicked(VaultFileKind.ROUTINES, it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Source Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Markdown Storage Source",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = uiState.settings.storageMode == StorageMode.INTERNAL_SAMPLE,
                            onClick = { onSetStorageMode(StorageMode.INTERNAL_SAMPLE) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.testTag("mode_sample_vault_button")
                        ) {
                            Text("Sample Vault")
                        }
                        SegmentedButton(
                            selected = uiState.settings.storageMode == StorageMode.EXTERNAL_VAULT,
                            onClick = { onSetStorageMode(StorageMode.EXTERNAL_VAULT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.testTag("mode_external_vault_button")
                        ) {
                            Text("External Vault")
                        }
                    }
                    Text(
                        text = if (uiState.settings.storageMode == StorageMode.INTERNAL_SAMPLE) {
                            "Using Routinary's built-in sample Markdown files so you can explore right away."
                        } else {
                            "Reading and writing directly to your Obsidian Markdown files on device storage."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // All Files Access Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All Files Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val semantic = LocalRoutinaryColors.current
                        Surface(
                            color = if (uiState.hasAllFilesAccess) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.hasAllFilesAccess) {
                                        Icons.Rounded.CheckCircle
                                    } else {
                                        Icons.Rounded.WarningAmber
                                    },
                                    contentDescription = null,
                                    tint = if (uiState.hasAllFilesAccess) {
                                        semantic.success
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.hasAllFilesAccess) "Granted" else "Not Granted",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "Required on Android to read and write arbitrary Obsidian vault paths directly via All Files Access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = requestAllFilesAccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_all_files_access_button")
                    ) {
                        Text(
                            if (uiState.hasAllFilesAccess) {
                                "Manage All Files Access"
                            } else {
                                "Grant All Files Access"
                            }
                        )
                    }
                }
            }

            // Vault & Markdown Paths Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Vault & File Paths",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    PathWithPickerRow(
                        label = "Vault Root Folder",
                        value = uiState.settings.vaultRootPath,
                        onValueChange = onUpdateVaultRootPath,
                        buttonLabel = "Browse",
                        buttonIcon = Icons.Rounded.FolderOpen,
                        onPickClick = { folderPickerLauncher.launch(null) },
                        inputTestTag = "vault_root_path_input",
                        buttonTestTag = "browse_vault_folder_button"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    PathWithPickerRow(
                        label = "Maintenance Tasks File",
                        value = uiState.settings.maintenanceFilePath,
                        onValueChange = onUpdateMaintenancePath,
                        buttonLabel = "Pick",
                        buttonIcon = Icons.Rounded.Description,
                        onPickClick = { tasksFilePicker.launch(arrayOf("text/*", "*/*")) },
                        inputTestTag = "maintenance_path_input",
                        buttonTestTag = "pick_maintenance_file_button"
                    )

                    PathWithPickerRow(
                        label = "Maintenance History File",
                        value = uiState.settings.historyFilePath,
                        onValueChange = onUpdateHistoryPath,
                        buttonLabel = "Pick",
                        buttonIcon = Icons.Rounded.Description,
                        onPickClick = { historyFilePicker.launch(arrayOf("text/*", "*/*")) },
                        inputTestTag = "history_path_input",
                        buttonTestTag = "pick_history_file_button"
                    )

                    PathWithPickerRow(
                        label = "Routines File",
                        value = uiState.settings.routinesFilePath,
                        onValueChange = onUpdateRoutinesPath,
                        buttonLabel = "Pick",
                        buttonIcon = Icons.Rounded.Description,
                        onPickClick = { routinesFilePicker.launch(arrayOf("text/*", "*/*")) },
                        inputTestTag = "routines_path_input",
                        buttonTestTag = "pick_routines_file_button"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onInitializeExternalFiles,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("init_external_files_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Missing Files")
                        }
                        OutlinedButton(
                            onClick = onResetSampleVault,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_sample_vault_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Sample")
                        }
                    }
                }
            }

            // Routine Overtime Alerts Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Routine Overtime Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sound Chime", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Play a gentle notification tone when a step enters overtime",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.overtimeSoundEnabled,
                            onCheckedChange = onSetOvertimeSound,
                            modifier = Modifier.testTag("overtime_sound_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Haptic Vibration", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Vibrate when a routine step exceeds its scheduled time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.overtimeVibrateEnabled,
                            onCheckedChange = onSetOvertimeVibrate,
                            modifier = Modifier.testTag("overtime_vibrate_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PathWithPickerRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    buttonLabel: String,
    buttonIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onPickClick: () -> Unit,
    inputTestTag: String,
    buttonTestTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag(inputTestTag)
        )
        OutlinedButton(
            onClick = onPickClick,
            modifier = Modifier
                .height(56.dp)
                .testTag(buttonTestTag)
        ) {
            Icon(
                imageVector = buttonIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(buttonLabel)
        }
    }
}
