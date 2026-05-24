package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) }
    var currentTimeLabel by remember { mutableStateOf("00:00") }
    var totalTimeLabel by remember { mutableStateOf("00:00") }

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

    val infiniteTransition = rememberInfiniteTransition(label = "music_pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = SleekTextMain, fontWeight = FontWeight.Bold)
                        Text("HD AUDIO DECODER", style = MaterialTheme.typography.labelSmall, color = SleekFolderText, fontWeight = FontWeight.ExtraBold)
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
                .background(SleekBg)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .background(Color.White, shape = RoundedCornerShape(24.dp))
                    .border(1.dp, SleekBorderLight, shape = RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val barsCount = 28
                    val barWidth = size.width / (barsCount * 1.5f)
                    val space = barWidth * 0.5f

                    for (i in 0 until barsCount) {
                        val amplitude = if (isPlaying) {
                            val freqMultiplier = 3.0f + (i % 5)
                            val offsetVal = (i * 0.2f)
                            (kotlin.math.sin(phase + offsetVal) * 35f + 45f) * freqMultiplier / 3f
                        } else {
                            5f + (kotlin.math.sin(phase + i * 0.1f) * 4f)
                        }

                        val h = amplitude.coerceIn(4f, size.height - 40f)
                        val x = i * (barWidth + space) + space
                        val y = (size.height - h) / 2f

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(SleekFolderText, SleekCodeText)
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(SleekFolderBg.copy(alpha = 0.9f))
                        .border(1.dp, SleekFolderText, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = SleekFolderText,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = file.nameWithoutExtension.replace("_", " "),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SleekTextMain,
                    maxLines = 1,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "HD Stereo Audio  |  ${file.length() / 1024} KB",
                    fontSize = 13.sp,
                    color = SleekTextSub,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Slider(
                    value = currentProgress,
                    onValueChange = { targetProgress ->
                        currentProgress = targetProgress
                        mediaPlayer?.let { player ->
                            val targetMs = (targetProgress * player.duration).toInt()
                            player.seekTo(targetMs)
                        }
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
                    Text(currentTimeLabel, color = SleekTextSub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(totalTimeLabel, color = SleekTextSub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                IconButton(
                    onClick = {
                        mediaPlayer?.let { player ->
                            val target = (player.currentPosition - 5000).coerceAtLeast(0)
                            player.seekTo(target)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = SleekTextAlt, modifier = Modifier.size(28.dp))
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SleekFolderBg)
                        .clickable {
                            mediaPlayer?.let { player ->
                                if (player.isPlaying) {
                                    player.pause()
                                    isPlaying = false
                                } else {
                                    player.start()
                                    isPlaying = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = SleekFolderText,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = {
                        mediaPlayer?.let { player ->
                            val target = (player.currentPosition + 5000).coerceAtMost(player.duration)
                            player.seekTo(target)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "ForwardDelta", tint = SleekTextAlt, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
