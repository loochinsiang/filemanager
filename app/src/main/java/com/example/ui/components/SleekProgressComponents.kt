@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoadingScreen(
    message: String = "Loading, please wait...",
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Material 3 high-fidelity LoadingIndicator
            LoadingIndicator(
                color = SleekFolderText,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = message,
                color = SleekTextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Processing files and structures",
                color = SleekTextSub,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            
            if (onCancel != null) {
                Spacer(modifier = Modifier.height(28.dp))
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SleekTextMain
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel Operation", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    progress: Float, // 0f to 1f
    message: String = "Processing task...",
    statusText: String = "",
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp)
        ) {
            Text(
                text = message,
                color = SleekTextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Material 3 LinearWavyProgressIndicator
            // Use property or lambda overload. To be super compatible, we can use the lambda progress block
            LinearWavyProgressIndicator(
                progress = { progress },
                color = SleekCodeText,
                trackColor = SleekBorderLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    color = SleekTextSub,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = SleekCodeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            if (onCancel != null) {
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SleekTextMain
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel Task", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
