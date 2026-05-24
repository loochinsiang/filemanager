package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    file: File,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVideo = file.extension.lowercase(Locale.ROOT) in listOf("mp4", "mkv", "avi", "webm", "mov")
    
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) }
    var currentTimeLabel by remember { mutableStateOf("00:00") }
    var totalTimeLabel by remember { mutableStateOf("00:00") }

    if (!isVideo) {
        LaunchedEffect(file) {
            try {
                val player = MediaPlayer.create(context, Uri.fromFile(file))
                if (player != null) {
                    mediaPlayer = player
                    val durationMs = player.duration
                    val m = (durationMs / 1000) / 60
                    val s = (durationMs / 1000) % 60
                    totalTimeLabel = String.format(Locale.ROOT, "%02d:%02d", m, s)
                }
            } catch (_: Exception) {}
        }
    
        LaunchedEffect(isPlaying) {
            while (isPlaying) {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            val duration = player.duration.toFloat()
                            if (duration > 0f) {
                                val current = player.currentPosition.toFloat()
                                currentProgress = current / duration
                                val m = (player.currentPosition / 1000) / 60
                                val s = (player.currentPosition / 1000) % 60
                                currentTimeLabel = String.format(Locale.ROOT, "%02d:%02d", m, s)
                            }
                        } else {
                            isPlaying = false
                        }
                    }
                } catch (_: Exception) {
                    isPlaying = false
                }
                delay(250)
            }
        }
    
        DisposableEffect(Unit) {
            onDispose {
                mediaPlayer?.let {
                    try {
                        if (it.isPlaying) {
                            it.stop()
                        }
                    } catch (_: Exception) {}
                    it.release()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = SleekTextMain, fontWeight = FontWeight.Bold)
                        Text(if (isVideo) "VIDEO PLAYER" else "AUDIO PLAYER", style = MaterialTheme.typography.labelSmall, color = SleekFolderText, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        try {
                            mediaPlayer?.stop()
                        } catch (_: Exception) {}
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", tint = SleekTextMain)
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isVideo) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.fromFile(file))
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setOnPreparedListener { start() }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Spacer(Modifier.height(40.dp))
                // Visual Deck Icon Card
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(SleekFolderBg.copy(alpha = 0.2f), shape = RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = SleekFolderText,
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(Modifier.height(40.dp))
    
                // Title
                Text(
                    text = file.nameWithoutExtension.replace("_", " "),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SleekTextMain,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                
                Spacer(Modifier.height(30.dp))
    
                // Progress tracking seek slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                ) {
                    Slider(
                        value = currentProgress,
                        onValueChange = { targetProgress ->
                            currentProgress = targetProgress
                            try {
                                mediaPlayer?.let { player ->
                                    val targetMs = (targetProgress * player.duration).toInt()
                                    player.seekTo(targetMs)
                                }
                            } catch (_: Exception) {}
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = SleekFolderText,
                            inactiveTrackColor = SleekOtherBg,
                            thumbColor = SleekFolderText
                        )
                    )
    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(currentTimeLabel, color = SleekTextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(totalTimeLabel, color = SleekTextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(20.dp))
    
                // Transport Actions Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            try {
                                mediaPlayer?.let { player ->
                                    val target = (player.currentPosition - 5000).coerceAtLeast(0)
                                    player.seekTo(target)
                                }
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = SleekTextAlt, modifier = Modifier.size(28.dp))
                    }
    
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SleekFolderBg)
                            .clickable {
                                try {
                                    mediaPlayer?.let { player ->
                                        if (player.isPlaying) {
                                            player.pause()
                                            isPlaying = false
                                        } else {
                                            player.start()
                                            isPlaying = true
                                        }
                                    }
                                } catch (_: Exception) {
                                    isPlaying = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = SleekFolderText,
                            modifier = Modifier.size(36.dp)
                        )
                    }
    
                    IconButton(
                        onClick = {
                            try {
                                mediaPlayer?.let { player ->
                                    val target = (player.currentPosition + 5000).coerceAtMost(player.duration)
                                    player.seekTo(target)
                                }
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "ForwardDelta", tint = SleekTextAlt, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}
