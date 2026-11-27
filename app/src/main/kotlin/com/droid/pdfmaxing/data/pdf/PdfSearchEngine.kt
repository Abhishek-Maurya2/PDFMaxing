package com.droid.pdfmaxing.data.pdf

import android.graphics.RectF
import android.os.Build
import android.graphics.pdf.PdfRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import java.util.zip.InflaterInputStream

/**
 * Represents a single search match in a PDF document.
 * Bounds are stored in normalized [0.0..1.0] coordinates relative to the page width and height,
 * ensuring resolution-independent rendering that aligns precisely with page content.
 */
data class PdfSearchMatch(
    val pageIndex: Int,
    val matchIndexInPage: Int,
    val bounds: List<RectF> = emptyList(),
    val snippet: String = ""
)

/**
 * Text segment with normalized bounding boxes on the page [0f..1f].
 */
data class PdfTextSegment(
    val text: String,
    val bounds: List<RectF>
)

data class PageSelectionResult(
    val text: String,
    val normalizedBounds: List<RectF>
)

/**
 * Engine for searching text within PDF documents.
 * Supports Android 15+ (API 35+) native PdfRenderer.Page.searchText and getTextContents
 * with an automatic fallback to content stream text parsing for all devices.
 */
object PdfSearchEngine {

    /**
     * Extracts plain text from a single PDF page using API 35+ native getTextContents()
     * when available, returning an empty string if unavailable or on error.
     */
    suspend fun extractPageText(renderer: PdfRenderer, pageIndex: Int): String = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 35) return@withContext ""
        try {
            val page = renderer.openPage(pageIndex)
            try {
                page.textContents.joinToString("\n") { it.text }.trim()
            } finally {
                page.close()
            }
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Extracts text segments with normalized bounds [0f..1f] for direct on-page selection and hover detection.
     */
    suspend fun extractPageTextSegments(renderer: PdfRenderer, pageIndex: Int): List<PdfTextSegment> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 35) return@withContext emptyList()
        try {
            val page = renderer.openPage(pageIndex)
            try {
                val pageWidth = page.width.toFloat().coerceAtLeast(1f)
                val pageHeight = page.height.toFloat().coerceAtLeast(1f)
                val textContents = try {
                    page.textContents
                } catch (_: Throwable) {
                    emptyList()
                }
                textContents.map { content ->
                    val normBounds = content.bounds.map { r ->
                        RectF(
                            (r.left / pageWidth).coerceIn(0f, 1f),
                            (r.top / pageHeight).coerceIn(0f, 1f),
                            (r.right / pageWidth).coerceIn(0f, 1f),
                            (r.bottom / pageHeight).coerceIn(0f, 1f)
                        )
                    }.filter { (it.right - it.left) > 0.0005f && (it.bottom - it.top) > 0.0005f }
                    PdfTextSegment(
                        text = content.text,
                        bounds = normBounds
                    )
                }.filter { it.bounds.isNotEmpty() }
            } finally {
                page.close()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Native selection using PdfRenderer.Page.selectContent on API 35+.
     */
    suspend fun selectContentNative(
        renderer: PdfRenderer,
        pageIndex: Int,
        startNormX: Float,
        startNormY: Float,
        stopNormX: Float,
        stopNormY: Float
    ): PageSelectionResult? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 35) return@withContext null
        try {
            val page = renderer.openPage(pageIndex)
            try {
                val pageWidth = page.width.toFloat().coerceAtLeast(1f)
                val pageHeight = page.height.toFloat().coerceAtLeast(1f)

                val startPoint = android.graphics.Point(
                    (startNormX * pageWidth).toInt().coerceIn(0, page.width),
                    (startNormY * pageHeight).toInt().coerceIn(0, page.height)
                )
                val stopPoint = android.graphics.Point(
                    (stopNormX * pageWidth).toInt().coerceIn(0, page.width),
                    (stopNormY * pageHeight).toInt().coerceIn(0, page.height)
                )

                val startBoundary = android.graphics.pdf.models.selection.SelectionBoundary(startPoint)
                val stopBoundary = android.graphics.pdf.models.selection.SelectionBoundary(stopPoint)

                val pageSelection = page.selectContent(startBoundary, stopBoundary) ?: return@withContext null
                val selectedContents = pageSelection.selectedTextContents ?: emptyList()
                val sb = StringBuilder()
                val allBounds = mutableListOf<RectF>()

                for (content in selectedContents) {
                    sb.append(content.text).append(" ")
                    for (r in content.bounds) {
                        val nb = RectF(
                            (r.left / pageWidth).coerceIn(0f, 1f),
                            (r.top / pageHeight).coerceIn(0f, 1f),
                            (r.right / pageWidth).coerceIn(0f, 1f),
                            (r.bottom / pageHeight).coerceIn(0f, 1f)
                        )
                        if ((nb.right - nb.left) > 0.001f && (nb.bottom - nb.top) > 0.001f) {
                            allBounds.add(nb)
                        }
                    }
                }
                PageSelectionResult(
                    text = sb.toString().trim(),
                    normalizedBounds = allBounds
                )
            } finally {
                page.close()
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Search pages using API 35+ native searchText or getTextContents if available.
     * Bounds are normalized to [0.0..1.0] relative to the page dimensions so highlights
     * align exactly on top of the text without drifting into white spaces or margins.
     * Returns null if API < 35 or if an error occurs, signaling the need for stream fallback.
     */
    fun searchWithNativeApi(
        renderer: PdfRenderer,
        pageIndex: Int,
        query: String
    ): List<PdfSearchMatch>? {
        if (Build.VERSION.SDK_INT < 35) return null
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList()

        return try {
            val page = renderer.openPage(pageIndex)
            try {
                val pageWidth = page.width.toFloat().coerceAtLeast(1f)
                val pageHeight = page.height.toFloat().coerceAtLeast(1f)

                // 1. Primary: native page.searchText
                var rawMatches = try {
                    page.searchText(cleanQuery)
                } catch (e: Throwable) {
                    emptyList()
                }

                // If no matches found with exact query, try case variations
                // in case the native search is case-sensitive on the target device
                val variations = listOf(
                    cleanQuery.lowercase(),
                    cleanQuery.uppercase(),
                    cleanQuery.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                ).distinct().filter { it != cleanQuery }

                for (variation in variations) {
                    if (rawMatches.isNotEmpty()) break
                    try {
                        rawMatches = page.searchText(variation)
                    } catch (_: Throwable) {}
                }

                if (rawMatches.isNotEmpty()) {
                    rawMatches.mapIndexed { idx, matchBounds ->
                        val normalizedBounds = matchBounds.bounds.map { r ->
                            RectF(
                                (r.left / pageWidth).coerceIn(0f, 1f),
                                (r.top / pageHeight).coerceIn(0f, 1f),
                                (r.right / pageWidth).coerceIn(0f, 1f),
                                (r.bottom / pageHeight).coerceIn(0f, 1f)
                            )
                        }.filter { r -> (r.right - r.left) > 0.001f && (r.bottom - r.top) > 0.001f }

                        PdfSearchMatch(
                            pageIndex = pageIndex,
                            matchIndexInPage = idx,
                            bounds = normalizedBounds,
                            snippet = cleanQuery
                        )
                    }
                } else {
                    // 2. Secondary native API check: page.getTextContents()
                    val textContents = try {
                        page.getTextContents()
                    } catch (e: Throwable) {
                        emptyList()
                    }

                    if (textContents.isNotEmpty()) {
                        val results = mutableListOf<PdfSearchMatch>()
                        val lowerQuery = cleanQuery.lowercase()
                        var matchIdx = 0

                        for (content in textContents) {
                            val text = content.text
                            val lowerText = text.lowercase()
                            var startIndex = 0

                            while (startIndex < lowerText.length) {
                                val foundIndex = lowerText.indexOf(lowerQuery, startIndex)
                                if (foundIndex == -1) break

                                val snippetStart = (foundIndex - 20).coerceAtLeast(0)
                                val snippetEnd = (foundIndex + cleanQuery.length + 20).coerceAtMost(text.length)
                                val snippet = text.substring(snippetStart, snippetEnd).trim()

                                val normalizedBounds = content.bounds.map { r ->
                                    RectF(
                                        (r.left / pageWidth).coerceIn(0f, 1f),
                                        (r.top / pageHeight).coerceIn(0f, 1f),
                                        (r.right / pageWidth).coerceIn(0f, 1f),
                                        (r.bottom / pageHeight).coerceIn(0f, 1f)
                                    )
                                }.filter { r -> (r.right - r.left) > 0.001f && (r.bottom - r.top) > 0.001f }

                                results.add(
                                    PdfSearchMatch(
                                        pageIndex = pageIndex,
                                        matchIndexInPage = matchIdx++,
                                        bounds = normalizedBounds,
                                        snippet = snippet
                                    )
                                )
                                startIndex = foundIndex + cleanQuery.length.coerceAtLeast(1)
                            }
                        }
                        results
                    } else {
                        emptyList()
                    }
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
     * Extracts text from (text) Tj, <hex> Tj, [(text) kerning (more)] TJ, ' and " operators,
     * decoding hex sequences, unescaping octal/standard escapes, and decompressing FlateDecode streams.
     */
    suspend fun searchInPdfBytes(
        pdfBytes: ByteArray,
        query: String
    ): List<PdfSearchMatch> = withContext(Dispatchers.Default) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || pdfBytes.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<PdfSearchMatch>()
        val lowerQuery = cleanQuery.lowercase()

        try {
            val isoString = String(pdfBytes, Charsets.ISO_8859_1)

            // 1. First attempt: locate structured page objects via /Type /Page
            val objRegex = Pattern.compile("(\\d+)\\s+(\\d+)\\s+obj(.*?)endobj", Pattern.DOTALL)
            val matcher = objRegex.matcher(isoString)
            val isPagePattern = Pattern.compile("/Type\\s*/Page\\b")

            val pageTexts = mutableListOf<String>()
            val directStreams = mutableListOf<ByteArray>()

            while (matcher.find()) {
                val objBody = matcher.group(3) ?: continue

                // Check if this object contains a direct stream
                if (objBody.contains("stream")) {
                    val objId = matcher.group(1) ?: ""
                    extractStreamData(pdfBytes, objId)?.let { directStreams.add(it) }
                }

                if (!isPagePattern.matcher(objBody).find()) continue

                val contentStreamIds = extractContentStreamIds(objBody)
                val pageTextBuilder = StringBuilder()

                for (cId in contentStreamIds) {
                    val streamData = extractStreamData(pdfBytes, cId) ?: continue
                    val textFromStream = extractTextFromStream(streamData)
                    if (textFromStream.isNotBlank()) {
                        pageTextBuilder.append(textFromStream).append(" ")
                    }
                }

                // If /Contents was a direct stream within the page object
                if (contentStreamIds.isEmpty() && objBody.contains("stream")) {
                    val objId = matcher.group(1) ?: ""
                    extractStreamData(pdfBytes, objId)?.let { sData ->
                        val text = extractTextFromStream(sData)
                        if (text.isNotBlank()) {
                            pageTextBuilder.append(text).append(" ")
                        }
                    }
                }

                pageTexts.add(pageTextBuilder.toString())
            }

            // If structured page objects were found, search within them
            if (pageTexts.isNotEmpty()) {
                for (pageIdx in pageTexts.indices) {
                    val fullPageText = pageTexts[pageIdx]
                    if (fullPageText.isBlank()) continue

                    var startIndex = 0
                    var matchIdx = 0
                    val lowerPageText = fullPageText.lowercase()

                    while (startIndex < lowerPageText.length) {
                        val foundIndex = lowerPageText.indexOf(lowerQuery, startIndex)
                        if (foundIndex == -1) break

                        val snippetStart = (foundIndex - 20).coerceAtLeast(0)
                        val snippetEnd = (foundIndex + cleanQuery.length + 20).coerceAtMost(fullPageText.length)
                        val snippet = fullPageText.substring(snippetStart, snippetEnd).trim()

                        results.add(
                            PdfSearchMatch(
                                pageIndex = pageIdx,
                                matchIndexInPage = matchIdx++,
                                bounds = emptyList(),
                                snippet = snippet
                            )
                        )
                        startIndex = foundIndex + cleanQuery.length.coerceAtLeast(1)
                    }
                }
            }

            // Fallback: If no matches found through structured pages (e.g. compressed /ObjStm or linearized PDF),
            // search across all extracted direct content streams
            if (results.isEmpty() && directStreams.isNotEmpty()) {
                var streamIdx = 0
                for (sData in directStreams) {
                    val text = extractTextFromStream(sData)
                    if (text.isBlank()) {
                        streamIdx++
                        continue
                    }

                    var startIndex = 0
                    var matchIdx = 0
                    val lowerText = text.lowercase()

                    while (startIndex < lowerText.length) {
                        val foundIndex = lowerText.indexOf(lowerQuery, startIndex)
                        if (foundIndex == -1) break

                        val snippetStart = (foundIndex - 20).coerceAtLeast(0)
                        val snippetEnd = (foundIndex + cleanQuery.length + 20).coerceAtMost(text.length)
                        val snippet = text.substring(snippetStart, snippetEnd).trim()

                        results.add(
                            PdfSearchMatch(
                                pageIndex = streamIdx,
                                matchIndexInPage = matchIdx++,
                                bounds = emptyList(),
                                snippet = snippet
                            )
                        )
                        startIndex = foundIndex + cleanQuery.length.coerceAtLeast(1)
                    }
                    streamIdx++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }

    private fun extractContentStreamIds(pageObjStr: String): List<String> {
        val singlePattern = Pattern.compile("/Contents\\s+(\\d+)\\s+(\\d+)\\s+R")
        val singleMatcher = singlePattern.matcher(pageObjStr)
        if (singleMatcher.find()) {
            return listOf(singleMatcher.group(1) ?: return emptyList())
        }

        val arrayPattern = Pattern.compile("/Contents\\s*\\[(.*?)\\]", Pattern.DOTALL)
        val arrayMatcher = arrayPattern.matcher(pageObjStr)
        if (arrayMatcher.find()) {
            val inner = arrayMatcher.group(1) ?: return emptyList()
            val idPattern = Pattern.compile("(\\d+)\\s+(\\d+)\\s+R")
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
        val streamTag = "stream".toByteArray(Charsets.ISO_8859_1)
        val endstreamTag = "endstream".toByteArray(Charsets.ISO_8859_1)

        // Locate the actual PDF indirect object definition ("<objId> <gen> obj") rather than
        // matching "<objId> " anywhere inside numbers or stream bytes.
        val objHeaderRegex = Pattern.compile("(?:^|[\\r\\n\\s])${Pattern.quote(objId)}\\s+\\d+\\s+obj\\b")
        val isoHeaderView = String(pdfBytes, Charsets.ISO_8859_1)
        val headerMatcher = objHeaderRegex.matcher(isoHeaderView)
        if (!headerMatcher.find()) return null

        val objStart = headerMatcher.end()
        val endObjTag = "endobj".toByteArray(Charsets.ISO_8859_1)
        val objEnd = indexOf(pdfBytes, endObjTag, objStart).let { if (it == -1) pdfBytes.size else it }

        var streamStart = indexOf(pdfBytes, streamTag, objStart)
        if (streamStart == -1 || streamStart >= objEnd) return null
        streamStart += streamTag.size

        // Skip newline after 'stream' (\r\n or \n)
        if (streamStart < pdfBytes.size && pdfBytes[streamStart] == '\r'.code.toByte()) streamStart++
        if (streamStart < pdfBytes.size && pdfBytes[streamStart] == '\n'.code.toByte()) streamStart++

        val streamEnd = indexOf(pdfBytes, endstreamTag, streamStart)
        if (streamEnd == -1 || streamEnd < streamStart || streamEnd > objEnd) return null

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

        // 1. Match (text) Tj, (text) ', and (text) "
        val tjPattern = Pattern.compile("\\(((?:[^()\\\\]|\\\\.)*)\\)\\s*(?:Tj|['\"])")
        val tjMatcher = tjPattern.matcher(streamStr)
        while (tjMatcher.find()) {
            tjMatcher.group(1)?.let { raw ->
                sb.append(unescapePdfString(raw)).append(" ")
            }
        }

        // 2. Match <hex> Tj, <hex> ', and <hex> "
        val hexTjPattern = Pattern.compile("<([0-9a-fA-F\\s]+)>\\s*(?:Tj|['\"])")
        val hexTjMatcher = hexTjPattern.matcher(streamStr)
        while (hexTjMatcher.find()) {
            hexTjMatcher.group(1)?.let { rawHex ->
                val decoded = decodePdfHex(rawHex)
                if (decoded.isNotBlank()) sb.append(decoded).append(" ")
            }
        }

        // 3. Match [(text) 20 <hex> -250 (more)] TJ
        val tjArrayPattern = Pattern.compile("\\[(.*?)\\]\\s*TJ", Pattern.DOTALL)
        val tjArrayMatcher = tjArrayPattern.matcher(streamStr)
        while (tjArrayMatcher.find()) {
            val inner = tjArrayMatcher.group(1) ?: continue
            val tokenPattern = Pattern.compile("\\(((?:[^()\\\\]|\\\\.)*)\\)|<([0-9a-fA-F\\s]+)>|(-?\\d+(?:\\.\\d+)?)")
            val tokenMatcher = tokenPattern.matcher(inner)

            while (tokenMatcher.find()) {
                val literalText = tokenMatcher.group(1)
                val hexText = tokenMatcher.group(2)
                val spacingVal = tokenMatcher.group(3)

                when {
                    literalText != null -> {
                        sb.append(unescapePdfString(literalText))
                    }
                    hexText != null -> {
                        sb.append(decodePdfHex(hexText))
                    }
                    spacingVal != null -> {
                        // In PDF font units (usually 1000 units/em), a spacing value <= -100 indicates a word space
                        val num = spacingVal.toDoubleOrNull() ?: 0.0
                        if (num <= -100.0) {
                            sb.append(" ")
                        }
                    }
                }
            }
            sb.append(" ")
        }

        return sb.toString().replace(Regex("\\s+"), " ")
    }

    private fun decodePdfHex(hexStr: String): String {
        val clean = hexStr.replace(Regex("\\s+"), "")
        if (clean.isEmpty()) return ""
        val padded = if (clean.length % 2 != 0) clean + "0" else clean

        val bytes = ByteArray(padded.length / 2)
        for (i in bytes.indices) {
            val byteStr = padded.substring(i * 2, i * 2 + 2)
            bytes[i] = (byteStr.toIntOrNull(16) ?: 0).toByte()
        }

        // Check for UTF-16BE: byte order mark 0xFEFF or initial zero byte
        if (bytes.size >= 2) {
            if ((bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) || (bytes[0] == 0.toByte())) {
                return try {
                    String(bytes, Charsets.UTF_16BE).replace("\u0000", "")
                } catch (e: Exception) {
                    String(bytes, Charsets.ISO_8859_1)
                }
            }
        }
        return String(bytes, Charsets.ISO_8859_1)
    }

    private fun unescapePdfString(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\\' && i + 1 < input.length) {
                val next = input[i + 1]
                if (next in '0'..'7') {
                    // Octal escape sequence: \ddd
                    var octalStr = "$next"
                    var j = i + 2
                    while (j < input.length && j < i + 4 && input[j] in '0'..'7') {
                        octalStr += input[j]
                        j++
                    }
                    val charCode = octalStr.toIntOrNull(8) ?: 32
                    sb.append(charCode.toChar())
                    i = j
                } else {
                    when (next) {
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000c')
                        '\\', '(', ')' -> sb.append(next)
                        else -> sb.append(next)
                    }
                    i += 2
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
