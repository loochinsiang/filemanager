package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import java.io.File

// Screen state flow router
sealed class ScreenState {
    object Main : ScreenState()
    data class CodeEditor(val file: File) : ScreenState()
    data class ZipViewer(val file: File) : ScreenState()
    data class ImageViewer(val file: File) : ScreenState()
    data class MusicPlayer(val file: File) : ScreenState()
    data class HexEditor(val file: File) : ScreenState()
}

class MainActivity : ComponentActivity() {
    private val hasStoragePermissionVal = mutableStateOf(false)

    override fun attachBaseContext(newBase: android.content.Context) {
        val localeManager = com.example.ui.components.LocaleManager.getInstance(newBase)
        super.attachBaseContext(localeManager.applyLocaleToContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                
                val isPermissionGranted by hasStoragePermissionVal
                var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Main) }
                
                // Preserve file explorer's directory state across screen switches (e.g., when back handling)
                var currentDirectory by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }

                // Track first-time Welcome screen dismiss state
                val sharedPrefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
                var showWelcomeScreen by remember { mutableStateOf(sharedPrefs.getBoolean("show_welcome", true)) }

                // System back-button integrations
                if (currentScreen != ScreenState.Main) {
                    BackHandler {
                        currentScreen = ScreenState.Main
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (showWelcomeScreen) {
                        WelcomeScreen(
                            hasPermission = isPermissionGranted,
                            onGrantPermission = { requestStorageAccess() },
                            onDismiss = {
                                sharedPrefs.edit().putBoolean("show_welcome", false).apply()
                                showWelcomeScreen = false
                            }
                        )
                    } else {
                        // Animated transition utilizing Material Design 3 Expressive sliding crossfade and spring physics
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                val isGoingBack = targetState is ScreenState.Main
                                if (isGoingBack) {
                                    (slideInHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f)) { -it / 3 } + fadeIn(animationSpec = tween(180)))
                                        .togetherWith(
                                            slideOutHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f)) { it } + fadeOut(animationSpec = tween(150))
                                        )
                                } else {
                                    (slideInHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f)) { it } + fadeIn(animationSpec = tween(180)))
                                        .togetherWith(
                                            slideOutHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f)) { -it / 3 } + fadeOut(animationSpec = tween(150))
                                        )
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            label = "panel_swap"
                        ) { targetScreen ->
                            when (targetScreen) {
                                is ScreenState.Main -> {
                                    MainFileManagerScreen(
                                        phoneRoot = Environment.getExternalStorageDirectory(),
                                        currentDirectory = currentDirectory,
                                        onDirectoryChange = { currentDirectory = it },
                                        hasStoragePermission = isPermissionGranted,
                                        onRequestStoragePermission = { requestStorageAccess() },
                                        onOpenFile = { targetFile, mode ->
                                            currentScreen = when (mode) {
                                                "editor" -> ScreenState.CodeEditor(targetFile)
                                                "zip" -> ScreenState.ZipViewer(targetFile)
                                                "image" -> ScreenState.ImageViewer(targetFile)
                                                "sound" -> ScreenState.MusicPlayer(targetFile)
                                                "hex" -> ScreenState.HexEditor(targetFile)
                                                else -> ScreenState.CodeEditor(targetFile)
                                            }
                                        }
                                    )
                                }
                                is ScreenState.CodeEditor -> {
                                    CodeEditorScreen(
                                        file = targetScreen.file,
                                        onBack = { currentScreen = ScreenState.Main }
                                    )
                                }
                                is ScreenState.ZipViewer -> {
                                    ZipViewerScreen(
                                        file = targetScreen.file,
                                        onBack = { currentScreen = ScreenState.Main },
                                        onNavigateToExtracted = { extractedDir ->
                                            // Navigate file explorer itself into the extracted result folder!
                                            currentDirectory = extractedDir
                                            currentScreen = ScreenState.Main
                                        }
                                    )
                                }
                            is ScreenState.ImageViewer -> {
                                ImageViewerScreen(
                                    file = targetScreen.file,
                                    onBack = { currentScreen = ScreenState.Main }
                                )
                            }
                            is ScreenState.MusicPlayer -> {
                                MusicPlayerScreen(
                                    file = targetScreen.file,
                                    onBack = { currentScreen = ScreenState.Main }
                                )
                            }
                            is ScreenState.HexEditor -> {
                                HexEditorScreen(
                                    file = targetScreen.file,
                                    onBack = { currentScreen = ScreenState.Main }
                                )
                            }
                        }
                    }
                    
                    // Mini Player overlay for audio
                    val isPlaying by com.example.AudioPlayerManager.isPlaying
                    val progress by com.example.AudioPlayerManager.progress
                    val playingFile = com.example.AudioPlayerManager.currentFile.value
                    if (currentScreen !is ScreenState.MusicPlayer && playingFile != null) {
                        val insetsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .padding(bottom = insetsPadding),
                            contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                        ) {
                            com.example.ui.components.MiniAudioPlayer(
                                file = playingFile,
                                isPlaying = isPlaying,
                                progress = progress,
                                onClose = { com.example.AudioPlayerManager.fadeOutAndStop() },
                                onPlayPause = { com.example.AudioPlayerManager.togglePlay() },
                                onClick = { currentScreen = ScreenState.MusicPlayer(playingFile) },
                                onNext = { com.example.AudioPlayerManager.player?.seekTo((com.example.AudioPlayerManager.player?.currentPosition ?: 0) + 10000) },
                                onPrev = { com.example.AudioPlayerManager.player?.seekTo(0L.coerceAtLeast((com.example.AudioPlayerManager.player?.currentPosition ?: 0) - 10000)) }
                            )
                        }
                    }
                    
                } // ends else block
            }
        }
    }
}

    override fun onResume() {
        super.onResume()
        hasStoragePermissionVal.value = checkStoragePermission()
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${packageName}")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                101
            )
        }
    }
}
