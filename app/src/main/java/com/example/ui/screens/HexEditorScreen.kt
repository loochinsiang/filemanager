package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HexEditorScreen(
    file: File,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rawBytes by remember { mutableStateOf<ByteArray>(ByteArray(0)) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var editHexValue by remember { mutableStateOf("") }
    var successSaveMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        if (file.exists() && file.isFile) {
            rawBytes = file.readBytes()
        }
    }

    val saveChanges = {
        try {
            file.writeBytes(rawBytes)
            successSaveMessage = "Changes saved standard compliance"
        } catch (_: Exception) {
            successSaveMessage = "Failed writing binary bytes"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = SleekTextMain, fontWeight = FontWeight.Bold)
                        Text("BINARY HEX INSPECTOR & EDITOR", style = MaterialTheme.typography.labelSmall, color = SleekFolderText, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", tint = SleekTextMain)
                    }
                },
                actions = {
                    IconButton(onClick = saveChanges) {
                        Icon(Icons.Default.Save, contentDescription = "Save Binary Changes", tint = SleekTextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekBg
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(SleekBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekBottomNavBg)
                    .border(1.dp, SleekBorderLight)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("OFFSET", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SleekTextAlt, fontWeight = FontWeight.Bold))
                Text("00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SleekTextAlt, fontWeight = FontWeight.Bold))
                Text("ASCII", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SleekTextAlt, fontWeight = FontWeight.Bold), modifier = Modifier.width(55.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                val itemsCount = (rawBytes.size + 15) / 16
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(itemsCount) { rowIndex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val offsetHex = String.format(Locale.ROOT, "%08X", rowIndex * 16)
                            Text(
                                text = offsetHex,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = SleekCodeText,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(start = 8.dp, end = 12.dp)
                            )

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (colIndex in 0..15) {
                                    val byteIndex = rowIndex * 16 + colIndex
                                    if (byteIndex < rawBytes.size) {
                                        val b = rawBytes[byteIndex]
                                        val byteHex = String.format(Locale.ROOT, "%02X", b)
                                        val isSelected = selectedIndex == byteIndex

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) SleekFolderBg
                                                    else Color.Transparent,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) SleekFolderText
                                                    else Color.Transparent,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .clickable {
                                                    selectedIndex = byteIndex
                                                    editHexValue = byteHex
                                                }
                                                .padding(horizontal = 2.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = byteHex,
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) SleekFolderText
                                                    else SleekTextMain,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(18.dp))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .width(55.dp)
                                    .padding(end = 6.dp)
                            ) {
                                for (colIndex in 0..15) {
                                    val byteIndex = rowIndex * 16 + colIndex
                                    if (byteIndex < rawBytes.size) {
                                        val b = rawBytes[byteIndex]
                                        val charVal = if (b >= 32 && b <= 126) b.toInt().toChar().toString() else "•"
                                        Text(
                                            text = charVal,
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = SleekTextSub
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Modify byte card
            selectedIndex?.let { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Offset: 0x${String.format(Locale.ROOT, "%08X", index)}",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextMain
                                )
                                val currentVal = rawBytes[index]
                                Text(
                                    text = "Dec: ${currentVal.toInt()}  |  Unsigned: ${currentVal.toInt() and 0xFF}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SleekTextSub
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editHexValue,
                                    onValueChange = { newVal ->
                                        if (newVal.length <= 2 && newVal.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                                            editHexValue = newVal.uppercase(Locale.ROOT)
                                            if (newVal.length == 2) {
                                                try {
                                                    val byteVal = newVal.toInt(16).toByte()
                                                    val temp = rawBytes.clone()
                                                    temp[index] = byteVal
                                                    rawBytes = temp
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    },
                                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                                    modifier = Modifier.width(76.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SleekFolderText,
                                        unfocusedBorderColor = SleekBorderLight
                                    )
                                )

                                Button(
                                    onClick = { selectedIndex = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Done", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            successSaveMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { successSaveMessage = null },
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = SleekFolderText) },
                    text = { Text(msg, color = SleekTextMain) },
                    confirmButton = {
                        Button(
                            onClick = { successSaveMessage = null },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText)
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}
