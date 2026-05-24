package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    hasPermission: Boolean,
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = SleekBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(36.dp))

            // Brand icon with circular gradient background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(SleekFolderBg, Color.White)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(2.dp, SleekFolderText, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = SleekFolderText,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Welcome to FileSmith",
                fontSize = 28.sp,
                color = SleekTextMain,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Expressive Developer File Suite",
                fontSize = 14.sp,
                color = SleekFolderText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "A lightweight, high-performance toolkit designed specifically for files, code editing, media rendering, and low-level hex diagnostics directly on your device.",
                fontSize = 13.sp,
                color = SleekTextSub,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Features Card Grid/Column
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WelcomeFeatureItem(
                        icon = Icons.Default.Code,
                        iconColor = SleekCodeText,
                        iconBg = SleekCodeBg,
                        title = "Code Syntax Studio",
                        desc = "View and write multiple programming languages with real-time highlighting and scale sizing."
                    )

                    WelcomeFeatureItem(
                        icon = Icons.Default.MusicNote,
                        iconColor = SleekAudioText,
                        iconBg = SleekAudioBg,
                        title = "Music & Studio Deck",
                        desc = "Analyze authentic waveforms, utilize 5-band EQ controls, adjust tempo/pitch, and detect live BPM."
                    )

                    WelcomeFeatureItem(
                        icon = Icons.Default.Palette,
                        iconColor = SleekImageText,
                        iconBg = SleekImageBg,
                        title = "SVG Live Renderer",
                        desc = "View raw SVG outputs in real-time, fit vectors to viewport, and convert them to Android Vector XML."
                    )

                    WelcomeFeatureItem(
                        icon = Icons.Default.DeveloperMode,
                        iconColor = SleekOtherText,
                        iconBg = SleekOtherBg,
                        title = "Hex & Binary Inspector",
                        desc = "Examine raw byte sequences, modify files, clone resources, and inspect metadata properties."
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // Action section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!hasPermission) {
                    Button(
                        onClick = onGrantPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekFolderText,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.Default.SdStorage, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Grant Storage Access", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Continue with Limited View", color = SleekTextSub, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekFolderText,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WelcomeFeatureItem(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SleekTextMain
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = SleekTextSub,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
