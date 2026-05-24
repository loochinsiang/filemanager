package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipViewerScreen(
    file: File,
    onBack: () -> Unit,
    onNavigateToExtracted: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    var entries by remember { mutableStateOf<List<ZipEntryInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isExtracting by remember { mutableStateOf(false) }
    var extractionRatio by remember { mutableStateOf(0f) }
    var previewEntry by remember { mutableStateOf<Pair<String, String>?>(null) } // Name to contents

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(file) {
        isLoading = true
        try {
            if (file.exists() && file.isFile) {
                ZipFile(file).use { zip ->
                    val list = zip.entries().asSequence().map { entry ->
                        ZipEntryInfo(
                            name = entry.name,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                            isDirectory = entry.isDirectory
                        )
                    }.toList()
                    entries = list
                }
            }
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Failed to open Zip archive: ${e.localizedMessage}")
        } finally {
            isLoading = false
        }
    }

    val extractZip: (Boolean, String?) -> Unit = { extractAll, singleName ->
        isExtracting = true
        extractionRatio = 0.1f
        var success = false
        var targetOutDir: File? = null

        try {
            val parentDir = file.parentFile ?: file
            val outDirName = file.nameWithoutExtension + "_extracted"
            val outputDirectory = File(parentDir, outDirName)
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs()
            }
            targetOutDir = outputDirectory

            ZipFile(file).use { zip ->
                val allEntries = zip.entries().asSequence().toList()
                val totalNumber = allEntries.size
                var extractedCount = 0

                for (entry in allEntries) {
                    if (extractAll || entry.name == singleName) {
                        val destinationFile = File(outputDirectory, entry.name)
                        if (entry.isDirectory) {
                            destinationFile.mkdirs()
                        } else {
                            destinationFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(destinationFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                    extractedCount++
                    extractionRatio = (extractedCount.toFloat() / totalNumber.toFloat())
                }
            }
            success = true
        } catch (e: Exception) {
            success = false
        } finally {
            isExtracting = false
            extractionRatio = 1.0f
            if (success && targetOutDir != null) {
                onNavigateToExtracted(targetOutDir)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = SleekTextMain, fontWeight = FontWeight.Bold)
                        Text("ZIP ARCHIVE EXTRACTOR", style = MaterialTheme.typography.labelSmall, color = SleekFolderText, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", tint = SleekTextMain)
                    }
                },
                actions = {
                    Button(
                        onClick = { extractZip(true, null) },
                        enabled = !isExtracting && entries.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Extract All", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekBg
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(SleekBg)
        ) {
            if (isLoading) {
                com.example.ui.components.LoadingScreen(
                    message = "Reading zip entries..."
                )
            } else if (entries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FolderZip,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = SleekTextSub.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Empty or Corrupted Zip Archive", color = SleekTextSub)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Entries Count", style = MaterialTheme.typography.labelMedium, color = SleekTextSub)
                                    Text("${entries.size} files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SleekTextMain)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Archive Size", style = MaterialTheme.typography.labelMedium, color = SleekTextSub)
                                    Text("${file.length() / 1024} KB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SleekFolderText)
                                }
                            }
                        }
                    }

                    items(entries) { entry ->
                        ZipEntryRow(
                            entry = entry,
                            onPreview = {
                                try {
                                    ZipFile(file).use { zip ->
                                        val rentry = zip.getEntry(entry.name)
                                        if (rentry != null) {
                                            val textContent = zip.getInputStream(rentry).use { input ->
                                                input.bufferedReader().use { it.readText() }
                                            }
                                            previewEntry = Pair(entry.name, textContent)
                                        }
                                    }
                                } catch (_: Exception) {
                                }
                            },
                            onExtract = {
                                extractZip(false, entry.name)
                            }
                        )
                    }
                }
            }

            if (isExtracting) {
                com.example.ui.components.ProgressScreen(
                    progress = extractionRatio,
                    message = "Extracting ZIP archive...",
                    statusText = "Extracting files to device folder"
                )
            }

            previewEntry?.let { (name, text) ->
                AlertDialog(
                    onDismissRequest = { previewEntry = null },
                    title = { Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = SleekTextMain) },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .background(SleekBg, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = text,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = SleekTextAlt
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { previewEntry = null },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText)
                        ) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ZipEntryRow(
    entry: ZipEntryInfo,
    onPreview: () -> Unit,
    onExtract: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (entry.isDirectory) SleekFolderBg else SleekCodeBg,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.isDirectory) Icons.Default.FolderOpen else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (entry.isDirectory) SleekFolderText else SleekCodeText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!entry.isDirectory) {
                    Text(
                        text = "Original: ${entry.size / 1024} KB  |  Compressed: ${entry.compressedSize / 1024} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekTextSub
                    )
                } else {
                    Text("Directory", style = MaterialTheme.typography.labelSmall, color = SleekTextSub)
                }
            }

            if (!entry.isDirectory) {
                val isPreviewable = entry.name.endsWith(".txt", true) ||
                        entry.name.endsWith(".md", true) ||
                        entry.name.endsWith(".json", true) ||
                        entry.name.endsWith(".kt", true) ||
                        entry.name.endsWith(".xml", true)

                if (isPreviewable) {
                    IconButton(onClick = onPreview) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview Entry", tint = SleekFolderText)
                    }
                }

                IconButton(onClick = onExtract) {
                    Icon(Icons.Default.Download, contentDescription = "Extract Single File", tint = SleekCodeText)
                }
            }
        }
    }
}

data class ZipEntryInfo(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean
)
