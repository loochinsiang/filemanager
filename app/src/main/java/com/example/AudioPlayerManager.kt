package com.example

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import java.io.File

object AudioPlayerManager {
    var player: ExoPlayer? = null
    var currentFile = mutableStateOf<File?>(null)
    var isPlaying = mutableStateOf(false)
    var progress = mutableStateOf(0f)
    private var progressJob: Job? = null

    fun initialize(context: Context) {
        if (player == null) {
            player = ExoPlayer.Builder(context.applicationContext).build()
            player?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                    isPlaying.value = isPlayingChange
                    if (isPlayingChange) {
                        startProgressTracker()
                    } else {
                        progressJob?.cancel()
                    }
                }
            })
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val dur = player?.duration?.toFloat() ?: 1f
                val pos = player?.currentPosition?.toFloat() ?: 0f
                if (dur > 0) {
                    progress.value = pos / dur
                }
                delay(1000)
            }
        }
    }

    fun playFile(file: File) {
        if (currentFile.value?.absolutePath != file.absolutePath) {
            currentFile.value = file
            player?.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
            player?.prepare()
        }
        player?.playWhenReady = true
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    fun togglePlay() {
        if (isPlaying.value) pause() else resume()
    }

    fun stop() {
        player?.stop()
        currentFile.value = null
        isPlaying.value = false
    }

    fun release() {
        player?.release()
        player = null
        currentFile.value = null
        isPlaying.value = false
    }
}
