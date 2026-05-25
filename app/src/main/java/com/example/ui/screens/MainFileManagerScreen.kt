package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import android.os.StatFs
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainTab {
    FILES, RECENT, TOOLS, SETTINGS
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainFileManagerScreen(
    phoneRoot: File,
    currentDirectory: File,
    onDirectoryChange: (File) -> Unit,
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit,
    onOpenFile: (File, String) -> Unit, // file and "editor" | "zip" | "image" | "sound" | "hex"
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(MainTab.FILES) }
    var showPermissionExplainer by remember { mutableStateOf(false) }
    
    val activeRoot = phoneRoot

    // Navigate to the parent directory on system back gesture when inside a subdirectory
    androidx.activity.compose.BackHandler(enabled = activeTab == MainTab.FILES && currentDirectory.absolutePath != activeRoot.absolutePath) {
        currentDirectory.parentFile?.let {
            onDirectoryChange(it)
        }
    }

    var fileList by remember { mutableStateOf<List<File>>(emptyList()) }
    var isFolderLoading by remember { mutableStateOf(false) }
    var recentFilesKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Source", "Media", "Archives", "Images"
    var sortMode by remember { mutableStateOf("Name (A-Z)") }

    // Dialog state controllers
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    
    var binaryChoiceFile by remember { mutableStateOf<File?>(null) }
    var largeFileWarningFile by remember { mutableStateOf<File?>(null) }
    
    var activeItemActions by remember { mutableStateOf<File?>(null) } // BottomSheet control
    var showRenameDialog by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<File?>(null) }
    var showDetailsDialog by remember { mutableStateOf<File?>(null) }

    val context = LocalContext.current

    val handleFileOpen = { file: File ->
        val ext = file.extension.lowercase(Locale.ROOT)
        when {
            ext in listOf("mcpack", "mcworld", "mctemplate", "mcaddon") -> {
                val intent = Intent(Intent.ACTION_VIEW)
                val authority = "${context.packageName}.fileprovider"
                val fileUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                intent.setDataAndType(fileUri, "application/octet-stream")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(intent, "Open with Minecraft..."))
            }
            ext == "zip" -> onOpenFile(file, "zip")
            ext in listOf("png", "jpg", "jpeg", "webp") -> onOpenFile(file, "image")
            ext == "svg" -> onOpenFile(file, "editor")
            ext in listOf("wav", "mp3", "m4a", "ogg", "flac", "mp4", "mkv") -> onOpenFile(file, "sound")
            ext == "bin" || ext == "hex" -> onOpenFile(file, "hex")
            ext == "apk" -> {
                installApk(context, file)
            }
            else -> {
                if (isBinaryFile(file)) {
                    binaryChoiceFile = file
                } else if (file.length() > 1.5 * 1024 * 1024) {
                    largeFileWarningFile = file
                } else {
                    onOpenFile(file, "editor")
                }
            }
        }
    }

    // Read files directory list
    val refreshFilesList = {
        isFolderLoading = true
        scope.launch(Dispatchers.IO) {
            val query = searchQuery
            if (query.isNotBlank()) delay(400) else delay(100) // debounce for deep search
            
            val files = try {
                if (hasStoragePermission) {
                    if (query.isNotBlank()) {
                        val result = mutableListOf<File>()
                        try {
                            currentDirectory.walkTopDown().maxDepth(10).forEach { file ->
                                if (file.name.contains(query, ignoreCase = true) && file.absolutePath != currentDirectory.absolutePath) {
                                    result.add(file)
                                    if (result.size > 1000) return@forEach // Cap at 1000
                                }
                            }
                        } catch (e: Exception) {}
                        result
                    } else {
                        currentDirectory.listFiles()?.toList() ?: emptyList()
                    }
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }
            val sorted = files.sortedWith(Comparator { f1, f2 ->
                if (f1.isDirectory && !f2.isDirectory) -1
                else if (!f1.isDirectory && f2.isDirectory) 1
                else {
                    when (sortMode) {
                        "Name (A-Z)" -> f1.name.lowercase(Locale.ROOT).compareTo(f2.name.lowercase(Locale.ROOT))
                        "Name (Z-A)" -> f2.name.lowercase(Locale.ROOT).compareTo(f1.name.lowercase(Locale.ROOT))
                        "Size (Asc)" -> f1.length().compareTo(f2.length())
                        "Size (Desc)" -> f2.length().compareTo(f1.length())
                        "Date (Desc)" -> f2.lastModified().compareTo(f1.lastModified())
                        "Date (Asc)" -> f1.lastModified().compareTo(f2.lastModified())
                        "Type (A-Z)" -> f1.extension.lowercase(Locale.ROOT).compareTo(f2.extension.lowercase(Locale.ROOT))
                        "Type (Z-A)" -> f2.extension.lowercase(Locale.ROOT).compareTo(f1.extension.lowercase(Locale.ROOT))
                        else -> f1.name.lowercase(Locale.ROOT).compareTo(f2.name.lowercase(Locale.ROOT))
                    }
                }
            })
            withContext(Dispatchers.Main) {
                fileList = sorted
                isFolderLoading = false
            }
        }
    }

    LaunchedEffect(hasStoragePermission, currentDirectory, sortMode, searchQuery) {
        refreshFilesList()
    }

    // Storage estimation metrics
    val storageMetrics = remember(hasStoragePermission, fileList, currentDirectory) {
        if (hasStoragePermission) {
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
                Pair("Phone Storage Enabled", 0.5f)
            }
        } else {
            Pair("Permission Access Denied", 0f)
        }
    }

    // Filter logic
    val filteredFiles = remember(fileList, searchQuery, selectedFilter) {
        fileList.filter { file ->
            val matchesSearch = file.name.contains(searchQuery, ignoreCase = true)
            val matchesChip = when (selectedFilter) {
                "Source" -> !file.isDirectory && file.extension.lowercase(Locale.ROOT) in listOf("kt", "kts", "java", "js", "py", "css", "xml", "json", "md", "txt", "svg")
                "Media" -> !file.isDirectory && file.extension.lowercase(Locale.ROOT) in listOf("mp3", "wav", "m4a", "ogg", "flac", "mp4", "mkv")
                "Archives" -> !file.isDirectory && file.extension.lowercase(Locale.ROOT) in listOf("zip", "mcpack", "mcworld", "mctemplate", "mcaddon")
                "Images" -> !file.isDirectory && file.extension.lowercase(Locale.ROOT) in listOf("png", "jpg", "jpeg", "svg")
                else -> true
            }
            matchesSearch && matchesChip
        }
    }

    val orientation = androidx.compose.ui.platform.LocalConfiguration.current.orientation
    val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Row(modifier = modifier.fillMaxSize()) {
        if (isLandscape) {
            val tabFiles = androidx.compose.ui.res.stringResource(com.example.R.string.tab_files)
            val tabRecent = androidx.compose.ui.res.stringResource(com.example.R.string.tab_recent)
            val tabTools = androidx.compose.ui.res.stringResource(com.example.R.string.tab_tools)
            val tabSettings = androidx.compose.ui.res.stringResource(com.example.R.string.tab_settings)
            
            NavigationRail(
                containerColor = SleekBottomNavBg,
            ) {
                NavigationRailItem(
                    selected = activeTab == MainTab.FILES,
                    onClick = { activeTab = MainTab.FILES },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Files", tint = if (activeTab == MainTab.FILES) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) },
                    label = { Text(tabFiles, fontWeight = FontWeight.Bold, color = if (activeTab == MainTab.FILES) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) }
                )
                NavigationRailItem(
                    selected = activeTab == MainTab.RECENT,
                    onClick = { activeTab = MainTab.RECENT },
                    icon = { Icon(Icons.Default.History, contentDescription = "Recent", tint = if (activeTab == MainTab.RECENT) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) },
                    label = { Text(tabRecent, fontWeight = FontWeight.Bold, color = if (activeTab == MainTab.RECENT) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) }
                )
                NavigationRailItem(
                    selected = activeTab == MainTab.TOOLS,
                    onClick = { activeTab = MainTab.TOOLS },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Tools", tint = if (activeTab == MainTab.TOOLS) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) },
                    label = { Text(tabTools, fontWeight = FontWeight.Bold, color = if (activeTab == MainTab.TOOLS) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) }
                )
                NavigationRailItem(
                    selected = activeTab == MainTab.SETTINGS,
                    onClick = { activeTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (activeTab == MainTab.SETTINGS) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) },
                    label = { Text(tabSettings, fontWeight = FontWeight.Bold, color = if (activeTab == MainTab.SETTINGS) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) }
                )
            }
        }
        Scaffold(
        topBar = {
            when (activeTab) {
                MainTab.FILES -> {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (currentDirectory.absolutePath != activeRoot.absolutePath) {
                                    IconButton(
                                        onClick = {
                                            currentDirectory.parentFile?.let { onDirectoryChange(it) }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Parent Directory",
                                            tint = SleekTextMain
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = androidx.compose.ui.res.stringResource(com.example.R.string.header_explorer),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekTextMain,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        text = if (!hasStoragePermission) "Access Required" else androidx.compose.ui.res.stringResource(com.example.R.string.subtitle_in, (if (currentDirectory.absolutePath == activeRoot.absolutePath) "root" else currentDirectory.name)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SleekTextSub,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            
                            if (hasStoragePermission) {
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
                        }

                        if (hasStoragePermission) {
                            BreadcrumbsRow(
                                root = activeRoot,
                                current = currentDirectory,
                                onBreadcrumbClick = { onDirectoryChange(it) }
                            )
                        }
                    }
                }
                MainTab.RECENT -> {
                    Column(
                        modifier = Modifier
                            .background(SleekBg)
                            .padding(top = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Recent History",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextMain,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "Recently opened file elements",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SleekTextSub,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                    prefs.edit().putString("recent_files", "").apply()
                                    recentFilesKey++
                                    Toast.makeText(context, "History cleared successfully", Toast.LENGTH_SHORT).show()
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = SleekOtherBg)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = SleekTextAlt)
                            }
                        }
                    }
                }
                MainTab.TOOLS -> {
                    Column(
                        modifier = Modifier
                            .background(SleekBg)
                            .padding(top = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Studio Tools",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextMain,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "Advanced integrated diagnostics utilities",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SleekTextSub,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                MainTab.SETTINGS -> {
                    Column(
                        modifier = Modifier
                            .background(SleekBg)
                            .padding(top = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextMain,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "Properties, permissions, and app reset",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SleekTextSub,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!isLandscape) {
                val tabFiles = androidx.compose.ui.res.stringResource(com.example.R.string.tab_files)
                val tabRecent = androidx.compose.ui.res.stringResource(com.example.R.string.tab_recent)
                val tabTools = androidx.compose.ui.res.stringResource(com.example.R.string.tab_tools)
                val tabSettings = androidx.compose.ui.res.stringResource(com.example.R.string.tab_settings)

                NavigationBar(
                    containerColor = SleekBottomNavBg,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == MainTab.FILES,
                        onClick = { activeTab = MainTab.FILES },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Files", tint = if (activeTab == MainTab.FILES) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) },
                        label = { Text(tabFiles, fontWeight = FontWeight.Bold, color = if (activeTab == MainTab.FILES) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) }
                    )
                    NavigationBarItem(
                        selected = activeTab == MainTab.RECENT,
                        onClick = { activeTab = MainTab.RECENT },
                        icon = { Icon(Icons.Default.History, contentDescription = "Recent", tint = if (activeTab == MainTab.RECENT) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) },
                        label = { Text(tabRecent, fontWeight = FontWeight.Bold, color = if (activeTab == MainTab.RECENT) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) }
                    )
                    NavigationBarItem(
                        selected = activeTab == MainTab.TOOLS,
                        onClick = { activeTab = MainTab.TOOLS },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Tools", tint = if (activeTab == MainTab.TOOLS) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) },
                        label = { Text(tabTools, fontWeight = FontWeight.Bold, color = if (activeTab == MainTab.TOOLS) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) }
                    )
                    NavigationBarItem(
                        selected = activeTab == MainTab.SETTINGS,
                        onClick = { activeTab = MainTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (activeTab == MainTab.SETTINGS) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) },
                        label = { Text(tabSettings, fontWeight = FontWeight.Bold, color = if (activeTab == MainTab.SETTINGS) SleekFolderText else SleekTextAlt.copy(alpha = 0.6f)) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == MainTab.FILES && hasStoragePermission) {
                FloatingActionButton(
                    onClick = { showCreateFileDialog = true },
                    containerColor = SleekFolderBg,
                    contentColor = SleekFolderText,
                    modifier = Modifier.testTag("floating_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
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
            when (activeTab) {
                MainTab.FILES -> {
                    if (!hasStoragePermission) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = SleekFolderText.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Phone Storage Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = SleekTextMain
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This tab accesses your phone's external files directory. Please configure developer storage permission to proceed.",
                                fontSize = 13.sp,
                                color = SleekTextSub,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { showPermissionExplainer = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText)
                            ) {
                                Text("Configure Permission", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        val viewHeader = @Composable {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("search_files_input"),
                                placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.search_placeholder), color = SleekTextSub) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SleekTextSub) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = SleekTextSub)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = SleekBorderLight,
                                    unfocusedBorderColor = SleekBorderLight
                                )
                            )

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("All", "Source", "Media", "Archives", "Images").forEach { filterName ->
                                        val selected = selectedFilter == filterName
                                        val translatedName = when (filterName) {
                                            "All" -> androidx.compose.ui.res.stringResource(com.example.R.string.filter_all)
                                            "Source" -> androidx.compose.ui.res.stringResource(com.example.R.string.filter_source)
                                            "Media" -> androidx.compose.ui.res.stringResource(com.example.R.string.filter_media)
                                            "Archives" -> androidx.compose.ui.res.stringResource(com.example.R.string.filter_archives)
                                            "Images" -> androidx.compose.ui.res.stringResource(com.example.R.string.filter_images)
                                            else -> filterName
                                        }
                                        FilterChip(
                                            selected = selected,
                                            onClick = { selectedFilter = filterName },
                                            label = {
                                                Text(
                                                    text = translatedName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            },
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
                                Box {
                                    var showSortMenu by remember { mutableStateOf(false) }
                                    IconButton(onClick = { showSortMenu = true }) {
                                        Icon(Icons.Default.Sort, "Sort", tint = SleekFolderText)
                                    }
                                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                        listOf("Name (A-Z)", "Name (Z-A)", "Size (Asc)", "Size (Desc)", "Date (Desc)", "Date (Asc)", "Type (A-Z)", "Type (Z-A)").forEach { mode ->
                                            DropdownMenuItem(
                                                text = { Text(mode, fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = { sortMode = mode; showSortMenu = false }
                                            )
                                        }
                                    }
                                }
                            }

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
                                                text = androidx.compose.ui.res.stringResource(com.example.R.string.active_device_storage),
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

                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        val viewContent = @Composable {
                            AnimatedContent(
                                targetState = currentDirectory,
                                modifier = Modifier
                                    .fillMaxSize(),
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
                                if (isFolderLoading) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        androidx.compose.material3.LoadingIndicator(color = SleekFolderText)
                                        Spacer(Modifier.height(12.dp))
                                        Text("Loading directory...", color = SleekTextSub, style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else if (currentDirectory.absolutePath.endsWith("Android/data", ignoreCase = true)) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            androidx.compose.material.icons.Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = SleekTextAlt,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text("Access Denied", color = SleekTextMain, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Due to new system limitations, you cannot access this folder directly. Download Shizuku to view.", color = SleekTextSub, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                } else if (filteredFiles.isEmpty()) {
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
                                        contentPadding = PaddingValues(bottom = 80.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(filteredFiles) { file ->
                                            FileElementRow(
                                                file = file,
                                                onClick = {
                                                    if (file.isDirectory) {
                                                        onDirectoryChange(file)
                                                    } else {
                                                        addRecentFile(context, file)
                                                        handleFileOpen(file)
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
                        }

                        if (isLandscape) {
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                Column(
                                    modifier = Modifier
                                        .weight(0.45f)
                                        .padding(end = 16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    viewHeader()
                                }
                                Box(modifier = Modifier.weight(0.55f)) {
                                    viewContent()
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                viewHeader()
                                viewContent()
                            }
                        }
                    }
                }
                MainTab.RECENT -> {
                    val recentsList = remember(activeTab, recentFilesKey) { getRecentFiles(context) }
                    
                    if (recentsList.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = SleekTextSub.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("No Recent History", fontWeight = FontWeight.Bold, color = SleekTextMain)
                            Text("Files you open or view will appear here for fast access.", color = SleekTextSub, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(recentsList) { file ->
                                FileElementRow(
                                    file = file,
                                    onClick = {
                                        handleFileOpen(file)
                                    },
                                    onOptions = {
                                        activeItemActions = file
                                    }
                                )
                            }
                        }
                    }
                }
                MainTab.TOOLS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Active Core Suites", fontWeight = FontWeight.Bold, color = SleekTextMain, fontSize = 16.sp)

                        ToolLaunchCard(
                            title = "Advanced Equalizer & Deck",
                            desc = "Open Music Deck to change speed/pitch, utilize 5-band equalizer, and compute real-time BPM values.",
                            icon = Icons.Default.LibraryMusic,
                            iconColor = SleekAudioText,
                            iconBg = SleekAudioBg,
                            onClick = {
                                selectedFilter = "Media"
                                activeTab = MainTab.FILES
                            }
                        )

                        ToolLaunchCard(
                            title = "SVG Live rendering",
                            desc = "Inspect raw SVG vector tags inside code view, auto-adjust dimensions, and translate to Vector XML.",
                            icon = Icons.Default.Palette,
                            iconColor = SleekImageText,
                            iconBg = SleekImageBg,
                            onClick = {
                                selectedFilter = "Images"
                                activeTab = MainTab.FILES
                            }
                        )

                        ToolLaunchCard(
                            title = "Zip Archive Packer / Extractor",
                            desc = "Compress any directory into standard archives, inspect file headers, and extract data files safely.",
                            icon = Icons.Default.FolderZip,
                            iconColor = SleekZipText,
                            iconBg = SleekZipBg,
                            onClick = {
                                selectedFilter = "Archives"
                                activeTab = MainTab.FILES
                            }
                        )

                        ToolLaunchCard(
                            title = "Low-Level HEX Analyzer",
                            desc = "Locate binary assets, scan byte-level properties, and duplicate resources on local storage.",
                            icon = Icons.Default.Code,
                            iconColor = SleekCodeText,
                            iconBg = SleekCodeBg,
                            onClick = {
                                selectedFilter = "All"
                                activeTab = MainTab.FILES
                            }
                        )

                        Spacer(Modifier.height(40.dp))
                    }
                }
                MainTab.SETTINGS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("App Properties", fontWeight = FontWeight.Bold, color = SleekTextMain, fontSize = 16.sp)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Application Label", color = SleekTextSub, fontSize = 13.sp)
                                    Text("FileSmith Studio", color = SleekTextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                HorizontalDivider(color = SleekBorderLight)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Software Version", color = SleekTextSub, fontSize = 13.sp)
                                    Text("v1.3.1 - Expressive", color = SleekTextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                HorizontalDivider(color = SleekBorderLight)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Dev Storage Access", color = SleekTextSub, fontSize = 13.sp)
                                    Text(if (hasStoragePermission) "Granted (All-Access)" else "Denied", color = if (hasStoragePermission) SleekFolderText else Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        com.example.ui.components.LanguagePreference()

                        Text("Performance & Cache Controls", fontWeight = FontWeight.Bold, color = SleekTextMain, fontSize = 16.sp)

                        Button(
                            onClick = {
                                val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().clear().apply()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reset App Preferences & Cache", fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
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
                if (item.isFile) {
                    if (item.extension.lowercase(Locale.ROOT) == "apk") {
                        DropdownMenuItem(
                            text = { Text("Install APK Package", color = SleekFolderText, fontWeight = FontWeight.Bold) },
                            onClick = {
                                activeItemActions = null
                                installApk(context, item)
                            },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = SleekFolderText) }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Download / Save to Downloads", color = SleekTextMain, fontWeight = FontWeight.Medium) },
                        onClick = {
                            activeItemActions = null
                            downloadFileToPublicDownloads(context, item)
                        },
                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = SleekTextSub) }
                    )
                }
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
    } // End of Scaffold content
    } // End of Row

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
                    placeholder = { Text("util.kt / vector.svg ...", color = SleekTextSub) },
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

    // Dialog: Binary Choice Alert
    binaryChoiceFile?.let { targetFile ->
        AlertDialog(
            onDismissRequest = { binaryChoiceFile = null },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = SleekFolderText) },
            title = { Text("Unsupported / Binary File", fontWeight = FontWeight.Bold, color = SleekTextMain) },
            text = {
                Text(
                    "The file \"${targetFile.name}\" appears to contain binary data or uses an unsupported media format.\n\n" +
                    "Opening it directly in the text editor might cause severe lag or completely crash the application.\n\n" +
                    "How would you like to inspect this file?",
                    color = SleekTextMain
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        binaryChoiceFile = null
                        onOpenFile(targetFile, "hex")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekFolderText)
                ) {
                    Text("Open in Hex Inspector", color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            binaryChoiceFile = null
                            onOpenFile(targetFile, "editor")
                        }
                    ) {
                        Text("Force Open as Text", color = SleekTextSub)
                    }
                    TextButton(
                        onClick = { binaryChoiceFile = null }
                    ) {
                        Text("Cancel", color = SleekTextSub)
                    }
                }
            },
            containerColor = SleekBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog: Large File Warning Alert
    largeFileWarningFile?.let { targetFile ->
        AlertDialog(
            onDismissRequest = { largeFileWarningFile = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SleekFolderText) },
            title = { Text("Large File Warning", fontWeight = FontWeight.Bold, color = SleekTextMain) },
            text = {
                val sizeOnMb = String.format("%.2f", targetFile.length().toFloat() / (1024 * 1024))
                Text(
                    "The file \"${targetFile.name}\" is very large (${sizeOnMb} MB).\n\n" +
                    "Opening huge text files causes memory pressure and makes editor scroll extremely laggy.\n\n" +
                    "Would you like to open it anyway, or inspect via Hex Editor?",
                    color = SleekTextMain
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        largeFileWarningFile = null
                        onOpenFile(targetFile, "editor")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekFolderText)
                ) {
                    Text("Force Open as Text", color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            largeFileWarningFile = null
                            onOpenFile(targetFile, "hex")
                        }
                    ) {
                        Text("Use Hex Inspector", color = SleekFolderText)
                    }
                    TextButton(
                        onClick = { largeFileWarningFile = null }
                    ) {
                        Text("Cancel", color = SleekTextSub)
                    }
                }
            },
            containerColor = SleekBg,
            shape = RoundedCornerShape(16.dp)
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
                text = if (dir.absolutePath == root.absolutePath) androidx.compose.ui.res.stringResource(com.example.R.string.btn_root) else dir.name,
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
        val iconPainter = when {
            file.isDirectory -> Triple(SleekFolderBg, SleekFolderText, androidx.compose.material.icons.Icons.Default.Folder.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) })
            ext in listOf("kt", "kts", "java", "js", "py", "css", "xml", "json", "md", "txt") -> Triple(SleekCodeBg, SleekCodeText, androidx.compose.material.icons.Icons.Default.IntegrationInstructions.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) })
            ext in listOf("png", "jpg", "jpeg") -> Triple(SleekImageBg, SleekImageText, androidx.compose.material.icons.Icons.Default.Image.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) })
            ext == "svg" -> Triple(SleekImageBg, SleekImageText, androidx.compose.material.icons.Icons.Default.Palette.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) })
            ext == "apk" -> Triple(SleekZipBg, SleekZipText, androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_apk_custom))
            ext in listOf("zip", "mcpack", "mcworld", "mctemplate", "mcaddon") -> Triple(SleekZipBg, SleekZipText, androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_minecraft_zip))
            ext in listOf("mp3", "wav", "m4a", "ogg", "flac", "mp4", "mkv") -> Triple(SleekAudioBg, SleekAudioText, androidx.compose.material.icons.Icons.Default.LibraryMusic.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) })
            else -> Triple(SleekOtherBg, SleekOtherText, androidx.compose.material.icons.Icons.Default.InsertDriveFile.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) })
        }

        val bgCol = iconPainter.first
        val txtCol = iconPainter.second
        val painter = iconPainter.third

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(bgCol, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter,
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
            ext in listOf("zip", "mcpack", "mcworld", "mctemplate", "mcaddon") -> "Archive"
            ext in listOf("mp3", "wav", "m4a", "ogg", "flac", "mp4", "mkv") -> "Media"
            ext == "svg" -> "SVG"
            ext in listOf("png", "jpg", "jpeg") -> "Image"
            ext in listOf("kt", "java", "js") -> "Kotlin"
            ext == "apk" -> "APK"
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

@Composable
fun ToolLaunchCard(
    title: String,
    desc: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBg, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = SleekTextMain, fontSize = 14.sp)
                Text(text = desc, color = SleekTextSub, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SleekTextSub)
        }
    }
}

