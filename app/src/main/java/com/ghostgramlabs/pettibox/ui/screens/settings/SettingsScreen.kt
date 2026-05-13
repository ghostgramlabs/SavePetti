package com.ghostgramlabs.pettibox.ui.screens.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostgramlabs.pettibox.data.preferences.LocalBackupStatus
import com.ghostgramlabs.pettibox.data.preferences.OcrPreferences
import com.ghostgramlabs.pettibox.data.preferences.ThemeMode
import com.ghostgramlabs.pettibox.ui.components.ScreenHeading
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val localBackupPath = remember { viewModel.localBackupPath() }
    val autoScanOcr by viewModel.autoScanOcr.collectAsStateWithLifecycle(initialValue = true)
    val pdfPageLimit by viewModel.pdfPageLimit.collectAsStateWithLifecycle(
        initialValue = OcrPreferences.DEFAULT_PDF_PAGES
    )
    val localBackupStatus by viewModel.localBackupStatus.collectAsStateWithLifecycle(
        initialValue = LocalBackupStatus(
            enabled = false,
            lastBackupAt = 0L,
            lastBackupName = "",
            folderUri = ""
        )
    )
    val importBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching { viewModel.importBackupUri(uri) }
                    .onSuccess { result ->
                        snackbarHostState.showSnackbar(
                            "Restored ${result.saves} saves, ${result.categories} collections, ${result.tags} tags"
                        )
                    }
                    .onFailure {
                        snackbarHostState.showSnackbar("That backup file couldn't be imported")
                    }
            }
        }
    }
    val chooseBackupFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching { viewModel.setLocalBackupFolder(uri) }
                    .onSuccess { snackbarHostState.showSnackbar("Backup folder selected") }
                    .onFailure { snackbarHostState.showSnackbar("Couldn't use that folder") }
            }
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeading(
                title = "Settings",
                subtitle = "How PettiBox looks, plus a backup whenever you need one."
            )

            Spacer(Modifier.height(8.dp))
            // Inset the rest of the page so it lines up with section gutters
            // while still letting ScreenHeading own its own padding above.
            Column(Modifier.padding(horizontal = 20.dp)) {
            SettingsSection(title = "Appearance") {
                ThemeChoice(
                    mode = ThemeMode.SYSTEM,
                    icon = Icons.Rounded.PhoneAndroid,
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
                )
                ThemeChoice(
                    mode = ThemeMode.LIGHT,
                    icon = Icons.Rounded.LightMode,
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                )
                ThemeChoice(
                    mode = ThemeMode.DARK,
                    icon = Icons.Rounded.DarkMode,
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) }
                )
            }

            Spacer(Modifier.height(16.dp))
            SettingsSection(title = "Text recognition") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.TextSnippet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Auto-scan images and PDFs for search",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Applies to new saves only. Turn it off to save files without extracting text.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoScanOcr,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                viewModel.setAutoScanOcr(enabled)
                                snackbarHostState.showSnackbar(
                                    if (enabled) "Auto-scan is on for new saves"
                                    else "Auto-scan is off for new saves"
                                )
                            }
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val count = viewModel.scanExistingSaves()
                            snackbarHostState.showSnackbar(
                                if (count == 0) "No older images or PDFs need scanning"
                                else "Scanning existing saves for search: queued $count item${if (count == 1) "" else "s"}"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Scan existing saves", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "PDF scan limit",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Higher limits make more pages searchable, but large PDFs take longer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OcrPreferences.PDF_PAGE_LIMIT_OPTIONS.forEach { limit ->
                        PageLimitChoice(
                            limit = limit,
                            selected = pdfPageLimit == limit,
                            onClick = {
                                scope.launch {
                                    viewModel.setPdfPageLimit(limit)
                                    snackbarHostState.showSnackbar("PDF scan limit set to $limit pages")
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SettingsSection(title = "Help") {
                HelpFlow()
                Spacer(Modifier.height(10.dp))
                HelpItem(
                    title = "Save from any app",
                    body = "In YouTube, Instagram, Chrome, Photos, Files, or another app, tap Share and choose PettiBox. Pick a collection and it saves instantly.",
                    icon = Icons.Rounded.Share
                )
                HelpItem(
                    title = "Add from PettiBox",
                    body = "Tap the + button on Home to add a note, paste a link, choose pictures, or pick a file.",
                    icon = Icons.Rounded.PhoneAndroid
                )
                HelpItem(
                    title = "Find it later",
                    body = "Search looks through titles, notes, tags, sources, links, and text found inside images or PDFs.",
                    icon = Icons.Rounded.Search
                )
                HelpItem(
                    title = "Keep things organized",
                    body = "Collections help group saves. You can create your own collections and edit their name, icon, and color.",
                    icon = Icons.Rounded.LightMode
                )
                HelpItem(
                    title = "Text recognition is optional",
                    body = "Auto-scan makes screenshots and PDFs searchable. Large PDFs are indexed for the first 30 pages to keep the app fast.",
                    icon = Icons.AutoMirrored.Rounded.TextSnippet
                )
                HelpItem(
                    title = "Share and remove safely",
                    body = "Open a saved item to share one attachment from a group, swipe through items, or remove an attachment with Undo.",
                    icon = Icons.Rounded.Share
                )
                HelpItem(
                    title = "Back up your shelf",
                    body = "Export creates a ZIP with backup.json and local attachment files. Nightly local backup can also copy the ZIP to a folder you choose.",
                    icon = Icons.Rounded.Download
                )
            }

            Spacer(Modifier.height(16.dp))
            SettingsSection(title = "Backup") {
                Text(
                    "Export a ZIP backup with your saves, collections, tags, and local attachment files.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Automatic local backup",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (localBackupStatus.lastBackupAt > 0L) {
                                "Nightly at about 2 AM. Last backup: ${formatBackupTime(localBackupStatus.lastBackupAt)}."
                            } else {
                                "Nightly at about 2 AM. Stored on this device only."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = localBackupStatus.enabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                viewModel.setAutoLocalBackup(enabled)
                                snackbarHostState.showSnackbar(
                                    if (enabled) "Nightly local backup is on"
                                    else "Nightly local backup is off"
                                )
                            }
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (localBackupStatus.folderUri.isBlank()) {
                        "By default, automatic backups stay in PettiBox app storage. Pick a folder to keep a copy somewhere easier to find."
                    } else {
                        "Automatic backups are copied to your selected folder. PettiBox also keeps a private safety copy on this device."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Default backup path",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        localBackupPath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(10.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { chooseBackupFolder.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (localBackupStatus.folderUri.isBlank()) "Choose backup folder"
                        else "Change backup folder",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            runCatching { viewModel.createLocalBackupNow() }
                                .onSuccess { (_, result) ->
                                    snackbarHostState.showSnackbar(
                                        "Local backup saved in PettiBox backups with ${result.saves} saves and ${result.embeddedFiles} files"
                                    )
                                }
                                .onFailure {
                                    snackbarHostState.showSnackbar("Couldn't create local backup")
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Back up now on this device", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        scope.launch {
                            runCatching { viewModel.exportBackupZipFile() }
                                .onSuccess { (file, result) ->
                                    if (!shareBackupFile(ctx, file)) {
                                        snackbarHostState.showSnackbar("Couldn't export backup")
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            "Backup includes ${result.saves} saves and ${result.embeddedFiles} files"
                                        )
                                    }
                                }
                                .onFailure {
                                    snackbarHostState.showSnackbar("Couldn't export backup")
                                }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export backup file", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { importBackup.launch(arrayOf("application/zip", "application/json", "text/*", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import backup ZIP or JSON", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun HelpFlow() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(12.dp)
    ) {
        Text(
            "The main idea",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        HelpStep(number = "1", title = "Tap Share", body = "Use the Share button in the app where you found something.")
        HelpStep(number = "2", title = "Choose PettiBox", body = "PettiBox appears in Android's share menu for links, text, images, PDFs, and files.")
        HelpStep(number = "3", title = "Pick a collection", body = "Tap a collection chip. PettiBox saves immediately.")
        HelpStep(number = "4", title = "Search later", body = "Find it by title, note, tag, source, link, or text inside images and PDFs.")
    }
}

@Composable
private fun HelpStep(
    number: String,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HelpItem(
    title: String,
    body: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PageLimitChoice(
    limit: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (selected) {
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    } else {
        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    }
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = colors
    ) {
        Text("$limit", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThemeChoice(
    mode: ThemeMode,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (selected) {
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    } else {
        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    }

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = colors
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(mode.label(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

private fun shareBackupFile(ctx: Context, file: File): Boolean {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "PettiBox backup")
        clipData = ClipData.newUri(ctx.contentResolver, file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching {
        ctx.startActivity(Intent.createChooser(intent, "Export PettiBox backup"))
    }.isSuccess
}

private fun formatBackupTime(timestamp: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
