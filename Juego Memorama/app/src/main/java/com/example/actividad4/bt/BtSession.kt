package com.example.actividad4.bt

import android.content.Context
import kotlinx.coroutines.*
import java.io.*
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/**
 * Sesión RFCOMM compartida (host/cliente).
 * - Mensajes de texto por línea (UTF-8) -> onLineMessage
 * - Envío/recepción de XML (archivo completo) -> onXmlReceived
 * - Manejo de EOF/IOExceptions sin crashear la app.
 */
object BtSession {

    // === API pública (usada por la UI) ===
    @Volatile private var socket: android.bluetooth.BluetoothSocket? = null
    @Volatile private var input: InputStream? = null
    @Volatile private var output: OutputStream? = null
    @Volatile private var amHost: Boolean = false

    /** Callback para líneas (por ejemplo: "TAP 7", "VOTE 4x5", "VOTERES ..."). */
    @JvmStatic var onLineMessage: ((String) -> Unit)? = null

    /** Callback cuando llega un XML completo. */
    @JvmStatic var onXmlReceived: ((File) -> Unit)? = null

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private var readerJob: Job? = null
    private val running = AtomicBoolean(false)

    fun isHost(): Boolean = amHost

    /**
     * Adjunta el socket y arranca el loop de lectura.
     * No lanza excepción si el socket se cierra o falla la lectura: cierra y limpia.
     */
    fun attachSocket(sock: android.bluetooth.BluetoothSocket, fromServer: Boolean) {
        clearSession()
        socket = sock
        amHost = fromServer
        try {
            input = BufferedInputStream(sock.inputStream)
            output = BufferedOutputStream(sock.outputStream)
        } catch (t: Throwable) {
            // No dejar estado medio roto
            clearSession()
            return
        }
        running.set(true)
        readerJob = ioScope.launch {
            readLoop()
        }
    }

    /** Envío de una línea de texto UTF-8 con salto de línea. */
    fun sendLine(line: String) {
        val out = output ?: return
        try {
            val data = (line + "\n").toByteArray(Charsets.UTF_8)
            synchronized(out) {
                out.write(data)
                out.flush()
            }
        } catch (_: IOException) {
            // Si falla, cerramos silencio y limpiamos
            closeQuietly()
        }
    }

    /**
     * Envía un archivo XML completo enmarcado:
     *   FILEXML <bytes>\n
     *   <payload bytes>
     */
    fun sendXmlFile(file: File) {
        val out = output ?: return
        if (!file.exists() || !file.isFile) return
        val size = file.length()
        try {
            val header = "FILEXML $size\n".toByteArray(Charsets.UTF_8)
            val buf = ByteArray(DEFAULT_BUFFER)
            FileInputStream(file).use { fis ->
                synchronized(out) {
                    out.write(header)
                    var remaining = size
                    while (remaining > 0) {
                        val read = fis.read(buf, 0, min(buf.size.toLong(), remaining).toInt())
                        if (read <= 0) break
                        out.write(buf, 0, read)
                        remaining -= read
                    }
                    out.flush()
                }
            }
        } catch (_: IOException) {
            closeQuietly()
        }
    }

    /** Cierra y limpia todo sin lanzar. */
    fun clearSession() {
        running.set(false)
        readerJob?.cancel()
        readerJob = null
        closeQuietly()
        onLineMessage = null
        // onXmlReceived lo conservamos por si la pantalla aún lo necesita
    }

    // === Internos ===
    private suspend fun readLoop() = withContext(Dispatchers.IO) {
        val `in` = input ?: return@withContext
        val reader = DataReader(`in`)

        try {
            while (running.get()) {
                // Intentamos leer una línea UTF-8 (terminada en \n)
                val line = reader.readLineUtf8() ?: break // EOF limpio
                if (line.startsWith("FILEXML ")) {
                    // Formato: FILEXML <bytes>
                    val parts = line.split(" ")
                    val size = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                    if (size <= 0L) continue
                    val xmlFile = reader.readFilePayload(size, suffix = ".xml")
                    if (xmlFile != null) {
                        // Saltamos al Main para no pelear con la UI
                        withContext(Dispatchers.Main) {
                            onXmlReceived?.invoke(xmlFile)
                        }
                    } else {
                        // Falló la lectura del payload: cerramos sesión
                        break
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onLineMessage?.invoke(line)
                    }
                }
            }
        } catch (_: EOFException) {
            // peer cerró => salida limpia
        } catch (_: IOException) {
            // error de E/S => cerramos sin crashear
        } catch (_: CancellationException) {
            // cancelado por clearSession()
        } finally {
            // cierre silencioso
            closeQuietly()
        }
    }

    private fun closeQuietly() {
        try { output?.flush() } catch (_: Throwable) {}
        try { input?.close() } catch (_: Throwable) {}
        try { output?.close() } catch (_: Throwable) {}
        try { socket?.close() } catch (_: Throwable) {}
        input = null
        output = null
        socket = null
        running.set(false)
    }

    private const val DEFAULT_BUFFER = 64 * 1024

    /**
     * Pequeño helper para leer líneas y blobs de un InputStream bufferizado
     * sin usar BufferedReader (evitamos conversiones innecesarias).
     */
    private class DataReader(private val input: InputStream) {
        private val lineBuf = ByteArrayOutputStream(256)
        private val one = ByteArray(1)

        /** Devuelve una línea UTF-8 sin el '\n'; null en EOF. */
        @Throws(IOException::class)
        fun readLineUtf8(): String? {
            lineBuf.reset()
            while (true) {
                val n = input.read(one, 0, 1)
                if (n == -1) {
                    // EOF: si tenemos algo acumulado, entregarlo; si no, null
                    return if (lineBuf.size() > 0) {
                        val s = lineBuf.toByteArray().toString(Charsets.UTF_8)
                        s
                    } else null
                }
                val b = one[0]
                if (b == '\n'.code.toByte()) {
                    return lineBuf.toByteArray().toString(Charsets.UTF_8).trimEnd('\r')
                } else {
                    lineBuf.write(b.toInt())
                }
            }
        }

        /**
         * Lee exactamente 'size' bytes a un archivo temporal y lo devuelve.
         * Retorna null si hay EOF prematuro.
         */
        @Throws(IOException::class)
        fun readFilePayload(size: Long, suffix: String): File? {
            val tmp = File.createTempFile("bt_payload_", suffix)
            var remaining = size
            val buf = ByteArray(DEFAULT_BUFFER)
            FileOutputStream(tmp).use { out ->
                while (remaining > 0) {
                    val n = input.read(buf, 0, min(buf.size.toLong(), remaining).toInt())
                    if (n == -1) {
                        // EOF prematuro
                        return null
                    }
                    out.write(buf, 0, n)
                    remaining -= n
                }
                out.flush()
            }
            return tmp
        }
    }
}