// Helpers for recents tracking
fun addRecentFile(context: android.content.Context, file: File) {
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val paths = prefs.getString("recent_files", "")?.split("|")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
    paths.remove(file.absolutePath)
    paths.add(0, file.absolutePath)
    if (paths.size > 15) {
        paths.removeAt(paths.size - 1)
    }
    prefs.edit().putString("recent_files", paths.joinToString("|")).apply()
}

fun getRecentFiles(context: android.content.Context): List<File> {
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    val paths = prefs.getString("recent_files", "")?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
    return paths.map { File(it) }.filter { it.exists() }
}

fun isBinaryFile(file: File): Boolean {
    if (!file.exists() || file.isDirectory) return false
    val extension = file.extension.lowercase(Locale.ROOT)
    
    // List of definitely text extensions
    val textExts = listOf(
        "kt", "kts", "java", "js", "py", "css", "xml", "json", "md", "txt", "svg", 
        "html", "htm", "log", "gradle", "properties", "toml", "yaml", "yml", "ini", "conf", "sh", "bat"
    )
    if (extension in textExts) {
        return false
    }
    
    // Check known binary extensions
    val binaryExts = listOf(
        "mp4", "mkv", "avi", "mov", "wmv", "3gp", "flv", "webm",
        "mp3", "wav", "m4a", "ogg", "flac", "aac",
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico",
        "zip", "tar", "gz", "rar", "7z", "apk", "jar", "class",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "bin", "hex", "exe", "dll", "so", "o", "a", "db", "sqlite",
        "mcpack", "mcworld", "mctemplate", "mcaddon"
    )
    if (extension in binaryExts) return true
    
    // Fallback: Check first 1024 bytes for null bytes or if file is > 500KB
    if (file.length() > 500 * 1024) return true
    try {
        file.inputStream().use { input ->
            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)
            for (i in 0 until bytesRead) {
                if (buffer[i] == 0.toByte()) {
                    return true
                }
            }
        }
    } catch (_: Exception) {}
    return false
}

fun installApk(context: android.content.Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error launching installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

fun downloadFileToPublicDownloads(context: android.content.Context, file: File) {
    try {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val targetFile = File(downloadsDir, file.name)
        file.inputStream().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Toast.makeText(context, "Downloaded to public Downloads: ${targetFile.absolutePath}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        // Fallback: Trigger share intent
        try {
            val authority = "${context.packageName}.fileprovider"
            val fileUri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share / Save File"))
        } catch (ex: Exception) {
            Toast.makeText(context, "Action failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
