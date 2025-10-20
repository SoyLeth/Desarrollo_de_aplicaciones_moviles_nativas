package com.example.practica3.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat

data class FileItem(
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long?,
    val lastModified: Long?,
    val extension: String?
) {
    fun formattedSize(): String =
        if (isDirectory || sizeBytes == null || sizeBytes < 0) "--"
        else when {
            sizeBytes >= 1_000_000_000L -> "%.2f GB".format(sizeBytes / 1_000_000_000f)
            sizeBytes >= 1_000_000L     -> "%.2f MB".format(sizeBytes / 1_000_000f)
            sizeBytes >= 1_000L         -> "%.2f KB".format(sizeBytes / 1_000f)
            else                        -> "$sizeBytes B"
        }
    fun formattedDate(): String =
        lastModified?.let { DateFormat.getDateTimeInstance().format(it) } ?: "--"
}

class FileRepository(private val context: Context) {

    suspend fun listChildren(dirTreeUri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, dirTreeUri) ?: return@withContext emptyList()
        root.listFiles()
            .sortedWith(compareBy<DocumentFile>({ !it.isDirectory }, { it.name?.lowercase() ?: "" }))
            .map { df ->
                FileItem(
                    uri = df.uri,
                    name = df.name ?: "(sin nombre)",
                    isDirectory = df.isDirectory,
                    sizeBytes = if (df.isDirectory) null else df.length(),
                    lastModified = runCatching { df.lastModified() }.getOrNull(),
                    extension = df.name?.substringAfterLast('.', missingDelimiterValue = "").orEmpty()
                        .lowercase().ifEmpty { null }
                )
            }
    }

    fun getParent(dirOrFileUri: Uri): Uri? {
        val df = DocumentFile.fromTreeUri(context, dirOrFileUri)
            ?: DocumentFile.fromSingleUri(context, dirOrFileUri)
        return df?.parentFile?.uri
    }
}
