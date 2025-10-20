@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.practica3.ui.viewer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun ImageViewerScreen(
    uriString: String,
    onBack: () -> Unit
) {
    val uri = remember(uriString) { Uri.parse(uriString) }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Gestos
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    fun reset() { scale = 1f; rotation = 0f; offsetX = 0f; offsetY = 0f }

    val context = LocalContext.current
    LaunchedEffect(uriString) {
        error = null; bmp = null
        val (bitmap, err) = loadBitmapSaf { context.contentResolver.openInputStream(uri) }
        bmp = bitmap; error = err
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visor de imágenes") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Atrás") }
                },
                actions = {
                    IconButton(onClick = { rotation += 90f }) { Icon(Icons.Outlined.RotateRight, contentDescription = "Rotar 90°") }
                    IconButton(onClick = { reset() }) { Icon(Icons.Outlined.Refresh, contentDescription = "Reiniciar vista") }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotate ->
                        scale = (scale * zoom).coerceIn(0.5f, 6f)
                        rotation += (rotate * 57.2958f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                bmp != null -> {
                    androidx.compose.foundation.Image(
                        bitmap = bmp!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = offsetX
                                translationY = offsetY
                                scaleX = scale
                                scaleY = scale
                                rotationZ = rotation
                            }
                    )
                }
                error != null -> {
                    Text(error!!, color = Color.White, modifier = Modifier.padding(16.dp))
                }
                else -> {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

/** Decodifica abriendo el InputStream dos veces (sin mark/reset). */
private suspend fun loadBitmapSaf(
    open: () -> InputStream?
): Pair<Bitmap?, String?> = withContext(Dispatchers.IO) {
    runCatching<Pair<Bitmap?, String?>> {
        // 1) Primer open: bounds
        val first = open() ?: return@runCatching Pair(null, "No se pudo abrir el archivo")
        val (w, h) = first.use { s1 ->
            val optBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(s1, null, optBounds)
            optBounds.outWidth to optBounds.outHeight
        }
        if (w <= 0 || h <= 0) return@runCatching Pair(null, "Formato de imagen no soportado")

        // 2) Calcular sample
        val maxDim = 4096
        var sample = 1
        var w2 = w
        var h2 = h
        while (kotlin.math.max(w2, h2) > maxDim) {
            sample *= 2
            w2 = w / sample
            h2 = h / sample
        }

        // 3) Segundo open: decodificación real
        val second = open() ?: return@runCatching Pair(null, "No se pudo abrir el archivo")
        val bmp = second.use { s2 ->
            val opt = BitmapFactory.Options().apply {
                inSampleSize = kotlin.math.max(1, sample)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeStream(s2, null, opt)
        } ?: return@runCatching Pair(null, "No se pudo decodificar la imagen")

        Pair(bmp, null)
    }.getOrElse { Pair(null, "Error: ${it.message}") }
}
