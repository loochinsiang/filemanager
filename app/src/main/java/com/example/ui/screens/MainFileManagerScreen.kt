package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import android.os.StatFs

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainFileManagerScreen(
    sandboxRoot: File,
    phoneRoot: File,
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit,
    onOpenFile: (File, String) -> Unit, // file and "editor" | "zip" | "image" | "sound" | "hex"
    modifier: Modifier = Modifier
) {
    var isBrowsingPhoneStorage by remember { mutableStateOf(false) }
    var currentDirectory by remember { mutableStateOf(sandboxRoot) }
    var showPermissionExplainer by remember { mutableStateOf(false) }
    
    val activeRoot = if (isBrowsingPhoneStorage && hasStoragePermission) phoneRoot else sandboxRoot

    var fileList by remember { mutableStateOf<List<File>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Source", "Media", "Archives", "Images"

    // Dialog state controllers
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    
    var activeItemActions by remember { mutableStateOf<File?>(null) } // BottomSheet control
    var showRenameDialog by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<File?>(null) }
    var showDetailsDialog by remember { mutableStateOf<File?>(null) }

    // Read files directory list
    val refreshFilesList = {
        val files = try {
            currentDirectory.listFiles()?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        // Folders first, files after
        fileList = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
    }

    LaunchedEffect(isBrowsingPhoneStorage, hasStoragePermission) {
        if (isBrowsingPhoneStorage) {
            if (hasStoragePermission) {
                if (!currentDirectory.absolutePath.startsWith(phoneRoot.absolutePath)) {
                    currentDirectory = phoneRoot
                }
            } else {
                currentDirectory = sandboxRoot
                isBrowsingPhoneStorage = false
            }
        } else {
            if (!currentDirectory.absolutePath.startsWith(sandboxRoot.absolutePath)) {
                currentDirectory = sandboxRoot
            }
        }
    }

    LaunchedEffect(currentDirectory) {
        refreshFilesList()
    }

    // Storage estimation metrics
    val storageMetrics = remember(fileList, currentDirectory, isBrowsingPhoneStorage, hasStoragePermission) {
        if (isBrowsingPhoneStorage && hasStoragePermission) {
            try {
                val stat = StatFs(phoneRoot.path)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                val availableBlocks = stat.availableBlocksLong
                
                val totalBytes = totalBlocks * blockSize
                val availableBytes = availableBlocks * blockSize
                val usedBytes = totalBytes - availableBytes
                
                val gbUsed = usedBytes.toDouble() / (1024 * 1024 * 1024)
                val gbTotal = totalBytes.toDouble() / (1024 * 1024 * 1024)
                
                val ratio = (gbUsed / gbTotal).coerceIn(0.01, 1.0)
                Pair(String.format(Locale.ROOT, "%.1f GB of %.1f GB", gbUsed, gbTotal), ratio.toFloat())
            } catch (_: Exception) {
                Pair("Phone Storage Available", 0.5f)
            }
        } else {
            var usedSpace: Long = 0
            fun accum(f: File) {
                try {
                    if (f.isFile) usedSpace += f.length()
                    else f.listFiles()?.forEach { accum(it) }
                } catch (_: Exception) {}
            }
            accum(sandboxRoot)
            val mbUsed = usedSpace.toDouble() / (1024 * 1024)
            val limitMb = 12.0 // simulate sandboxed quota
            val ratio = (mbUsed / limitMb).coerceIn(0.01, 1.0)
            Pair(String.format(Locale.ROOT, "%.1f MB of %.1f MB", mbUsed, limitMb), ratio.toFloat())
        }
    }

    // Filter logic
    val filteredFiles = remember(fileList, searchQuery, selectedFilter) {
        fileList.filter { file ->
            val matchesSearch = file.name.contains(searchQuery, ignoreCase = true)
            val matchesChip = when (selectedFilter) {
                "Source" -> !file.isDirectory && file.extension.lowercase(Locale.ROOT) in listOf("kt", "kts", "java", "js", "py", "css", "xml", "json", "md", "txt")
                "Media" -> !file.isDirectory && file.extension.lowercase(Locale.ROOT) in listOf("mp3", "wav", "m4a", "ogg")
                "Archives" -> !file.isDirectory && file.extension.lowercase(Locale.ROOT) == "zip"
                "Images" -> !file.isDirectory && file.extension.lowercase(Locale.ROOT) in listOf("png", "jpg", "jpeg", "svg")
                else -> true
            }
            matchesSearch && matchesChip
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(SleekBg)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Files",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextMain,
                            letterSpacing = (-0.5).sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            // Sandbox Mode Selector
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (!isBrowsingPhoneStorage) SleekFolderBg else SleekOtherBg,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { isBrowsingPhoneStorage = false }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Sandbox Environment",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isBrowsingPhoneStorage) SleekFolderText else SleekTextAlt
                                )
                            }
                            
                            // Phone Storage Mode Selector
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isBrowsingPhoneStorage) SleekFolderBg else SleekOtherBg,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (!hasStoragePermission) {
                                            showPermissionExplainer = true
                                        } else {
                                            isBrowsingPhoneStorage = true
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (isBrowsingPhoneStorage) SleekFolderText else SleekTextAlt,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Phone Files",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBrowsingPhoneStorage) SleekFolderText else SleekTextAlt
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "Active: " + (if (currentDirectory.absolutePath == activeRoot.absolutePath) "root" else currentDirectory.name),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextSub,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Top quick file creators
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showCreateDirDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = SleekFolderBg),
                            modifier = Modifier.testTag("create_folder_button")
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Directory", tint = SleekFolderText)
                        }
                        IconButton(
                            onClick = { showCreateFileDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = SleekCodeBg),
                            modifier = Modifier.testTag("create_file_button")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "New Source File", tint = SleekCodeText)
                        }
                    }
                }

                // Breadcrumbs navigation row
                BreadcrumbsRow(
                    root = activeRoot,
                    current = currentDirectory,
                    onBreadcrumbClick = { currentDirectory = it }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SleekBottomNavBg,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(80.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Files Tab
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Stay on files */ },
                    icon = {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(32.dp)
                                .background(SleekFolderBg, shape = RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Files",
                                tint = SleekFolderText
                            )
                        }
                    },
                    label = { Text("Files", fontWeight = FontWeight.Bold, color = SleekFolderText) }
                )

                // Recent Tab
                NavigationBarItem(
                    selected = false,
                    onClick = { /* Decorative only */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recent",
                            tint = SleekTextAlt.copy(alpha = 0.6f)
                        )
                    },
                    label = { Text("Recent", color = SleekTextAlt.copy(alpha = 0.6f)) }
                )

                // Tools Tab
                NavigationBarItem(
                    selected = false,
                    onClick = { /* Decorative only */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Tools",
                            tint = SleekTextAlt.copy(alpha = 0.6f)
                        )
                    },
                    label = { Text("Tools", color = SleekTextAlt.copy(alpha = 0.6f)) }
                )

                // Settings Tab
                NavigationBarItem(
                    selected = false,
                    onClick = { /* Decorative only */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = SleekTextAlt.copy(alpha = 0.6f)
                        )
                    },
                    label = { Text("Settings", color = SleekTextAlt.copy(alpha = 0.6f)) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateFileDialog = true },
                containerColor = SleekFolderBg,
                contentColor = SleekFolderText,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("floating_add_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Item",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(SleekBg)
        ) {
            // Search and Filter Bar Layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search files & archives...", color = SleekTextSub) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SleekTextAlt) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Seek", tint = SleekTextAlt)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("search_field"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = SleekFolderBg,
                        unfocusedBorderColor = SleekBorderLight,
                        focusedTextColor = SleekTextMain,
                        unfocusedTextColor = SleekTextMain
                    ),
                    singleLine = true
                )

                // Category Filter Chips Row (No nested horizontal structures, simple custom components)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Source", "Media", "Archives", "Images")
                    filters.forEach { filterName ->
                        val selected = selectedFilter == filterName
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = filterName },
                            label = { Text(filterName, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekFolderBg,
                                selectedLabelColor = SleekFolderText,
                                containerColor = Color.White,
                                labelColor = SleekTextAlt
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = if (selected) SleekFolderBg else SleekBorderLight,
                                selectedBorderColor = SleekFolderBg
                            )
                        )
                    }
                }

                // Storage card positioned safely inside top dashboard to avoid floating overlaps
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SleekFolderBg, shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SdStorage, contentDescription = null, tint = SleekFolderText, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isBrowsingPhoneStorage) "Active Device Storage" else "Storage Metrics Limit", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = SleekTextSub, 
                                    fontWeight = FontWeight.Bold
                                )
                                Text(storageMetrics.first, style = MaterialTheme.typography.labelSmall, color = SleekFolderText, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { storageMetrics.second },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = SleekFolderText,
                                trackColor = SleekOtherBg
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Directional Sliding Directory Transition Anim
            AnimatedContent(
                targetState = currentDirectory,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    if (targetState.path.length > initialState.path.length) {
                        slideInHorizontally { width -> width / 3 } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width / 3 } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width / 3 } + fadeOut()
                    }
                },
                label = "directory_flip"
            ) { activeDir ->
                if (filteredFiles.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = SleekTextSub.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No matching files found here", color = SleekTextSub, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredFiles) { file ->
                            FileElementRow(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        currentDirectory = file
                                    } else {
                                        val ext = file.extension.lowercase(Locale.ROOT)
                                        when {
                                            ext == "zip" -> onOpenFile(file, "zip")
                                            ext in listOf("png", "jpg", "jpeg") -> onOpenFile(file, "image")
                                            ext == "svg" -> onOpenFile(file, "editor")
                                            ext in listOf("wav", "mp3", "m4a") -> onOpenFile(file, "sound")
                                            ext == "bin" || ext == "hex" -> onOpenFile(file, "hex")
                                            else -> onOpenFile(file, "editor")
                                        }
                                    }
                                },
                                onOptions = {
                                    activeItemActions = file
                                }
                            )
                        }
                    }
                }
            }

            // Empty space block
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    // Modal Actions bottom sheet light style
    activeItemActions?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { activeItemActions = null },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = SleekBorderLight) }
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp, 
                    color = SleekTextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                DropdownMenuItem(
                    text = { Text("Rename Element", color = SleekTextMain, fontWeight = FontWeight.Medium) },
                    onClick = {
                        activeItemActions = null
                        showRenameDialog = item
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = SleekTextSub) }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate Resource", color = SleekTextMain, fontWeight = FontWeight.Medium) },
                    onClick = {
                        activeItemActions = null
                        try {
                            if (item.isFile) {
                                val dupName = item.nameWithoutExtension + "_copy." + item.extension
                                val dupFile = File(item.parentFile, dupName)
                                item.copyTo(dupFile, overwrite = true)
                                refreshFilesList()
                            }
                        } catch (_: Exception) {}
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SleekTextSub) }
                )
                DropdownMenuItem(
                    text = { Text("View Detailed Metadata", color = SleekTextMain, fontWeight = FontWeight.Medium) },
                    onClick = {
                        activeItemActions = null
                        showDetailsDialog = item
                    },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = SleekTextSub) }
                )
                DropdownMenuItem(
                    text = { Text("Inspect Raw HEX Bytes", color = SleekTextMain, fontWeight = FontWeight.Medium) },
                    onClick = {
                        activeItemActions = null
                        onOpenFile(item, "hex")
                    },
                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, tint = SleekTextSub) }
                )
                HorizontalDivider(color = SleekBorderLight)
                DropdownMenuItem(
                    text = { Text("Secure Delete", color = Color(0xFFBA1A1A), fontWeight = FontWeight.Bold) },
                    onClick = {
                        activeItemActions = null
                        showDeleteConfirmDialog = item
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFBA1A1A)) }
                )
            }
        }
    }

    // Dialog: Create Folder
    if (showCreateDirDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDirDialog = false },
            title = { Text("Create Directory", color = SleekTextMain, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    placeholder = { Text("Folder name ...", color = SleekTextSub) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekFolderText,
                        unfocusedBorderColor = SleekBorderLight
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.trim().isNotEmpty()) {
                            val d = File(currentDirectory, newItemName.trim())
                            d.mkdirs()
                            newItemName = ""
                            showCreateDirDialog = false
                            refreshFilesList()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText)
                ) { Text("Create", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDirDialog = false }) { Text("Cancel", color = SleekTextSub) }
            }
        )
    }

    // Dialog: Create File
    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create Source File", color = SleekTextMain, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    placeholder = { Text("app_helper.kt / vector.svg ...", color = SleekTextSub) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekFolderText,
                        unfocusedBorderColor = SleekBorderLight
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.trim().isNotEmpty()) {
                            val f = File(currentDirectory, newItemName.trim())
                            try {
                                f.createNewFile()
                                val ext = f.extension.lowercase(Locale.ROOT)
                                if (ext == "svg") {
                                    f.writeText("<svg viewBox=\"0 0 100 100\">\n  <circle cx=\"50\" cy=\"50\" r=\"30\" fill=\"#3D5AFE\" />\n</svg>")
                                } else if (ext == "kt") {
                                    f.writeText("package com.example\n\nfun main() {\n    println(\"Initialized program!\")\n}")
                                }
                            } catch (_: Exception) {}
                            newItemName = ""
                            showCreateFileDialog = false
                            refreshFilesList()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText)
                ) { Text("Create", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) { Text("Cancel", color = SleekTextSub) }
            }
        )
    }

    // Dialog: Rename
    showRenameDialog?.let { item ->
        var renameText by remember { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Element", color = SleekTextMain, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekFolderText,
                        unfocusedBorderColor = SleekBorderLight
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.trim().isNotEmpty() && renameText != item.name) {
                            val dest = File(item.parentFile, renameText.trim())
                            if (item.renameTo(dest)) {
                                refreshFilesList()
                            }
                            showRenameDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText)
                ) { Text("Confirm", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("Cancel", color = SleekTextSub) }
            }
        )
    }

    // Dialog: Delete
    showDeleteConfirmDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Secure Delete Action", color = SleekTextMain, fontWeight = FontWeight.Bold) },
            text = { Text("Are you completely sure you want to permanently erase: ${item.name}?", color = SleekTextAlt) },
            confirmButton = {
                Button(
                    onClick = {
                        if (item.isDirectory) {
                            item.deleteRecursively()
                        } else {
                            item.delete()
                        }
                        showDeleteConfirmDialog = null
                        refreshFilesList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A), contentColor = Color.White)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text("Cancel", color = SleekTextSub) }
            }
        )
    }

    // Dialog: Metadata details
    showDetailsDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDetailsDialog = null },
            title = { Text("Document Properties", color = SleekTextMain, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: ${item.name}", fontWeight = FontWeight.Bold, color = SleekTextMain)
                    Text("Absolute Path:", fontSize = 11.sp, color = SleekTextSub)
                    Text(item.absolutePath, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SleekTextAlt)
                    Text("Type: ${if (item.isDirectory) "Directory Folder" else "Standard File"}", color = SleekTextAlt)
                    if (item.isFile) {
                        Text("Size on storage: ${item.length() / 1024} KB (${item.length()} bytes)", color = SleekTextAlt)
                    }
                    val date = Date(item.lastModified())
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    Text("Last modified: ${format.format(date)}", color = SleekTextAlt)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetailsDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText)
                ) { Text("Close", fontWeight = FontWeight.Bold) }
            }
        )
    }

    // Dialog: Permission Explainer
    if (showPermissionExplainer) {
        AlertDialog(
            onDismissRequest = { showPermissionExplainer = false },
            title = { Text("Grant Storage Access", color = SleekTextMain, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "To browse and manage your actual device files directly from this manager, please grant storage permissions. You will be redirected to settings to allow All Files access.",
                    color = SleekTextAlt
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionExplainer = false
                        onRequestStoragePermission()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText)
                ) { Text("Allow Access", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionExplainer = false }) { Text("Cancel", color = SleekTextSub) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreadcrumbsRow(
    root: File,
    current: File,
    onBreadcrumbClick: (File) -> Unit
) {
    val segments = remember(current) {
        val list = mutableListOf<File>()
        var curr: File? = current
        while (curr != null) {
            list.add(curr)
            if (curr.absolutePath == root.absolutePath) break
            curr = curr.parentFile
        }
        list.reverse()
        list
    }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        segments.forEachIndexed { idx, dir ->
            val isLast = idx == segments.lastIndex
            Text(
                text = if (dir.absolutePath == root.absolutePath) "ROOT" else dir.name,
                fontSize = 12.sp,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                color = if (isLast) SleekFolderText else SleekTextSub,
                modifier = Modifier
                    .background(
                        color = if (isLast) SleekFolderBg else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(!isLast) { onBreadcrumbClick(dir) }
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            )

            if (!isLast) {
                Text("/", color = SleekTextSub.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileElementRow(
    file: File,
    onClick: () -> Unit,
    onOptions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onOptions
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ext = file.extension.lowercase(Locale.ROOT)
        val (bgCol, txtCol, icon) = when {
            file.isDirectory -> Triple(SleekFolderBg, SleekFolderText, Icons.Default.Folder)
            ext in listOf("kt", "kts", "java", "js", "py", "css", "xml", "json", "md", "txt") -> Triple(SleekCodeBg, SleekCodeText, Icons.Default.IntegrationInstructions)
            ext in listOf("png", "jpg", "jpeg") -> Triple(SleekImageBg, SleekImageText, Icons.Default.Image)
            ext == "svg" -> Triple(SleekImageBg, SleekImageText, Icons.Default.Palette)
            ext == "zip" -> Triple(SleekZipBg, SleekZipText, Icons.Default.FolderZip)
            ext in listOf("mp3", "wav", "m4a", "ogg") -> Triple(SleekAudioBg, SleekAudioText, Icons.Default.LibraryMusic)
            else -> Triple(SleekOtherBg, SleekOtherText, Icons.Default.InsertDriveFile)
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(bgCol, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = txtCol,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                color = SleekTextMain,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val detailsText = if (file.isDirectory) {
                val qty = file.listFiles()?.size ?: 0
                "$qty elements"
            } else {
                val sizeKb = file.length() / 1024
                val date = Date(file.lastModified())
                val format = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                val typeLabel = when {
                    ext == "zip" -> "Archive"
                    ext in listOf("mp3", "wav", "m4a") -> "Audio"
                    ext == "svg" -> "SVG"
                    ext in listOf("png", "jpg", "jpeg") -> "Image"
                    ext in listOf("kt", "java", "js") -> "Kotlin"
                    else -> "Binary"
                }
                "$typeLabel · $sizeKb KB · ${format.format(date)}"
            }

            Text(
                text = detailsText,
                style = MaterialTheme.typography.labelSmall,
                color = SleekTextSub
            )
        }

        // For code or edit files, render EDIT small tag to match Design HTML exactly!
        if (!file.isDirectory && ext in listOf("kt", "java", "js", "py", "svg")) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .background(SleekFolderBg, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "EDIT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekFolderText
                )
            }
        }

        IconButton(onClick = onOptions) {
            Icon(Icons.Default.MoreVert, contentDescription = "Resource options", tint = SleekTextSub)
        }
    }
}
