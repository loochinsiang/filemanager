package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.FileSandbox
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                
                // Programmatically initialize sandbox assets
                val sandboxRoot = remember {
                    FileSandbox.setupSandbox(context)
                }

                var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Main) }

                // System back-button integrations
                if (currentScreen != ScreenState.Main) {
                    BackHandler {
                        currentScreen = ScreenState.Main
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Animated transition fade for full panels
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        modifier = Modifier.padding(innerPadding),
                        label = "panel_swap"
                    ) { targetScreen ->
                        when (targetScreen) {
                            is ScreenState.Main -> {
                                MainFileManagerScreen(
                                    sandboxRoot = sandboxRoot,
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
                }
            }
        }
    }
}
