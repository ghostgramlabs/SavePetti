package com.ghostgramlabs.pettibox.ui.screens.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghostgramlabs.pettibox.data.local.CategoryEntity
import com.ghostgramlabs.pettibox.data.preferences.LocalBackupStatus
import com.ghostgramlabs.pettibox.data.preferences.OcrPreferences
import com.ghostgramlabs.pettibox.data.preferences.ThemeMode
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import com.ghostgramlabs.pettibox.ui.components.CreateCollectionDialog
import com.ghostgramlabs.pettibox.ui.components.EditCollectionDialog
import com.ghostgramlabs.pettibox.ui.components.KeeperMascot
import com.ghostgramlabs.pettibox.ui.components.KeeperPose
import com.ghostgramlabs.pettibox.ui.components.ScreenHeading
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
            folderUri = "",
            lastCopyFailedAt = 0L
        )
    )
    val collections by viewModel.collections.collectAsStateWithLifecycle(initialValue = emptyList())
    var showCreateCollection by remember { mutableStateOf(false) }
    var editingCollection by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCollection by remember { mutableStateOf<CategoryEntity?>(null) }
    var showHelpDetails by remember { mutableStateOf(false) }
    var busyLabel by remember { mutableStateOf<String?>(null) }
    var hasExactAlarmPermission by remember { mutableStateOf(viewModel.hasExactAlarmPermission()) }

    editingCollection?.let { target ->
        EditCollectionDialog(
            category = target,
            onDismiss = { editingCollection = null },
            onSave = { name, emoji, colorHex ->
                editingCollection = null
                viewModel.updateCollection(target, name, emoji, colorHex) { updated ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Updated ${updated.emoji} ${updated.name}")
                    }
                }
            },
            // Delete from inside the edit dialog is the conventional
            // "settings → edit thing → trash" path. We confirm via a
            // separate AlertDialog so an accidental tap doesn't nuke a
            // collection (and its saves' category links) silently.
            onDelete = if (target.userCreated) {
                {
                    editingCollection = null
                    deletingCollection = target
                }
            } else null
        )
    }

    deletingCollection?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingCollection = null },
            title = { Text("Delete ${target.name}?") },
            text = {
                Text(
                    "Saves in this collection will stay in PettiBox — they'll just no longer belong to a collection. You can move them somewhere else from each save's detail screen."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = target.name
                    val emoji = target.emoji
                    deletingCollection = null
                    viewModel.deleteCollection(target) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Deleted $emoji $name")
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingCollection = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
    if (showCreateCollection) {
        CreateCollectionDialog(
            onCreate = { newCollection ->
                showCreateCollection = false
                // Reject case-insensitive duplicates inline rather than
                // silently writing two collections with the same name.
                val duplicate = collections.any {
                    it.name.equals(newCollection.name.trim(), ignoreCase = true)
                }
                if (duplicate) {
                    scope.launch {
                        snackbarHostState.showSnackbar("A collection named \"${newCollection.name}\" already exists")
                    }
                    return@CreateCollectionDialog
                }
                viewModel.createCollection(newCollection) { created ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Created ${created.emoji} ${created.name}"
                        )
                    }
                }
            },
            onDismiss = { showCreateCollection = false }
        )
    }
    val importBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busyLabel = "Importing backup"
                runCatching { viewModel.importBackupUri(uri) }
                    .onSuccess { result ->
                        snackbarHostState.showSnackbar(
                            "Restored ${result.saves} saves, ${result.categories} collections, ${result.tags} tags"
                        )
                    }
                    .onFailure {
                        snackbarHostState.showSnackbar("That backup file couldn't be imported")
                    }
                busyLabel = null
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
                title = "Make it yours",
                subtitle = "Appearance, collections, scans, and backups — your shelf, your rules."
            )

            Spacer(Modifier.height(8.dp))
            // Inset the rest of the page so it lines up with section gutters
            // while still letting ScreenHeading own its own padding above.
            Column(Modifier.padding(horizontal = 20.dp)) {
            if (busyLabel != null) {
                WorkInProgressBanner(label = busyLabel!!)
                Spacer(Modifier.height(12.dp))
            }
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
            SettingsSection(title = "Collections") {
                CollectionsManager(
                    collections = collections,
                    onCreateClick = { showCreateCollection = true },
                    onEditCollection = { editingCollection = it }
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
                            "Applies to new saves only. English text works best; other languages may be partial.",
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
                            busyLabel = "Queueing text scan"
                            runCatching { viewModel.scanExistingSaves() }
                                .onSuccess { count ->
                                    snackbarHostState.showSnackbar(
                                        if (count == 0) "No older images or PDFs need scanning"
                                        else "Scanning existing saves for search: queued $count item${if (count == 1) "" else "s"}"
                                    )
                                }
                                .onFailure { snackbarHostState.showSnackbar("Couldn't queue text scan") }
                            busyLabel = null
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
            SettingsSection(title = "Reminders") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Exact alarm access",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (hasExactAlarmPermission) {
                                "Reminders can fire at the time you choose."
                            } else {
                                "Reminders still fire, but Android may delay them by a few minutes."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!hasExactAlarmPermission) {
                    Spacer(Modifier.height(12.dp))
                    NoticeBanner(
                        text = "For on-the-dot reminders, allow Alarms & reminders in Android settings.",
                        action = "Open settings",
                        pose = KeeperPose.Reminder,
                        onAction = {
                            if (!viewModel.openExactAlarmSettings()) {
                                scope.launch { snackbarHostState.showSnackbar("Couldn't open alarm settings") }
                            }
                            hasExactAlarmPermission = viewModel.hasExactAlarmPermission()
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SettingsSection(title = "Help") {
                HelpFlow()
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showHelpDetails = !showHelpDetails },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (showHelpDetails) "Hide detailed guide" else "Show detailed guide",
                        fontWeight = FontWeight.Bold
                    )
                }
                if (showHelpDetails) {

                // Grouped instead of a flat 10-row list — earlier the Help
                // section just stacked HelpItem rows until they read as a
                // FAQ dump. Three buckets keep each subsection short
                // enough to scan, and let us add the new features
                // (Favorites, Archive, Tags, Settings → New collection)
                // without making the wall taller.
                Spacer(Modifier.height(14.dp))
                HelpGroupTitle("The basics")
                HelpItem(
                    title = "Save from any app",
                    body = "In YouTube, Instagram, Chrome, Photos, Files, or another app, tap Share and choose PettiBox. Pick a collection, then save.",
                    icon = Icons.Rounded.Share
                )
                HelpItem(
                    title = "Add from PettiBox",
                    body = "Tap the + button on Home to add a note, paste a link, choose pictures, or pick a file.",
                    icon = Icons.Rounded.PhoneAndroid
                )
                HelpItem(
                    title = "Find it later",
                    body = "Search looks through titles, notes, tags, sources, links, and text found inside images or PDFs. Text recognition is most reliable for English.",
                    icon = Icons.Rounded.Search
                )
                HelpItem(
                    title = "Make a new collection",
                    body = "Settings → Create a collection. Pick a name, emoji, and color — it shows up in Browse and in the share sheet.",
                    icon = Icons.Rounded.GridView
                )

                Spacer(Modifier.height(14.dp))
                HelpGroupTitle("Get more out of it")
                HelpItem(
                    title = "Long-press for quick actions",
                    body = "Press and hold any save to pin, favorite, archive, snooze a reminder, move it to a collection, or delete — without opening it.",
                    icon = Icons.Rounded.TouchApp
                )
                HelpItem(
                    title = "Love it — see all favorites",
                    body = "Tap the heart on a save (or long-press → ❤️). Open Browse → Favorites to see every save you've hearted, all in one place.",
                    icon = Icons.Rounded.Favorite
                )
                HelpItem(
                    title = "Tags cut across collections",
                    body = "A save lives in one collection, but can carry many tags. Add tags from the save sheet or the detail screen. Open Browse → Tags to see every tag with its save count.",
                    icon = Icons.Rounded.LocalOffer
                )
                HelpItem(
                    title = "Archive when you're done",
                    body = "Archived saves disappear from Home but stay searchable. Browse → Archive shows every archived save across all collections — even ones with no collection.",
                    icon = Icons.Rounded.Archive
                )
                HelpItem(
                    title = "Remind me later",
                    body = "Tap the clock on any save (or in the share sheet) to schedule a reminder. Pick Tonight, Tomorrow, Weekend, Next week, or a custom date and time. Reminders fire on the dot and survive a phone restart.",
                    icon = Icons.Rounded.AccessTime
                )
                HelpItem(
                    title = "Text recognition is optional",
                    body = "Auto-scan makes English screenshots and PDFs searchable. Other languages may be partial. Large PDFs are indexed for the first 30 pages to keep the app fast.",
                    icon = Icons.AutoMirrored.Rounded.TextSnippet
                )

                Spacer(Modifier.height(14.dp))
                HelpGroupTitle("Backups & safety")
                HelpItem(
                    title = "Share and remove safely",
                    body = "Open a saved item to share one attachment from a group, swipe through items, or remove an attachment with Undo.",
                    icon = Icons.Rounded.Share
                )
                HelpItem(
                    title = "Back up your shelf",
                    body = "Export creates a ZIP with backup.json and local attachment files. Nightly local backup keeps the 3 most recent ZIPs on this device, and can also copy each one to a folder you choose.",
                    icon = Icons.Rounded.Download
                )
                }
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
                        "By default, the 3 most recent automatic backups stay in PettiBox app storage. Pick a folder to keep a copy somewhere easier to find."
                    } else {
                        "Automatic backups are copied to your selected folder. PettiBox also keeps the 3 most recent backups as a private safety copy on this device."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (localBackupStatus.lastCopyFailedAt > 0L) {
                    Spacer(Modifier.height(10.dp))
                    NoticeBanner(
                        text = "PettiBox made a private backup, but couldn't copy it to your selected folder. Pick the folder again if it moved or permissions changed.",
                        action = "Choose folder",
                        onAction = { chooseBackupFolder.launch(null) },
                        pose = KeeperPose.BackupWarning,
                        isError = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Default backup folder (full path)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        // Already an absolute path from
                        // Context.getExternalFilesDir(...).absolutePath, but
                        // wrap explicitly + bump line height a touch so the
                        // long path stays readable when it overflows to two
                        // or three lines on narrow phones.
                        localBackupPath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        // Some OEM builds ship without a Storage Access
                        // Framework picker or block the implicit intent —
                        // OpenDocumentTree throws ActivityNotFoundException
                        // there. Surface a friendly snackbar instead of
                        // crashing or doing nothing.
                        runCatching { chooseBackupFolder.launch(null) }
                            .onFailure {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "No file picker available on this device. Backups will stay in PettiBox storage."
                                    )
                                }
                            }
                    },
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
                            busyLabel = "Backing up now"
                            runCatching { viewModel.createLocalBackupNow() }
                                .onSuccess { (_, result) ->
                                    snackbarHostState.showSnackbar(
                                        backupSummaryMessage("Local backup saved", result)
                                    )
                                }
                                .onFailure {
                                    snackbarHostState.showSnackbar("Couldn't create local backup")
                                }
                            busyLabel = null
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
                            busyLabel = "Preparing export"
                            runCatching { viewModel.exportBackupZipFile() }
                                .onSuccess { (file, result) ->
                                    if (!shareBackupFile(ctx, file)) {
                                        snackbarHostState.showSnackbar("Couldn't export backup")
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            backupSummaryMessage("Backup includes", result)
                                        )
                                    }
                                }
                                .onFailure {
                                    snackbarHostState.showSnackbar("Couldn't export backup")
                                }
                            busyLabel = null
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
                    Text("Import backup", fontWeight = FontWeight.Bold)
                }
            }
            // Outer NavGraph Scaffold already pads for the bottom nav, and
            // this screen's Scaffold uses WindowInsets(0) to avoid double
            // padding — so this trailing spacer just adds breathing room
            // below the last section. The earlier .navigationBarsPadding()
            // chained after .height() was double-counting the inset.
            Spacer(Modifier.height(20.dp))
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
private fun WorkInProgressBanner(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NoticeBanner(
    text: String,
    action: String,
    onAction: () -> Unit,
    pose: KeeperPose = KeeperPose.Error,
    isError: Boolean = false
) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        KeeperMascot(
            size = 40.dp,
            accent = color,
            pose = pose,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) {
            Text(action, color = color)
        }
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
        HelpStep(number = "3", title = "Pick a collection", body = "Tap a collection chip, add any note or reminder, then save.")
        HelpStep(number = "4", title = "Search later", body = "Find it by title, note, tag, source, link, or English text inside images and PDFs.")
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

/**
 * Subhead inside the Help section. Smaller and quieter than a section
 * title — just enough to break a long Help block into scannable groups
 * without making the page look like a TOC.
 */
@Composable
private fun HelpGroupTitle(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
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

/**
 * Compact in-Settings collection manager. Renders the live count plus a
 * preview of up to four existing collections (emoji + name) so the user
 * sees what they already have before deciding to add another, and a
 * primary "Create a collection" CTA that opens [CreateCollectionDialog].
 *
 * Editing and deleting collections still happens inside Browse (drill in,
 * use the pencil / trash actions on user-created collections) — we do
 * not duplicate that surface here.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionsManager(
    collections: List<CategoryEntity>,
    onCreateClick: () -> Unit,
    onEditCollection: (CategoryEntity) -> Unit
) {
    Text(
        if (collections.isEmpty()) {
            "Group saves into collections — \"Recipes\", \"Read later\", anything that fits."
        } else {
            "You have ${collections.size} collection${if (collections.size == 1) "" else "s"}. Tap one to rename or delete."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (collections.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        // Show ALL collections, newest first. Was capped at the first 4
        // by sortOrder, which hid every newly-created collection (new
        // ones get the highest sortOrder and end up past index 4) — the
        // exact symptom: "I just made one and it doesn't show up." Also
        // the previous Row{...} overflowed the screen edge when total
        // chip width exceeded available space; FlowRow wraps to a second
        // / third line so the entire list is visible.
        val ordered = remember(collections) {
            collections.sortedByDescending { it.createdAt }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ordered.forEach { c ->
                // Every chip is tappable — built-ins open the dialog in
                // its read-only "Built-in collection" variant; user-made
                // ones open the editable variant with a Delete option.
                // Single mental model: tap to manage.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color(c.colorHex).copy(alpha = 0.16f))
                        .clickable { onEditCollection(c) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(c.emoji, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        c.name,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Button(
        onClick = onCreateClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Create a collection", fontWeight = FontWeight.Bold)
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

private fun backupSummaryMessage(
    prefix: String,
    result: SaveRepository.BackupExportResult
): String {
    val extras = buildList {
        if (result.favorites > 0) add("${result.favorites} favorites")
        if (result.archived > 0) add("${result.archived} archived")
        if (result.tags > 0) add("${result.tags} tags")
    }
    val extraText = if (extras.isEmpty()) "" else ", " + extras.joinToString(", ")
    return "$prefix ${result.saves} saves, ${result.embeddedFiles} files$extraText"
}

private fun formatBackupTime(timestamp: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
