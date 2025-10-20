@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.practica3.ui.viewer

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun TextViewerScreen(
    uriString: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clip: ClipboardManager = LocalClipboardManager.current
    var text by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uriString) {
        val (t, h) = loadTextPreview(context, Uri.parse(uriString))
        text = t
        hint = h
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visor de texto") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Atrás") }
                },
                actions = {
                    IconButton(
                        onClick = { text?.let { clip.setText(AnnotatedString(it)) } },
                        enabled = text != null
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            hint?.let {
                AssistChip(onClick = {}, label = { Text(it) }, enabled = false)
                Spacer(Modifier.height(8.dp))
            }
            if (text == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                Text(
                    text = text!!,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

/** Lee hasta ~1 MB; si excede, corta y muestra aviso. */
private suspend fun loadTextPreview(
    context: android.content.Context,
    uri: Uri,
    maxBytes: Long = 1_000_000
): Pair<String?, String?> = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(uri).use { input ->
            if (input == null) return@withContext Pair(null, "No se pudo abrir el archivo")
            val limited = LimitedInputStream(input, maxBytes + 1)
            val reader = BufferedReader(InputStreamReader(limited))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append('\n')
                if (limited.exceeded) break
            }
            val exceeded = limited.exceeded
            val text = sb.toString()
            val hint = if (exceeded) "Vista previa (recortado a ~1 MB)" else null
            Pair(text, hint)
        }
    }.getOrElse { Pair(null, "Error: ${it.message}") }
}

/** InputStream con límite duro. */
private class LimitedInputStream(
    private val wrapped: java.io.InputStream,
    private val limit: Long
) : java.io.InputStream() {
    var readSoFar = 0L
    var exceeded = false
    override fun read(): Int {
        if (readSoFar >= limit) { exceeded = true; return -1 }
        val r = wrapped.read()
        if (r >= 0) readSoFar++
        return r
    }
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (readSoFar >= limit) { exceeded = true; return -1 }
        val toRead = kotlin.math.min(len.toLong(), (limit - readSoFar)).toInt()
        val r = wrapped.read(b, off, toRead)
        if (r > 0) readSoFar += r
        if (readSoFar >= limit) exceeded = true
        return r
    }
    override fun close() = wrapped.close()
}
