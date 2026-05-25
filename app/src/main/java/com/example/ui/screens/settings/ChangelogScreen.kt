@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.example.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class ReleaseInfo(
    val name: String,
    val tagName: String,
    val publishedAt: String,
    val body: String
)

@Composable
fun MarkdownText(
    markdown: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = markdown,
        style = style,
        color = color
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var releases by remember { mutableStateOf<List<ReleaseInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun loadReleases() {
        isLoading = true
        kotlinx.coroutines.delay(600)
        releases = listOf(
            ReleaseInfo(
                name = "v1.3.1 (Current Release)",
                tagName = "v1.3.1",
                publishedAt = "2026-05-25T16:00:00Z",
                body = "• Added gorgeous, fully Adaptive Settings view inspired by the M3 Material system.\n• Integrated intuitive vertical gesture controls for Volume (right-side) and Brightness (left-side) into the media player.\n• Fixed full crash bug when swiping up or down on the mini-audio player panel.\n• Resolved issues with Minecraft format ZIP archives (.mcpack, .mcworld, etc.)."
            ),
            ReleaseInfo(
                name = "v1.2.0 (Theme & Quality)",
                tagName = "v1.2.0",
                publishedAt = "2026-04-10T12:00:00Z",
                body = "• Implemented beautiful custom adaptive app launcher icons.\n• Upgraded ZIP Archive extraction capabilities with path-traversal safety checks.\n• Render native SVG vector layouts instantly inside the text and code editor environments."
            ),
            ReleaseInfo(
                name = "v1.0.0 (First Stable Launch)",
                tagName = "v1.0.0",
                publishedAt = "2026-02-15T09:00:00Z",
                body = "• Initial release of FileSmith Studio!\n• Fast file exploration, folder tree browsing, image visualizers, low-level HEX analyzers, and integrated media playback engines."
            )
        )
        error = null
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadReleases()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changelog") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null && releases.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading changelog",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            coroutineScope.launch {
                                loadReleases()
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                releases.isEmpty() -> {
                    Text(
                        text = "No releases",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        
                        items(releases) { release ->
                            ReleaseCard(release = release)
                        }
                        
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseCard(release: ReleaseInfo) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
    
    val formattedDate = remember(release.publishedAt) {
        try {
            val date = dateFormat.parse(release.publishedAt.substring(0, 10))
            date?.let { displayDateFormat.format(it) } ?: release.publishedAt
        } catch (e: Exception) {
            release.publishedAt
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = release.name.ifBlank { release.tagName },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!release.body.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                MarkdownText(
                    markdown = release.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
