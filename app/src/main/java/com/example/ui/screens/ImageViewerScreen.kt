package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    file: File,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val dimensions = remember(file) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                "${options.outWidth} x ${options.outHeight}"
            } else {
                "Unknown Dimensions"
            }
        } catch (_: Exception) {
            "Unknown Dimensions"
        }
    }

    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange
        if (scale == 1f) offset = Offset.Zero
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = SleekTextMain, fontWeight = FontWeight.Bold)
                        Text("PORTRAIT & VECTOR VIEWER", style = MaterialTheme.typography.labelSmall, color = SleekFolderText, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", tint = SleekTextMain)
                    }
                },
                actions = {
                    IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate Image", tint = SleekTextMain)
                    }
                    IconButton(onClick = {
                        scale = 1f
                        offset = Offset.Zero
                        rotation = 0f
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom/Rotation", tint = SleekTextMain)
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
                .background(SleekBg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .background(Color.Black, shape = RoundedCornerShape(24.dp))
                    .transformable(state = state),
                contentAlignment = Alignment.Center
            ) {
                val painter = rememberAsyncImagePainter(file)
                Image(
                    painter = painter,
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            rotationZ = rotation,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.subtitle_image_quality), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SleekFolderText)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Resolution Size", style = MaterialTheme.typography.bodyMedium, color = SleekTextSub)
                        Text(dimensions, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SleekTextMain)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Size on Storage", style = MaterialTheme.typography.bodyMedium, color = SleekTextSub)
                        Text("${file.length() / 1024} KB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SleekTextMain)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("File Format", style = MaterialTheme.typography.bodyMedium, color = SleekTextSub)
                        Text(file.extension.uppercase(Locale.ROOT), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SleekTextMain)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Last Updated", style = MaterialTheme.typography.bodyMedium, color = SleekTextSub)
                        val date = Date(file.lastModified())
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        Text(format.format(date), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SleekTextMain)
                    }
                }
            }
        }
    }
}
