package com.example.ui.components

import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.MusicNote
import com.example.R
import androidx.compose.foundation.basicMarquee

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniAudioPlayer(
    file: File,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    progress: Float = 0f
) {
    val containerBg = Color(0xFFFBEBE8)
    val iconTint = Color(0xFF241414)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(containerBg)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            // Artwork
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(47.dp)
            ) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF9E403B),
                    trackColor = Color(0xFFE5C9C5)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(37.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5C9C5))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE5C9C5).copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = file.nameWithoutExtension,
                    style = MaterialTheme.typography.titleMedium,
                    color = iconTint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = "Audio File",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF755B58),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFE5C9C5), CircleShape)
                        .clickable { onPrev() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fast_rewind_custom),
                        contentDescription = "Prev",
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE5C9C5), CircleShape)
                        .clickable { onPlayPause() }
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.ic_pause_custom else R.drawable.ic_play_custom),
                        contentDescription = "Play/Pause",
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFE5C9C5), CircleShape)
                        .clickable { onNext() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fast_forward_custom),
                        contentDescription = "Next",
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}
