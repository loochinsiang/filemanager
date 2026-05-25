package com.example.ui.screens

import android.content.res.Configuration
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
    val orientation = LocalConfiguration.current.orientation
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) }
    var currentTimeLabel by remember { mutableStateOf("00:00") }
    var totalTimeLabel by remember { mutableStateOf("00:00") }
    var durationMs by remember { mutableStateOf(0L) }
    var showOverlay by remember { mutableStateOf(true) }

    // Audio effects state
    var pitch by remember { mutableFloatStateOf(1.0f) }
    var tempo by remember { mutableFloatStateOf(1.0f) }
    var enableReverb by remember { mutableStateOf(false) }
    var enableLimiter by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }
    
    var reverbRef by remember { mutableStateOf<PresetReverb?>(null) }
    var limiterRef by remember { mutableStateOf<LoudnessEnhancer?>(null) }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration
                    val m = (durationMs / 1000) / 60
                    val s = (durationMs / 1000) % 60
                    totalTimeLabel = String.format(Locale.ROOT, "%02d:%02d", m, s)
                    
                    // Attach AudioFX
                    try {
                        val sessId = exoPlayer.audioSessionId
                        if (sessId != 0) {
                            reverbRef = PresetReverb(0, sessId).apply {
                                preset = PresetReverb.PRESET_LARGEHALL
                                enabled = enableReverb
                            }
                            limiterRef = LoudnessEnhancer(sessId).apply {
                                setTargetGain(1500)
                                enabled = enableLimiter
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        exoPlayer.addListener(listener)
    }

    LaunchedEffect(pitch, tempo) {
        exoPlayer.playbackParameters = PlaybackParameters(tempo, pitch)
    }

    LaunchedEffect(enableReverb) {
        reverbRef?.enabled = enableReverb
    }

    LaunchedEffect(enableLimiter) {
        limiterRef?.enabled = enableLimiter
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val dur = exoPlayer.duration.toFloat()
            if (dur > 0f) {
                val current = exoPlayer.currentPosition.toFloat()
                currentProgress = current / dur
                val m = (exoPlayer.currentPosition / 1000) / 60
                val s = (exoPlayer.currentPosition / 1000) % 60
                currentTimeLabel = String.format(Locale.ROOT, "%02d:%02d", m, s)
            }
            delay(250)
        }
    }

    // Auto-hide overlay for video
    LaunchedEffect(showOverlay, isPlaying) {
        if (isVideo && showOverlay && isPlaying) {
            delay(3000)
            showOverlay = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { reverbRef?.release() } catch (_: Exception) {}
            try { limiterRef?.release() } catch (_: Exception) {}
            exoPlayer.release()
        }
    }

    val togglePlay = {
        if (isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isVideo) Color.Black else SleekBg
    ) { innerPadding ->
        if (isVideo) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showOverlay = !showOverlay }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                AnimatedVisibility(
                    visible = showOverlay,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        // Top bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Text(
                                file.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White) }
                        }

                        // Center Controls
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0)) },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "Rewind 10", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            
                            IconButton(
                                onClick = togglePlay,
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)) },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(Icons.Default.Forward10, contentDescription = "Forward 10", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }

                        // Bottom Controls
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currentTimeLabel, color = Color.White, fontSize = 12.sp)
                                Slider(
                                    value = currentProgress,
                                    onValueChange = { targetP ->
                                        currentProgress = targetP
                                        exoPlayer.seekTo((targetP * durationMs).toLong())
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = SleekFolderText,
                                        thumbColor = SleekFolderText,
                                        inactiveTrackColor = Color.White.copy(0.3f)
                                    )
                                )
                                Text(totalTimeLabel, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Audio Player
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SleekTextMain)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = SleekTextMain, fontWeight = FontWeight.Bold)
                        Text("AUDIO PLAYER", style = MaterialTheme.typography.labelSmall, color = SleekFolderText, fontWeight = FontWeight.ExtraBold)
                    }
                }

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: icon and progress
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(SleekFolderBg.copy(alpha = 0.2f), shape = RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, tint = SleekFolderText, modifier = Modifier.size(64.dp))
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Slider(
                                value = currentProgress,
                                onValueChange = { targetP ->
                                    currentProgress = targetP
                                    exoPlayer.seekTo((targetP * durationMs).toLong())
                                },
                                colors = SliderDefaults.colors(activeTrackColor = SleekFolderText, thumbColor = SleekFolderText)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentTimeLabel, color = SleekTextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(totalTimeLabel, color = SleekTextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 5000).coerceAtLeast(0)) }) {
                                    Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = SleekTextAlt)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(SleekFolderBg)
                                        .clickable { togglePlay() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = SleekFolderText, modifier = Modifier.size(32.dp))
                                }
                                IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 5000).coerceAtMost(durationMs)) }) {
                                    Icon(Icons.Default.FastForward, contentDescription = "ForwardDelta", tint = SleekTextAlt)
                                }
                            }
                        }
                        
                        // Right side: controls
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pitch (${String.format(Locale.US, "%.1f", pitch)}x)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = SleekTextMain)
                                Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..2.0f, modifier = Modifier.weight(2f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tempo (${String.format(Locale.US, "%.1f", tempo)}x)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = SleekTextMain)
                                Slider(value = tempo, onValueChange = { tempo = it }, valueRange = 0.5f..2.0f, modifier = Modifier.weight(2f))
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Switch(checked = enableReverb, onCheckedChange = { enableReverb = it }, colors = SwitchDefaults.colors(checkedThumbColor = SleekFolderText, checkedTrackColor = SleekFolderBg))
                                    Text("REVERB", fontSize = 10.sp, color = SleekTextSub, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Switch(checked = enableLimiter, onCheckedChange = { enableLimiter = it }, colors = SwitchDefaults.colors(checkedThumbColor = SleekFolderText, checkedTrackColor = SleekFolderBg))
                                    Text("LIMITER", fontSize = 10.sp, color = SleekTextSub, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Portrait Audio UI
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(32.dp))
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .background(SleekFolderBg.copy(alpha = 0.2f), shape = RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = SleekFolderText, modifier = Modifier.size(80.dp))
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
                            Slider(
                                value = currentProgress,
                                onValueChange = { targetP ->
                                    currentProgress = targetP
                                    exoPlayer.seekTo((targetP * durationMs).toLong())
                                },
                                colors = SliderDefaults.colors(activeTrackColor = SleekFolderText, thumbColor = SleekFolderText)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentTimeLabel, color = SleekTextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(totalTimeLabel, color = SleekTextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 5000).coerceAtLeast(0)) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = SleekTextAlt, modifier = Modifier.size(28.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(SleekFolderBg)
                                    .clickable { togglePlay() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = SleekFolderText, modifier = Modifier.size(36.dp))
                            }
                            IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 5000).coerceAtMost(durationMs)) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.FastForward, contentDescription = "ForwardDelta", tint = SleekTextAlt, modifier = Modifier.size(28.dp))
                            }
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Switch(checked = enableReverb, onCheckedChange = { enableReverb = it }, colors = SwitchDefaults.colors(checkedThumbColor = SleekFolderText, checkedTrackColor = SleekFolderBg))
                                Spacer(Modifier.height(8.dp))
                                Text("REVERB", fontSize = 10.sp, color = SleekTextSub, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Switch(checked = enableLimiter, onCheckedChange = { enableLimiter = it }, colors = SwitchDefaults.colors(checkedThumbColor = SleekFolderText, checkedTrackColor = SleekFolderBg))
                                Spacer(Modifier.height(8.dp))
                                Text("LIMITER", fontSize = 10.sp, color = SleekTextSub, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pitch (${String.format(Locale.US, "%.1f", pitch)}x)", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextMain)
                                Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..2.0f, modifier = Modifier.weight(2f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tempo (${String.format(Locale.US, "%.1f", tempo)}x)", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextMain)
                                Slider(value = tempo, onValueChange = { tempo = it }, valueRange = 0.5f..2.0f, modifier = Modifier.weight(2f))
                            }
                        }
                    }
                }
            }
        }
    }
}
