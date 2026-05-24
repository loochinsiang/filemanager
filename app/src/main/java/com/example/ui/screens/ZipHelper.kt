package com.example.ui.screens

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipHelper {
    fun removeEntry(zipFile: File, entryNameToRemove: String): Boolean {
        return try {
            val bakFile = File(zipFile.parentFile, zipFile.name + ".bak")
            if (!bakFile.exists()) {
                zipFile.copyTo(bakFile, overwrite = true)
            }
            
            val tempFile = File(zipFile.parentFile, zipFile.name + ".tmp")
            
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name != entryNameToRemove && !entry.name.startsWith("$entryNameToRemove/")) {
                            zos.putNextEntry(ZipEntry(entry.name))
                            zis.copyTo(zos)
                            zos.closeEntry()
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            
            tempFile.copyTo(zipFile, overwrite = true)
            tempFile.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun addOrUpdateEntry(zipFile: File, entryName: String, newContent: ByteArray): Boolean {
        return try {
            val bakFile = File(zipFile.parentFile, zipFile.name + ".bak")
            if (!bakFile.exists()) {
                zipFile.copyTo(bakFile, overwrite = true)
            }
            
            val tempFile = File(zipFile.parentFile, zipFile.name + ".tmp")
            
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name != entryName) {
                            zos.putNextEntry(ZipEntry(entry.name))
                            zis.copyTo(zos)
                            zos.closeEntry()
                        }
                        entry = zis.nextEntry
                    }
                    // Add new entry
                    zos.putNextEntry(ZipEntry(entryName))
                    zos.write(newContent)
                    zos.closeEntry()
                }
            }
            
            tempFile.copyTo(zipFile, overwrite = true)
            tempFile.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun renameEntry(zipFile: File, oldName: String, newName: String): Boolean {
        return try {
            val bakFile = File(zipFile.parentFile, zipFile.name + ".bak")
            if (!bakFile.exists()) {
                zipFile.copyTo(bakFile, overwrite = true)
            }
            
            val tempFile = File(zipFile.parentFile, zipFile.name + ".tmp")
            
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val nameToWrite = if (entry.name == oldName) newName else if (entry.name.startsWith("$oldName/")) entry.name.replaceFirst(oldName, newName) else entry.name
                        zos.putNextEntry(ZipEntry(nameToWrite))
                        zis.copyTo(zos)
                        zos.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            
            tempFile.copyTo(zipFile, overwrite = true)
            tempFile.delete()
            true
        } catch (e: Exception) {
            false
        }
    }
}
