package com.example.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import java.util.Locale

// Simple model representing parsed SVG elements for live visual rendering inside the app.
sealed class SvgElement {
    data class Rect(val x: Float, val y: Float, val width: Float, val height: Float, val rx: Float, val fill: Color) : SvgElement()
    data class Circle(val cx: Float, val cy: Float, val r: Float, val fill: Color) : SvgElement()
    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val stroke: Color, val strokeWidth: Float) : SvgElement()
    data class Polygon(val points: List<Pair<Float, Float>>, val fill: Color) : SvgElement()
    data class PathElement(val pathData: String, val fill: Color, val stroke: Color, val strokeWidth: Float) : SvgElement()
}

object SvgConverter {

    /**
     * Converts a raw SVG file/string into an Android Vector Drawable XML.
     */
    fun svgToXml(svgContent: String): String {
        val width = extractAttribute(svgContent, "width")?.removeSuffix("px")?.removeSuffix("dp")?.toIntOrNull() ?: 100
        val height = extractAttribute(svgContent, "height")?.removeSuffix("px")?.removeSuffix("dp")?.toIntOrNull() ?: 100
        
        // Find viewBox
        val viewBoxAttr = extractAttribute(svgContent, "viewBox")
        val viewBoxParts = viewBoxAttr?.split(Regex("\\s+|,\\s*")) ?: listOf("0", "0", "100", "100")
        val viewportWidth = if (viewBoxParts.size >= 4) viewBoxParts[2].toIntOrNull() ?: width else width
        val viewportHeight = if (viewBoxParts.size >= 4) viewBoxParts[3].toIntOrNull() ?: height else height

        val xmlBuilder = java.lang.StringBuilder()
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        xmlBuilder.append("<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n")
        xmlBuilder.append("    android:width=\"${width}dp\"\n")
        xmlBuilder.append("    android:height=\"${height}dp\"\n")
        xmlBuilder.append("    android:viewportWidth=\"$viewportWidth\"\n")
        xmlBuilder.append("    android:viewportHeight=\"$viewportHeight\">\n\n")

        // Parse Rects
        val rectRegex = Regex("<rect\\s*([^>]*)/?>")
        rectRegex.findAll(svgContent).forEach { match ->
            val attrs = match.groupValues[1]
            val rX = extractNum(attrs, "x") ?: 0f
            val rY = extractNum(attrs, "y") ?: 0f
            val rW = extractNum(attrs, "width") ?: 0f
            val rH = extractNum(attrs, "height") ?: 0f
            val rx = extractNum(attrs, "rx") ?: 0f
            val fillHex = convertColorToHex(extractAttribute(attrs, "fill") ?: "#000000")
            
            val pathData = if (rx > 0f) {
                // Round rect path approximation
                "M ${rX + rx} $rY h ${rW - 2 * rx} a $rx $rx 0 0 1 $rx $rx v ${rH - 2 * rx} a $rx $rx 0 0 1 -$rx $rx h -${rW - 2 * rx} a $rx $rx 0 0 1 -$rx -$rx v -${rH - 2 * rx} a $rx $rx 0 0 1 $rx -$rx Z"
            } else {
                "M $rX $rY h $rW v $rH h -$rW Z"
            }

            xmlBuilder.append("    <path\n")
            xmlBuilder.append("        android:fillColor=\"$fillHex\"\n")
            xmlBuilder.append("        android:pathData=\"$pathData\" />\n\n")
        }

        // Parse Circles
        val circleRegex = Regex("<circle\\s*([^>]*)/?>")
        circleRegex.findAll(svgContent).forEach { match ->
            val attrs = match.groupValues[1]
            val cx = extractNum(attrs, "cx") ?: 0f
            val cy = extractNum(attrs, "cy") ?: 0f
            val r = extractNum(attrs, "r") ?: 0f
            val fillHex = convertColorToHex(extractAttribute(attrs, "fill") ?: "#000000")

            // Circle represented as two SVG arcs in vector format
            val pathData = "M ${cx - r} $cy A $r $r 0 1 0 ${cx + r} $cy A $r $r 0 1 0 ${cx - r} $cy Z"

            xmlBuilder.append("    <path\n")
            xmlBuilder.append("        android:fillColor=\"$fillHex\"\n")
            xmlBuilder.append("        android:pathData=\"$pathData\" />\n\n")
        }

        // Parse Lines
        val lineRegex = Regex("<line\\s*([^>]*)/?>")
        lineRegex.findAll(svgContent).forEach { match ->
            val attrs = match.groupValues[1]
            val x1 = extractNum(attrs, "x1") ?: 0f
            val y1 = extractNum(attrs, "y1") ?: 0f
            val x2 = extractNum(attrs, "x2") ?: 0f
            val y2 = extractNum(attrs, "y2") ?: 0f
            val sColor = convertColorToHex(extractAttribute(attrs, "stroke") ?: "#000000")
            val sWidth = extractNum(attrs, "stroke-width") ?: 1f

            val pathData = "M $x1 $y1 L $x2 $y2"

            xmlBuilder.append("    <path\n")
            xmlBuilder.append("        android:strokeColor=\"$sColor\"\n")
            xmlBuilder.append("        android:strokeWidth=\"$sWidth\"\n")
            xmlBuilder.append("        android:pathData=\"$pathData\" />\n\n")
        }

        // Parse Polygons
        val polyRegex = Regex("<polygon\\s*([^>]*)/?>")
        polyRegex.findAll(svgContent).forEach { match ->
            val attrs = match.groupValues[1]
            val pointsStr = extractAttribute(attrs, "points") ?: ""
            val fillHex = convertColorToHex(extractAttribute(attrs, "fill") ?: "#000000")
            
            val points = parsePoints(pointsStr)
            if (points.isNotEmpty()) {
                val pathData = StringBuilder()
                pathData.append("M ${points[0].first} ${points[0].second}")
                for (i in 1 until points.size) {
                    pathData.append(" L ${points[i].first} ${points[i].second}")
                }
                pathData.append(" Z")

                xmlBuilder.append("    <path\n")
                xmlBuilder.append("        android:fillColor=\"$fillHex\"\n")
                xmlBuilder.append("        android:pathData=\"$pathData\" />\n\n")
            }
        }

        // Parse simple SVG Paths
        val pathRegex = Regex("<path\\s*([^>]*)/?>")
        pathRegex.findAll(svgContent).forEach { match ->
            val attrs = match.groupValues[1]
            val dData = extractAttribute(attrs, "d") ?: ""
            val fillColor = extractAttribute(attrs, "fill") ?: "none"
            val strokeColor = extractAttribute(attrs, "stroke") ?: "none"
            val strokeWidth = extractNum(attrs, "stroke-width") ?: 1f

            if (dData.isNotEmpty()) {
                xmlBuilder.append("    <path\n")
                if (fillColor != "none") {
                    xmlBuilder.append("        android:fillColor=\"${convertColorToHex(fillColor)}\"\n")
                }
                if (strokeColor != "none") {
                    xmlBuilder.append("        android:strokeColor=\"${convertColorToHex(strokeColor)}\"\n")
                    xmlBuilder.append("        android:strokeWidth=\"$strokeWidth\"\n")
                }
                xmlBuilder.append("        android:pathData=\"$dData\" />\n\n")
            }
        }

        xmlBuilder.append("</vector>")
        return xmlBuilder.toString()
    }

