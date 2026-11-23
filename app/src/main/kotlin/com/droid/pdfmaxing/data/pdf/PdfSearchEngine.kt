package com.droid.pdfmaxing.data.pdf

import android.graphics.RectF
import android.os.Build
import android.graphics.pdf.PdfRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.regex.Pattern
import java.util.zip.InflaterInputStream

/**
 * Represents a single search match in a PDF document.
 */
data class PdfSearchMatch(
    val pageIndex: Int,
    val matchIndexInPage: Int,
    val bounds: List<RectF> = emptyList(),
    val snippet: String = ""
)

/**
 * Engine for searching text within PDF documents.
 * Supports Android 15+ (API 35+) native PdfRenderer.Page.searchText
 * with an automatic fallback to content stream text parsing for all devices.
 */
object PdfSearchEngine {

    /**
     * Search pages using API 35+ native searchText if available.
     * Returns null if API < 35 or if an error occurs, signaling the need for stream fallback.
     */
    fun searchWithNativeApi(
        renderer: PdfRenderer,
        pageIndex: Int,
        query: String
    ): List<PdfSearchMatch>? {
        if (Build.VERSION.SDK_INT < 35) return null
        return try {
            val page = renderer.openPage(pageIndex)
            try {
                val matches = page.searchText(query)
                matches.mapIndexed { idx, matchBounds ->
                    PdfSearchMatch(
                        pageIndex = pageIndex,
                        matchIndexInPage = idx,
                        bounds = matchBounds.bounds,
                        snippet = query
                    )
                }
            } finally {
                page.close()
            }
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Fallback text parser that scans PDF content streams directly.
     * Extracts text from (text) Tj and [(t) (e) (x) (t)] TJ operators,
     * decompressing FlateDecode streams via standard zlib InflaterInputStream.
     */
    suspend fun searchInPdfBytes(
        pdfBytes: ByteArray,
        query: String
    ): List<PdfSearchMatch> = withContext(Dispatchers.Default) {
        if (query.isBlank() || pdfBytes.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<PdfSearchMatch>()
        val lowerQuery = query.lowercase()

        try {
            // Locate individual objects: (\d+)\s+0\s+obj(.*?)endobj
            val objRegex = Pattern.compile("(\\d+)\\s+0\\s+obj(.*?)endobj", Pattern.DOTALL)
            val matcher = objRegex.matcher(String(pdfBytes, Charsets.ISO_8859_1))
            val isPagePattern = Pattern.compile("/Type\\s*/Page\\b")

            var pageIdx = 0
            while (matcher.find()) {
                val objBody = matcher.group(2) ?: continue
                if (!isPagePattern.matcher(objBody).find()) continue

                val contentStreamIds = extractContentStreamIds(objBody)
                val pageTextBuilder = StringBuilder()

                for (cId in contentStreamIds) {
                    val streamData = extractStreamData(pdfBytes, cId) ?: continue
                    val textFromStream = extractTextFromStream(streamData)
                    pageTextBuilder.append(textFromStream).append(" ")
                }

                val fullPageText = pageTextBuilder.toString()
                if (fullPageText.isNotEmpty()) {
                    var startIndex = 0
                    var matchIdx = 0
                    val lowerPageText = fullPageText.lowercase()

                    while (startIndex < lowerPageText.length) {
                        val foundIndex = lowerPageText.indexOf(lowerQuery, startIndex)
                        if (foundIndex == -1) break

                        val snippetStart = (foundIndex - 20).coerceAtLeast(0)
                        val snippetEnd = (foundIndex + query.length + 20).coerceAtMost(fullPageText.length)
                        val snippet = fullPageText.substring(snippetStart, snippetEnd).trim()

                        results.add(
                            PdfSearchMatch(
                                pageIndex = pageIdx,
                                matchIndexInPage = matchIdx,
                                bounds = emptyList(),
                                snippet = snippet
                            )
                        )
                        matchIdx++
                        startIndex = foundIndex + query.length.coerceAtLeast(1)
                    }
                }
                pageIdx++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }

    private fun extractContentStreamIds(pageObjStr: String): List<String> {
        val singlePattern = Pattern.compile("/Contents\\s+(\\d+)\\s+0\\s+R")
        val singleMatcher = singlePattern.matcher(pageObjStr)
        if (singleMatcher.find()) {
            return listOf(singleMatcher.group(1) ?: return emptyList())
        }

        val arrayPattern = Pattern.compile("/Contents\\s*\\[(.*?)\\]", Pattern.DOTALL)
        val arrayMatcher = arrayPattern.matcher(pageObjStr)
        if (arrayMatcher.find()) {
            val inner = arrayMatcher.group(1) ?: return emptyList()
            val idPattern = Pattern.compile("(\\d+)\\s+0\\s+R")
            val idMatcher = idPattern.matcher(inner)
            val ids = mutableListOf<String>()
            while (idMatcher.find()) {
                idMatcher.group(1)?.let { ids.add(it) }
            }
            return ids
        }

        return emptyList()
    }

    private fun extractStreamData(pdfBytes: ByteArray, objId: String): ByteArray? {
        val target = "$objId 0 obj".toByteArray(Charsets.ISO_8859_1)
        val streamTag = "stream".toByteArray(Charsets.ISO_8859_1)
        val endstreamTag = "endstream".toByteArray(Charsets.ISO_8859_1)

        val objStart = indexOf(pdfBytes, target, 0)
        if (objStart == -1) return null

        var streamStart = indexOf(pdfBytes, streamTag, objStart)
        if (streamStart == -1) return null
        streamStart += streamTag.size

        // Skip newline after 'stream' (\r\n or \n)
        if (streamStart < pdfBytes.size && pdfBytes[streamStart] == '\r'.code.toByte()) streamStart++
        if (streamStart < pdfBytes.size && pdfBytes[streamStart] == '\n'.code.toByte()) streamStart++

        val streamEnd = indexOf(pdfBytes, endstreamTag, streamStart)
        if (streamEnd == -1 || streamEnd < streamStart) return null

        val rawData = pdfBytes.copyOfRange(streamStart, streamEnd)

        // Attempt FlateDecode decompression; if not compressed or fails, return raw
        return try {
            InflaterInputStream(rawData.inputStream()).use { it.readBytes() }
        } catch (e: Exception) {
            rawData
        }
    }

    private fun extractTextFromStream(streamData: ByteArray): String {
        val streamStr = String(streamData, Charsets.ISO_8859_1)
        val sb = StringBuilder()

        // 1. Match (text) Tj
        val tjPattern = Pattern.compile("\\(((?:[^()\\\\]|\\\\.)*)\\)\\s*Tj")
        val tjMatcher = tjPattern.matcher(streamStr)
        while (tjMatcher.find()) {
            tjMatcher.group(1)?.let { raw ->
                sb.append(unescapePdfString(raw)).append(" ")
            }
        }

        // 2. Match [(text) 20 (more)] TJ
        val tjArrayPattern = Pattern.compile("\\[(.*?)\\]\\s*TJ", Pattern.DOTALL)
        val tjArrayMatcher = tjArrayPattern.matcher(streamStr)
        while (tjArrayMatcher.find()) {
            val inner = tjArrayMatcher.group(1) ?: continue
            val partPattern = Pattern.compile("\\(((?:[^()\\\\]|\\\\.)*)\\)")
            val partMatcher = partPattern.matcher(inner)
            while (partMatcher.find()) {
                partMatcher.group(1)?.let { raw ->
                    sb.append(unescapePdfString(raw))
                }
            }
            sb.append(" ")
        }

        return sb.toString()
    }

    private fun unescapePdfString(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length) {
                when (val next = input[i + 1]) {
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000c'); i += 2 }
                    '\\', '(', ')' -> { sb.append(next); i += 2 }
                    else -> { sb.append(next); i += 2 }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun indexOf(source: ByteArray, target: ByteArray, start: Int): Int {
        if (target.isEmpty() || start >= source.size) return -1
        outer@ for (i in start..(source.size - target.size)) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
