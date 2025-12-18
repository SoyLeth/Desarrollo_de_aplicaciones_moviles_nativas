package com.example.actividad4.storage

import android.content.Context
import android.util.Xml
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class MatchHeader(
    val boardCols: Int,
    val boardRows: Int,
    val seed: Long,
    val playerAName: String,
    val playerBName: String,
    val playerAColor: Long,
    val playerBColor: Long
)

object MatchXml {
    /** Crea un XML nuevo con encabezado y sin jugadas. Devuelve el File. */
    fun writeNewMatch(
        context: Context,
        header: MatchHeader,
        fileName: String = "match_${System.currentTimeMillis()}.xml"
    ): File {
        val dir = File(context.filesDir, "matches")
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, fileName)

        val serializer = Xml.newSerializer()
        FileOutputStream(f).use { fos ->
            serializer.setOutput(fos, "UTF-8")
            serializer.startDocument("UTF-8", true)

            serializer.startTag(null, "match")
            serializer.attribute(null, "version", "1")

            serializer.startTag(null, "header")
            serializer.attribute(null, "cols", header.boardCols.toString())
            serializer.attribute(null, "rows", header.boardRows.toString())
            serializer.attribute(null, "seed", header.seed.toString())
            serializer.attribute(null, "playerAName", header.playerAName)
            serializer.attribute(null, "playerBName", header.playerBName)
            serializer.attribute(null, "playerAColor", header.playerAColor.toString())
            serializer.attribute(null, "playerBColor", header.playerBColor.toString())
            serializer.endTag(null, "header")

            serializer.startTag(null, "moves")
            // vacío al crear
            serializer.endTag(null, "moves")

            serializer.endTag(null, "match")
            serializer.endDocument()
            serializer.flush()
        }
        return f
    }

    /** Agrega una jugada (índice de carta tocada) al XML. */
    fun appendMove(file: File, index: Int) {
        // Leemos encabezado y jugadas actuales, reescribimos + nueva jugada
        val (header, moves) = readMatch(file)
        val newMoves = moves.toMutableList().also { it.add(index) }
        writeWhole(file, header, newMoves)
    }

    /** Lee header + lista de jugadas. */
    fun readMatch(file: File): Pair<MatchHeader, List<Int>> {
        FileInputStream(file).use { fis ->
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(fis, "UTF-8")

            var event = parser.eventType
            var header: MatchHeader? = null
            val moves = mutableListOf<Int>()

            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "header" -> {
                            val cols = parser.getAttributeValue(null, "cols")!!.toInt()
                            val rows = parser.getAttributeValue(null, "rows")!!.toInt()
                            val seed = parser.getAttributeValue(null, "seed")!!.toLong()
                            val aName = parser.getAttributeValue(null, "playerAName") ?: "Jugador A"
                            val bName = parser.getAttributeValue(null, "playerBName") ?: "Jugador B"
                            val aColor = parser.getAttributeValue(null, "playerAColor")!!.toLong()
                            val bColor = parser.getAttributeValue(null, "playerBColor")!!.toLong()
                            header = MatchHeader(cols, rows, seed, aName, bName, aColor, bColor)
                        }
                        "m" -> {
                            val idx = parser.getAttributeValue(null, "i")!!.toInt()
                            moves.add(idx)
                        }
                    }
                }
                event = parser.next()
            }
            return Pair(requireNotNull(header) { "XML inválido: falta <header>" }, moves)
        }
    }

    /** Sobrescribe el XML completo (header + moves). */
    fun writeWhole(file: File, header: MatchHeader, moves: List<Int>) {
        val serializer = Xml.newSerializer()
        FileOutputStream(file, false).use { fos ->
            serializer.setOutput(fos, "UTF-8")
            serializer.startDocument("UTF-8", true)
            serializer.startTag(null, "match")
            serializer.attribute(null, "version", "1")

            serializer.startTag(null, "header")
            serializer.attribute(null, "cols", header.boardCols.toString())
            serializer.attribute(null, "rows", header.boardRows.toString())
            serializer.attribute(null, "seed", header.seed.toString())
            serializer.attribute(null, "playerAName", header.playerAName)
            serializer.attribute(null, "playerBName", header.playerBName)
            serializer.attribute(null, "playerAColor", header.playerAColor.toString())
            serializer.attribute(null, "playerBColor", header.playerBColor.toString())
            serializer.endTag(null, "header")

            serializer.startTag(null, "moves")
            moves.forEach { idx ->
                serializer.startTag(null, "m")
                serializer.attribute(null, "i", idx.toString())
                serializer.endTag(null, "m")
            }
            serializer.endTag(null, "moves")

            serializer.endTag(null, "match")
            serializer.endDocument()
            serializer.flush()
        }
    }
}