    /**
     * Parses the SVG string into a list of drawable elements for native Canvas preview.
     */
    fun parseSvgForPreview(svgContent: String): List<SvgElement> {
        val elements = mutableListOf<SvgElement>()

        // 1. Rect
        Regex("<rect\\s*([^>]*)/?>").findAll(svgContent).forEach { match ->
            val attrs = match.groupValues[1]
            val x = extractNum(attrs, "x") ?: 0f
            val y = extractNum(attrs, "y") ?: 0f
            val w = extractNum(attrs, "width") ?: 0f
            val h = extractNum(attrs, "height") ?: 0f
            val rx = extractNum(attrs, "rx") ?: 0f
            val fill = parseComposeColor(extractAttribute(attrs, "fill") ?: "#000000")
            elements.add(SvgElement.Rect(x, y, w, h, rx, fill))
        }

        // 2. Circle
        Regex("<circle\\s*([^>]*)/?>").findAll(svgContent).forEach { match ->
            val attrs = match.groupValues[1]
            val cx = extractNum(attrs, "cx") ?: 0f
            val cy = extractNum(attrs, "cy") ?: 0f
            val r = extractNum(attrs, "r") ?: 0f
            val fill = parseComposeColor(extractAttribute(attrs, "fill") ?: "#000000")
            elements.add(SvgElement.Circle(cx, cy, r, fill))
        }

        // 3. Line
        Regex("<line\\s*([^>]*)/?>").findAll(svgContent).forEach { match ->
            val attrs = match.groupValues[1]
            val x1 = extractNum(attrs, "x1") ?: 0f
            val y1 = extractNum(attrs, "y1") ?: 0f
            val x2 = extractNum(attrs, "x2") ?: 0f
            val y2 = extractNum(attrs, "y2") ?: 0f
            val stroke = parseComposeColor(extractAttribute(attrs, "stroke") ?: "#000000")
            val sWidth = extractNum(attrs, "stroke-width") ?: 1f
            elements.add(SvgElement.Line(x1, y1, x2, y2, stroke, sWidth))
        }

        // 4. Polygon
        Regex("<polygon\\s*([^>]*)/?>").findAll(svgContent).forEach { match ->
            val attrs = match.groupValues[1]
            val pointsStr = extractAttribute(attrs, "points") ?: ""
            val fill = parseComposeColor(extractAttribute(attrs, "fill") ?: "#000000")
            val points = parsePoints(pointsStr)
            if (points.isNotEmpty()) {
                elements.add(SvgElement.Polygon(points, fill))
            }
        }

        // 5. Raw Path (simple visual extraction if parseable)
        Regex("<path\\s*([^>]*)/?>").findAll(svgContent).forEach { match ->
            val attrs = match.groupValues[1]
            val dData = extractAttribute(attrs, "d") ?: ""
            val fillColor = parseComposeColor(extractAttribute(attrs, "fill") ?: "none")
            val strokeColor = parseComposeColor(extractAttribute(attrs, "stroke") ?: "none")
            val sWidth = extractNum(attrs, "stroke-width") ?: 1f
            if (dData.isNotEmpty()) {
                elements.add(SvgElement.PathElement(dData, fillColor, strokeColor, sWidth))
            }
        }

        return elements
    }

