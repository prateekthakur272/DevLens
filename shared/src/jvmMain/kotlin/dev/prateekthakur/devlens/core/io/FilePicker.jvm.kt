package dev.prateekthakur.devlens.core.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import javax.swing.JFileChooser

actual object FilePicker {
    actual suspend fun pickFile(title: String, extensionFilter: String?): String? = withContext(Dispatchers.Main) {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        if (extensionFilter != null) {
            val extensions = extensionFilter.split(',').map { it.trim().removePrefix(".") }
            dialog.setFilenameFilter { _, name -> extensions.any { name.endsWith(".$it", ignoreCase = true) } }
        }
        dialog.isVisible = true
        val file = dialog.file ?: return@withContext null
        val directory = dialog.directory ?: ""
        directory + file
    }

    actual suspend fun pickSaveFile(title: String, defaultName: String): String? = withContext(Dispatchers.Main) {
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
        dialog.file = defaultName
        dialog.isVisible = true
        val file = dialog.file ?: return@withContext null
        val directory = dialog.directory ?: ""
        directory + file
    }

    actual suspend fun pickDirectory(title: String): String? = withContext(Dispatchers.Main) {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile?.absolutePath else null
    }
}
