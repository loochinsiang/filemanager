package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utils.SvgConverter
import com.example.utils.SvgElement
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    file: File,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rawText by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("kotlin") }
    var scaleFactor by remember { mutableStateOf(14.sp) }
    var isSaving by remember { mutableStateOf(false) }
    var showPreviewTab by remember { mutableStateOf(false) } // SVG Live Preview & XML Converter Toggle
    var convertedXmlText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Initialize rawText and language
    LaunchedEffect(file) {
        if (file.exists() && file.isFile) {
            rawText = file.readText()
            language = when (file.extension.lowercase()) {
                "kt", "kts" -> "kotlin"
                "java" -> "java"
                "js" -> "javascript"
                "py" -> "python"
                "css" -> "css"
                "svg" -> "svg"
                "xml" -> "xml"
                "json" -> "json"
                "md" -> "markdown"
                else -> "text"
            }
            if (language == "svg") {
                convertedXmlText = SvgConverter.svgToXml(rawText)
            }
        }
    }

    LaunchedEffect(rawText, language) {
        if (language == "svg") {
            try {
                convertedXmlText = SvgConverter.svgToXml(rawText)
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = SleekTextMain, fontWeight = FontWeight.Bold)
                        Text(language.uppercase(), style = MaterialTheme.typography.labelSmall, color = SleekFolderText, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back", tint = SleekTextMain)
                    }
                },
                actions = {
                    IconButton(onClick = { if (scaleFactor.value > 10f) scaleFactor = (scaleFactor.value - 2).sp }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out Font", tint = SleekTextMain)
                    }
                    IconButton(onClick = { if (scaleFactor.value < 28f) scaleFactor = (scaleFactor.value + 2).sp }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In Font", tint = SleekTextMain)
                    }
                    IconButton(onClick = {
                        isSaving = true
                        file.writeText(rawText)
                        isSaving = false
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Code Changes", tint = SleekTextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekBg
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(SleekBg)
        ) {
            // Screen toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(SleekBottomNavBg, RoundedCornerShape(12.dp))
                    .border(1.dp, SleekBorderLight, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Code / SVG Tab
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (!showPreviewTab) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (!showPreviewTab) 1.dp else 0.dp,
                                color = if (!showPreviewTab) SleekBorderLight else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { showPreviewTab = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Source Code",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (!showPreviewTab) SleekFolderText else SleekTextSub
                        )
                    }

                    if (language == "svg" || file.extension.lowercase() == "svg") {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (showPreviewTab) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (showPreviewTab) 1.dp else 0.dp,
                                    color = if (showPreviewTab) SleekBorderLight else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { showPreviewTab = true }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (showPreviewTab) SleekFolderText else SleekTextSub
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "SVG Live View",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (showPreviewTab) SleekFolderText else SleekTextSub
                                )
                            }
                        }
                    }
                }
            }

            if (!showPreviewTab) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13111A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorderLight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val scrollState = rememberScrollState()
                        val horizontalScrollState = rememberScrollState()
                        val lineCount = rawText.count { it == '\n' } + 1

                        // Line numbers
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(46.dp)
                                .verticalScroll(scrollState)
                                .background(Color(0xFF0C0B10))
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = i.toString(),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = scaleFactor,
                                        color = Color.Gray.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        // Code Edit area
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .horizontalScroll(horizontalScrollState)
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = rawText,
                                onValueChange = { rawText = it },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = scaleFactor,
                                    color = Color(0xFFE2E1E6)
                                ),
                                cursorBrush = SolidColor(Color.White),
                                visualTransformation = CodeSyntaxTransformation(language),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "LIVE VECTOR RENDERING", 
                        style = MaterialTheme.typography.labelMedium,
                        color = SleekFolderText,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .border(1.dp, SleekBorderLight, RoundedCornerShape(24.dp))
                            .background(Color.White, RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val elements = remember(rawText) { SvgConverter.parseSvgForPreview(rawText) }

                        // Parse viewBox dimensions to fit any coordinate space into viewport canvas bounds
                        val viewBoxDimensions = remember(rawText) {
                            val viewBoxRegex = Regex("viewBox\\s*=\\s*\"([^\"]*)\"|viewBox\\s*=\\s*'([^']*)'")
                            val match = viewBoxRegex.find(rawText)
                            val cleanedAttr = match?.groupValues?.get(1)?.ifEmpty { null } ?: match?.groupValues?.get(2)
                            val parts = cleanedAttr?.trim()?.split(Regex("\\s+|,\\s*"))
                            if (parts != null && parts.size >= 4) {
                                val w = parts[2].toFloatOrNull() ?: 100f
                                val h = parts[3].toFloatOrNull() ?: 100f
                                Pair(w, h)
                            } else {
                                val widthRegex = Regex("width\\s*=\\s*\"([^\"]*)\"|width\\s*=\\s*'([^']*)'")
                                val heightRegex = Regex("height\\s*=\\s*\"([^\"]*)\"|height\\s*=\\s*'([^']*)'")
                                val wVal = (widthRegex.find(rawText)?.groupValues?.get(1) ?: widthRegex.find(rawText)?.groupValues?.get(2))
                                    ?.removeSuffix("px")?.removeSuffix("dp")?.toFloatOrNull() ?: 100f
                                val hVal = (heightRegex.find(rawText)?.groupValues?.get(1) ?: heightRegex.find(rawText)?.groupValues?.get(2))
                                    ?.removeSuffix("px")?.removeSuffix("dp")?.toFloatOrNull() ?: 100f
                                Pair(wVal, hVal)
                            }
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val (viewportW, viewportH) = viewBoxDimensions
                            if (viewportW > 0f && viewportH > 0f) {
                                val scaleX = size.width / viewportW
                                val scaleY = size.height / viewportH
                                val scaleFactor = minOf(scaleX, scaleY)
                                val offsetX = (size.width - (viewportW * scaleFactor)) / 2f
                                val offsetY = (size.height - (viewportH * scaleFactor)) / 2f

                                drawContext.canvas.save()
                                drawContext.canvas.translate(offsetX, offsetY)
                                drawContext.canvas.scale(scaleFactor, scaleFactor)

                                elements.forEach { element ->
                                    when (element) {
                                        is SvgElement.Rect -> {
                                            drawRect(
                                                color = element.fill,
                                                topLeft = androidx.compose.ui.geometry.Offset(element.x, element.y),
                                                size = androidx.compose.ui.geometry.Size(element.width, element.height)
                                            )
                                        }
                                        is SvgElement.Circle -> {
                                            drawCircle(
                                                color = element.fill,
                                                radius = element.r,
                                                center = androidx.compose.ui.geometry.Offset(element.cx, element.cy)
                                            )
                                        }
                                        is SvgElement.Line -> {
                                            drawLine(
                                                color = element.stroke,
                                                start = androidx.compose.ui.geometry.Offset(element.x1, element.y1),
                                                end = androidx.compose.ui.geometry.Offset(element.x2, element.y2),
                                                strokeWidth = element.strokeWidth
                                            )
                                        }
                                        is SvgElement.Polygon -> {
                                            if (element.points.isNotEmpty()) {
                                                val p = Path().apply {
                                                    moveTo(element.points[0].first, element.points[0].second)
                                                    for (i in 1 until element.points.size) {
                                                        lineTo(element.points[i].first, element.points[i].second)
                                                    }
                                                    close()
                                                }
                                                drawPath(p, color = element.fill)
                                            }
                                        }
                                        is SvgElement.PathElement -> {
                                            val p = SvgConverter.buildComposePath(element.pathData)
                                            if (element.fill != Color.Transparent) {
                                                drawPath(p, color = element.fill)
                                            }
                                            if (element.stroke != Color.Transparent) {
                                                drawPath(p, color = element.stroke, style = Stroke(width = element.strokeWidth))
                                            }
                                        }
                                    }
                                }
                                drawContext.canvas.restore()
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ANDROID VECTOR XML",
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = SleekTextMain,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Button(
                            onClick = {
                                try {
                                    val targetXmlFile = File(file.parentFile, file.nameWithoutExtension + "_vector.xml")
                                    targetXmlFile.writeText(convertedXmlText)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekFolderBg, contentColor = SleekFolderText),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Vector XML", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .border(1.dp, SleekBorderLight, RoundedCornerShape(16.dp))
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = convertedXmlText,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = SleekTextAlt
                            )
                        )
                    }
                }
            }
        }
    }
}