    private fun extractAttribute(tagContent: String, attrName: String): String? {
        val patterns = listOf(
            Regex("$attrName\\s*=\\s*\"([^\"]*)\""),
            Regex("$attrName\\s*=\\s*'([^']*)'")
        )
        for (pattern in patterns) {
            val match = pattern.find(tagContent)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun extractNum(tagContent: String, attrName: String): Float? {
        return extractAttribute(tagContent, attrName)?.toFloatOrNull()
    }

    private fun parsePoints(pointsStr: String): List<Pair<Float, Float>> {
        val result = mutableListOf<Pair<Float, Float>>()
        val coords = pointsStr.trim().split(Regex("[\\s,]+"))
        for (i in 0 until coords.size - 1 step 2) {
            val cx = coords[i].toFloatOrNull()
            val cy = coords[i + 1].toFloatOrNull()
            if (cx != null && cy != null) {
                result.add(Pair(cx, cy))
            }
        }
        return result
    }

    private fun convertColorToHex(colorStr: String): String {
        val content = colorStr.trim().lowercase(Locale.ROOT)
        if (content.startsWith("#")) {
            val cleanHex = content.removePrefix("#")
            return when (cleanHex.length) {
                3 -> "#FF" + cleanHex.map { "$it$it" }.joinToString("")
                6 -> "#FF$cleanHex"
                8 -> "#$cleanHex"
                else -> "#FF000000"
            }
        }
        return when (content) {
            "red" -> "#FFFF0000"
            "blue" -> "#FF0000FF"
            "green" -> "#FF008000"
            "yellow" -> "#FFFFFF00"
            "white" -> "#FFFFFFFF"
            "black" -> "#FF000000"
            "pink" -> "#FFFFC0CB"
            "gray" -> "#FF808080"
            "purple" -> "#FF800080"
            "none" -> "#00000000"
            else -> "#FF000000"
        }
    }

    private fun parseComposeColor(colorStr: String): Color {
        val hex = convertColorToHex(colorStr)
        if (hex.startsWith("#")) {
            val unsignedHex = hex.removePrefix("#").toLong(16)
            return Color(unsignedHex)
        }
        return Color.Transparent
    }

    /**
     * Approximate SVG path data parser into Jetpack Compose Path
     */
    fun buildComposePath(d: String): Path {
        val path = Path()
        try {
            val commands = d.split(Regex("(?=[A-Za-z])"))
            var lastX = 0f
            var lastY = 0f
            for (commandGroup in commands) {
                val token = commandGroup.trim()
                if (token.isEmpty()) continue
                val action = token[0]
                
                // Regex matches standard decimal / scientific numbers, correctly isolating leading sign flags (e.g., negative coefficients)
                val numbersRegex = Regex("[-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?")
                val nums = numbersRegex.findAll(token.substring(1))
                    .mapNotNull { it.value.toFloatOrNull() }
                    .toList()

                when (action) {
                    'M' -> {
                        if (nums.size >= 2) {
                            lastX = nums[0]
                            lastY = nums[1]
                            path.moveTo(lastX, lastY)
                        }
                    }
                    'm' -> {
                        if (nums.size >= 2) {
                            lastX += nums[0]
                            lastY += nums[1]
                            path.moveTo(lastX, lastY)
                        }
                    }
                    'L' -> {
                        for (i in 0 until nums.size - 1 step 2) {
                            lastX = nums[i]
                            lastY = nums[i+1]
                            path.lineTo(lastX, lastY)
                        }
                    }
                    'l' -> {
                        for (i in 0 until nums.size - 1 step 2) {
                            lastX += nums[i]
                            lastY += nums[i+1]
                            path.lineTo(lastX, lastY)
                        }
                    }
                    'H' -> {
                        for (x in nums) {
                            lastX = x
                            path.lineTo(lastX, lastY)
                        }
                    }
                    'h' -> {
                        for (dx in nums) {
                            lastX += dx
                            path.lineTo(lastX, lastY)
                        }
                    }
                    'V' -> {
                        for (y in nums) {
                            lastY = y
                            path.lineTo(lastX, lastY)
                        }
                    }
                    'v' -> {
                        for (dy in nums) {
                            lastY += dy
                            path.lineTo(lastX, lastY)
                        }
                    }
                    'Z', 'z' -> {
                        path.close()
                    }
                }
            }
        } catch (_: Exception) {
            // Safe fallbacks for complex SVG parsing
        }
        return path
    }
}
