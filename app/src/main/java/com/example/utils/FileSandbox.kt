package com.example.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileSandbox {
    fun setupSandbox(context: Context): File {
        val rootDir = File(context.filesDir, "Sandbox")
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }

        // Create directories
        val docDir = File(rootDir, "Documents").apply { mkdirs() }
        val sourceDir = File(rootDir, "SourceCode").apply { mkdirs() }
        val mediaDir = File(rootDir, "Media").apply { mkdirs() }
        val archiveDir = File(rootDir, "Archive").apply { mkdirs() }

        // 1. Create Documents
        createFileUnlessExists(File(docDir, "Welcome.txt"), 
            "Welcome to Material Expressive File Manager!\n\n" +
            "This application showcases pure Kotlin & Jetpack Compose craftsmanship.\n" +
            "Available tools include:\n" +
            " - Syntactical Code Editor (with multi-language highlighting)\n" +
            " - ZIP Viewer & Extractor (standard compliance)\n" +
            " - Audio/Music player (with visualizers and playback state)\n" +
            " - Dynamic Vector SVG to XML converter with live preview canvas\n" +
            " - Complete binary Hex Editor with visual addressing & byte modifications\n" +
            " - Seamless, highly responsive transitions between directory branches\n"
        )

        // 2. Create Source Code examples
        createFileUnlessExists(File(sourceDir, "HelloWorld.kt"), 
            "package com.example\n\n" +
            "import android.os.Bundle\n" +
            "import androidx.activity.ComponentActivity\n\n" +
            "/**\n" +
            " * Representing a clean development interface\n" +
            " */\n" +
            "class HelloWorld : ComponentActivity() {\n" +
            "    companion object {\n" +
            "        private const val TAG = \"HelloWorld\"\n" +
            "    }\n\n" +
            "    private var currentCount = 0\n\n" +
            "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
            "        super.onCreate(savedInstanceState)\n" +
            "        println(\"Executing application loop...\")\n" +
            "        val userIsActive = true\n" +
            "        if (userIsActive) {\n" +
            "            performHandshake()\n" +
            "        }\n" +
            "    }\n\n" +
            "    private fun performHandshake() {\n" +
            "        val code = 0xAA.toByte()\n" +
            "        val msg = \"Expressive File Manager is up and running.\"\n" +
            "        println(\"Handshake state: \$msg (code: \$code)\")\n" +
            "        currentCount++\n" +
            "    }\n" +
            "}"
        )

        createFileUnlessExists(File(sourceDir, "index.js"), 
            "// Expressive Javascript File Example\n" +
            "const fileLimit = 1048576; // 1MB\n" +
            "let activeFiles = [];\n\n" +
            "function inspectFile(file) {\n" +
            "    console.log('Analyzing file: ' + file.name);\n" +
            "    if (file.size > fileLimit) {\n" +
            "        return { status: 'error', reason: 'Storage limit exceeded' };\n" +
            "    }\n" +
            "    activeFiles.push(file);\n" +
            "    return { status: 'success', fileCount: activeFiles.length };\n" +
            "}"
        )

        createFileUnlessExists(File(sourceDir, "styles.css"), 
            "/* Custom UI theme overlay styles */\n" +
            "body {\n" +
            "    background-color: #0E0D12;\n" +
            "    color: #E2E1E6;\n" +
            "    font-family: 'Roboto', sans-serif;\n" +
            "}\n\n" +
            ".expressive-card {\n" +
            "    border-radius: 24px;\n" +
            "    background: linear-gradient(135deg, #1B1A24 0%, #252433 100%);\n" +
            "    box-shadow: 0 8px 32px rgba(0,0,0,0.4);\n" +
            "    border: 1px solid rgba(255,255,255,0.08);\n" +
            "}"
        )

        createFileUnlessExists(File(sourceDir, "vector_heart.svg"), 
            "<svg viewBox=\"0 0 100 100\">\n" +
            "  <rect x=\"5\" y=\"5\" width=\"90\" height=\"90\" fill=\"#1B1A24\" rx=\"16\" />\n" +
            "  <circle cx=\"35\" cy=\"45\" r=\"20\" fill=\"#FF2D55\" />\n" +
            "  <circle cx=\"65\" cy=\"45\" r=\"20\" fill=\"#FF2D55\" />\n" +
            "  <polygon points=\"17,55 83,55 50,90\" fill=\"#FF2D55\" />\n" +
            "  <circle cx=\"50\" cy=\"50\" r=\"8\" fill=\"#FFFFFF\" />\n" +
            "</svg>"
        )

        createFileUnlessExists(File(sourceDir, "sample_doc.xml"), 
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<settings>\n" +
            "    <theme mode=\"dark\" primaryColor=\"#3D5AFE\">\n" +
            "        <fontFamily>Roboto</fontFamily>\n" +
            "        <animations enabled=\"true\" duration=\"250\" />\n" +
            "    </theme>\n" +
            "    <features>\n" +
            "        <editor tabSize=\"4\" syntaxHighlighting=\"true\" />\n" +
            "        <audio autoPlay=\"false\" showWaveForm=\"true\" />\n" +
            "    </features>\n" +
            "</settings>"
        )

        // 3. Create real WAV audio programmatically
        val wavFile = File(mediaDir, "Chiptune_Synthesizer_440Hz.wav")
        if (!wavFile.exists()) {
            createSampleWavFile(wavFile)
        }

        // 4. Create real valid ZIP programmatically
        val zipFile = File(archiveDir, "SampleProject.zip")
        if (!zipFile.exists()) {
            createSampleZipFile(zipFile)
        }

        // 5. Create a dummy test file with raw binary content for the Hex editor
        createFileUnlessExists(File(rootDir, "firmware.bin"), byteArrayOf(
            0x7F.toByte(), 'E'.toByte(), 'L'.toByte(), 'F'.toByte(), // ELF Magic Bytes
            0x02.toByte(), 0x01.toByte(), 0x01.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x01.toByte(), 0x00.toByte(), 0x3E.toByte(), 0x00.toByte(), // X86-64
            0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(),
            0x41.toByte(), 0x42.toByte(), 0x43.toByte(), 0x44.toByte(), // ABCD
            0x0F.toByte(), 0xFF.toByte(), 0x55.toByte(), 0x5F.toByte(),
            0x10.toByte(), 0x20.toByte(), 0x30.toByte(), 0x40.toByte(),
            'Z'.toByte(), 'e'.toByte(), 't'.toByte(), 'M'.toByte(),
            'a'.toByte(), 'n'.toByte(), 'a'.toByte(), 'g'.toByte(),
            'e'.toByte(), 'r'.toByte(), 'e'.toByte(), 'x'.toByte(),
            0x11.toByte(), 0x12.toByte(), 0x13.toByte(), 0x14.toByte()
        ))

        return rootDir
    }

    private fun createFileUnlessExists(file: File, content: String) {
        if (!file.exists()) {
            file.writeText(content)
        }
    }

    private fun createFileUnlessExists(file: File, bytes: ByteArray) {
        if (!file.exists()) {
            file.writeBytes(bytes)
        }
    }

    private fun createSampleWavFile(file: File) {
        val sampleRate = 8000
        val durationSeconds = 8
        val numSamples = sampleRate * durationSeconds
        val headerSize = 44
        val dataSize = numSamples * 2 // 16-bit mono PCM is 2 bytes per sample
        val fileSize = headerSize + dataSize - 8

        FileOutputStream(file).use { os ->
            // RIFF header
            os.write("RIFF".toByteArray())
            os.write(intToBytes(fileSize))
            os.write("WAVE".toByteArray())

            // fmt subchunk
            os.write("fmt ".toByteArray())
            os.write(intToBytes(16)) // Subchunk1Size (16 for PCM)
            os.write(shortToBytes(1)) // AudioFormat (1 for PCM)
            os.write(shortToBytes(1)) // NumChannels (1 for Mono)
            os.write(intToBytes(sampleRate)) // SampleRate
            os.write(intToBytes(sampleRate * 2)) // ByteRate (8000 * 1 * 2 bytes/sample)
            os.write(shortToBytes(2)) // BlockAlign (1 channel * 2 bytes)
            os.write(shortToBytes(16)) // BitsPerSample

            // data subchunk
            os.write("data".toByteArray())
            os.write(intToBytes(dataSize))

            // Write nice frequency sweep audio PCM data
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / sampleRate
                // Dynamic frequency modulation! (arpeggio sound!)
                val freqIndex = (progress.toInt() % 4)
                val baseFreq = when (freqIndex) {
                    0 -> 261.63 // C4
                    1 -> 329.63 // E4
                    2 -> 392.00 // G4
                    3 -> 523.25 // C5
                    else -> 440.0
                }
                val angle = 2.0 * Math.PI * baseFreq * progress
                val amplitude = 14000.0 // volume range
                val sampleVal = (Math.sin(angle) * amplitude).toInt().toShort()
                os.write(shortToBytes(sampleVal))
            }
        }
    }

    private fun createSampleZipFile(file: File) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // Entry 1: README.md
            zos.putNextEntry(ZipEntry("README.md"))
            zos.write(
                ("## Demonstration Archive\n\n" +
                 "This ZIP file is parsed and extracted natively on Android using fully compliant ZIP Streams.\n" +
                 "Explore the directory hierarchy inside the ZIP file to extract specific items or view their codes!").toByteArray()
            )
            zos.closeEntry()

            // Entry 2: subfolder/app_config.json
            zos.putNextEntry(ZipEntry("subfolder/app_config.json"))
            zos.write(
                ("{\n" +
                 "  \"appName\": \"Material Expressive File Manager\",\n" +
                 "  \"engine\": \"Jetpack Compose\n\",\n" +
                 "  \"build\": 2026,\n" +
                 "  \"features\": [\"ZipViewer\", \"HexEditor\", \"CodeEditor\", \"MusicPlayer\"]\n" +
                 "}").toByteArray()
            )
            zos.closeEntry()

            // Entry 3: subfolder/utilities.kt
            zos.putNextEntry(ZipEntry("subfolder/utilities.kt"))
            zos.write(
                ("package com.example.internal\n\n" +
                 "fun computeFileHash(file: String): Int {\n" +
                 "    return file.fold(0) { acc, c -> acc * 31 + c.code }\n" +
                 "}").toByteArray()
            )
            zos.closeEntry()
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToBytes(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }
}
