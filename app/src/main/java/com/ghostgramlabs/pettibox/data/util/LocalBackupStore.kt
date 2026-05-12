package com.ghostgramlabs.pettibox.data.util

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createBackupFile(): File {
        val dir = backupDir().also { it.mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return File(dir, "pettibox-auto-backup-$stamp.zip")
    }

    fun pruneOldBackups(keep: Int = 7) {
        backupDir()
            .listFiles { file -> file.isFile && file.name.startsWith("pettibox-auto-backup-") && file.extension == "zip" }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
            .drop(keep)
            .forEach { it.delete() }
    }

    fun backupLocationLabel(): String = "Device storage / PettiBox backups"

    fun backupPath(): String = backupDir().absolutePath

    private fun backupDir(): File {
        val external = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(external ?: context.filesDir, "pettibox-backups")
    }
}