class CodeSyntaxTransformation(private val language: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styledString = highlightCodeTokens(text.text, language)
        return TransformedText(styledString, OffsetMapping.Identity)
    }
}

fun highlightCodeTokens(code: String, language: String): AnnotatedString {
    val builder = AnnotatedString.Builder(code)

    val keywordColor = Color(0xFFC792EA)   // Lavender
    val typeColor = Color(0xFF82AAFF)      // Blue
    val commentColor = Color(0xFF676E95)   // Slate gray
    val stringColor = Color(0xFFC3E88D)    // Leaf green
    val numberColor = Color(0xFFF78C6C)    // Vibrant orange
    val markupTagColor = Color(0xFFF07178) // Red-Pink

    try {
        val singleLineComment = Regex("//.*")
        val multiLineComment = Regex("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/")
        
        singleLineComment.findAll(code).forEach { match ->
            builder.addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
        }
        
        multiLineComment.findAll(code).forEach { match ->
            builder.addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
        }

        val strings = Regex("\"[^\"]*\"|'[^']*'")
        strings.findAll(code).forEach { match ->
            builder.addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
        }

        val numbers = Regex("\\b\\d+(\\.\\d+)?\\b|\\b0x[0-9a-fA-F]+\\b")
        numbers.findAll(code).forEach { match ->
            builder.addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
        }

        when (language) {
            "kotlin", "java" -> {
                val keywords = Regex("\\b(package|import|class|interface|object|fun|val|var|vararg|const|private|protected|public|override|companion|internal|init|return|if|else|when|for|while|do|throw|try|catch|finally|this|super|null|true|false)\\b")
                keywords.findAll(code).forEach { match ->
                    builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                }

                val types = Regex("\\b(String|Int|Boolean|Float|Double|Long|Byte|Short|Char|Unit|Any|Bundle|ComponentActivity|Context)\\b")
                types.findAll(code).forEach { match ->
                    builder.addStyle(SpanStyle(color = typeColor), match.range.first, match.range.last + 1)
                }
            }
            "javascript" -> {
                val keywords = Regex("\\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|new|class|import|export|from|default|null|true|false|console)\\b")
                keywords.findAll(code).forEach { match ->
                    builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                }
            }
            "css" -> {
                val keywords = Regex("\\b(body|div|span|h1|h2|h3|h4|h5|h6|p|a|img|ul|li|section|header|footer|background|color|border|margin|padding|font-family|font-size|box-shadow)\\b")
                keywords.findAll(code).forEach { match ->
                    builder.addStyle(SpanStyle(color = keywordColor), match.range.first, match.range.last + 1)
                }
            }
            "svg", "xml" -> {
                val tags = Regex("<svg|<path|<rect|<circle|<ellipse|<line|<polyline|<polygon|<text|</svg>|</path>|</rect>|</circle>|</line>|</polygon>")
                tags.findAll(code).forEach { match ->
                    builder.addStyle(SpanStyle(color = markupTagColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                }
                val attrs = Regex("\\s+([a-zA-Z\\-]+)\\s*=")
                attrs.findAll(code).forEach { match ->
                    if (match.groupValues.size > 1) {
                        val firstGroupRange = match.groups[1]?.range
                        if (firstGroupRange != null) {
                            builder.addStyle(SpanStyle(color = typeColor), firstGroupRange.first, firstGroupRange.last + 1)
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {}

    return builder.toAnnotatedString()
}
