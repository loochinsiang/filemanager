package com.example.ui.screens

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontViewerScreen(
    file: File,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var previewText by remember { mutableStateOf("MT Manager") }
    var editTempText by remember { mutableStateOf(previewText) }

    val customFontFamily = remember(file) {
        try {
            if (file.exists() && file.isFile) {
                FontFamily(Typeface.createFromFile(file))
            } else {
                FontFamily.Default
            }
        } catch (e: Exception) {
            FontFamily.Default
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Font Viewer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextMain
                        )
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekTextSub,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back",
                            tint = SleekTextMain
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Font Information",
                            tint = SleekTextMain
                        )
                    }
                    IconButton(onClick = { 
                        editTempText = previewText
                        showEditDialog = true 
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Preview Text",
                            tint = SleekTextMain
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main glyph preview blocks
            Text(
                text = "abcdefghijklmnopqrstuvwxyz",
                fontFamily = customFontFamily,
                fontSize = 24.sp,
                color = Color.Black
            )

            Text(
                text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                fontFamily = customFontFamily,
                fontSize = 24.sp,
                color = Color.Black
            )

            Text(
                text = "01234567890123456789",
                fontFamily = customFontFamily,
                fontSize = 24.sp,
                color = Color.Black
            )

            Text(
                text = " . : , ; ' \" ! ? + - * / % = ~ |",
                fontFamily = customFontFamily,
                fontSize = 24.sp,
                color = Color.Black
            )

            Text(
                text = "@ # $ _ ^ \\ & < > ( ) [ ] { }",
                fontFamily = customFontFamily,
                fontSize = 24.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-scale custom font sizes preview
            val previewSizes = listOf(12, 18, 24, 36, 48, 60)
            previewSizes.forEach { size ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "$size ",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .width(28.dp)
                            .padding(top = 4.dp)
                    )
                    Text(
                        text = previewText,
                        fontFamily = customFontFamily,
                        fontSize = size.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Dialog: Font Info
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Font File Details", fontWeight = FontWeight.Bold, color = SleekTextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: ${file.name}", fontWeight = FontWeight.Bold, color = SleekTextMain)
                    Text("Format: ${file.extension.uppercase()}", color = SleekTextAlt)
                    Text("Absolute Path:", fontSize = 11.sp, color = SleekTextSub)
                    Text(file.absolutePath, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SleekTextAlt)
                    Text("Size: ${file.length() / 1024} KB (${file.length()} bytes)", color = SleekTextAlt)
                    val date = Date(file.lastModified())
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    Text("Last modified: ${format.format(date)}", color = SleekTextAlt)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close", color = SleekFolderText)
                }
            }
        )
    }

    // Dialog: Edit Preview
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Preview Text", fontWeight = FontWeight.Bold, color = SleekTextMain) },
            text = {
                OutlinedTextField(
                    value = editTempText,
                    onValueChange = { editTempText = it },
                    label = { Text("Preview Sample Text") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekFolderText,
                        focusedLabelColor = SleekFolderText
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTempText.isNotBlank()) {
                            previewText = editTempText
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = SleekTextSub)
                }
            }
        )
    }
}
