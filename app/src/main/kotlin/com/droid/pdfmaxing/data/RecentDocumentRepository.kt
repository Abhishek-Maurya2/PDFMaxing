package com.droid.pdfmaxing.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droid.pdfmaxing.data.model.RecentDocument
import com.droid.pdfmaxing.ui.reader.PdfViewLayoutMode
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pdf_recent_documents")

class RecentDocumentRepository(private val context: Context) {

    private val recentsKey = stringPreferencesKey("recents_json_list")

    val recentDocuments: Flow<List<RecentDocument>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[recentsKey] ?: "[]"
        parseRecentDocuments(jsonString)
    }

    suspend fun getDocument(uriString: String): RecentDocument? {
        val preferences = context.dataStore.data.first()
        val jsonString = preferences[recentsKey] ?: "[]"
        val list = parseRecentDocuments(jsonString)
        return list.firstOrNull { it.uriString == uriString }
    }

    suspend fun addOrUpdateDocument(document: RecentDocument) {
        context.dataStore.edit { preferences ->
            val currentList = parseRecentDocuments(preferences[recentsKey] ?: "[]").toMutableList()
            // Remove existing entry for the same URI if present
            currentList.removeAll { it.uriString == document.uriString }
            // Add as the latest item at the top
            currentList.add(0, document.copy(lastOpenedTimestamp = System.currentTimeMillis()))
            // Limit to top 50 recent documents
            val trimmed = currentList.take(50)
            preferences[recentsKey] = serializeRecentDocuments(trimmed)
        }
    }

    suspend fun updateDocumentViewState(
        uriString: String,
        page: Int? = null,
        zoomLevel: Float? = null,
        colorMode: PdfColorMode? = null,
        scrollOffset: Int? = null,
        layoutMode: PdfViewLayoutMode? = null,
        touchTimestamp: Boolean = true
    ) {
        context.dataStore.edit { preferences ->
            val currentList = parseRecentDocuments(preferences[recentsKey] ?: "[]").toMutableList()
            val index = currentList.indexOfFirst { it.uriString == uriString }
            if (index != -1) {
                val existing = currentList[index]
                val updated = existing.copy(
                    lastReadPage = page ?: existing.lastReadPage,
                    zoomLevel = zoomLevel ?: existing.zoomLevel,
                    colorMode = colorMode ?: existing.colorMode,
                    scrollOffset = scrollOffset ?: existing.scrollOffset,
                    layoutMode = layoutMode ?: existing.layoutMode,
                    lastOpenedTimestamp = if (touchTimestamp) System.currentTimeMillis() else existing.lastOpenedTimestamp
                )
                currentList[index] = updated
                if (touchTimestamp && index != 0) {
                    currentList.removeAt(index)
                    currentList.add(0, updated)
                }
                preferences[recentsKey] = serializeRecentDocuments(currentList)
            }
        }
    }

    suspend fun updateLastReadPage(uriString: String, page: Int, scrollOffset: Int = 0) {
        updateDocumentViewState(
            uriString = uriString,
            page = page,
            scrollOffset = scrollOffset,
            touchTimestamp = false
        )
    }

    suspend fun updateZoomLevel(uriString: String, zoomLevel: Float) {
        updateDocumentViewState(uriString = uriString, zoomLevel = zoomLevel, touchTimestamp = false)
    }

    suspend fun updateColorMode(uriString: String, colorMode: PdfColorMode) {
        updateDocumentViewState(uriString = uriString, colorMode = colorMode, touchTimestamp = false)
    }

    suspend fun updateLayoutMode(uriString: String, layoutMode: PdfViewLayoutMode, zoomLevel: Float? = null) {
        updateDocumentViewState(
            uriString = uriString,
            layoutMode = layoutMode,
            zoomLevel = zoomLevel,
            touchTimestamp = false
        )
    }

    suspend fun removeDocument(uriString: String) {
        context.dataStore.edit { preferences ->
            val currentList = parseRecentDocuments(preferences[recentsKey] ?: "[]").toMutableList()
            currentList.removeAll { it.uriString == uriString }
            preferences[recentsKey] = serializeRecentDocuments(currentList)
        }
    }

    suspend fun clearRecents() {
        context.dataStore.edit { preferences ->
            preferences.remove(recentsKey)
        }
    }

    companion object {
        const val SAMPLE_GUIDE_URI = "asset:///sample_guide.pdf"

        fun getSampleGuideDocument(): RecentDocument {
            return RecentDocument(
                uriString = SAMPLE_GUIDE_URI,
                title = "PDFMaxing Quick Guide.pdf",
                pageCount = 3,
                lastReadPage = 1,
                fileSizeBytes = 3435L,
                isAsset = true,
                assetPath = "sample_guide.pdf",
                zoomLevel = 1.0f,
                colorMode = PdfColorMode.NORMAL
            )
        }

        fun parseRecentDocuments(json: String): List<RecentDocument> {
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<RecentDocument>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val colorModeStr = obj.optString("colorMode", PdfColorMode.NORMAL.name)
                    val colorMode = try {
                        PdfColorMode.valueOf(colorModeStr)
                    } catch (e: Exception) {
                        PdfColorMode.NORMAL
                    }
                    val layoutModeStr = obj.optString("layoutMode", PdfViewLayoutMode.FIT_100.name)
                    val layoutMode = try {
                        PdfViewLayoutMode.valueOf(layoutModeStr)
                    } catch (e: Exception) {
                        PdfViewLayoutMode.FIT_100
                    }
                    list.add(
                        RecentDocument(
                            uriString = obj.getString("uriString"),
                            title = obj.getString("title"),
                            pageCount = obj.getInt("pageCount"),
                            lastReadPage = obj.optInt("lastReadPage", 1),
                            scrollOffset = obj.optInt("scrollOffset", 0),
                            lastOpenedTimestamp = obj.optLong("lastOpenedTimestamp", System.currentTimeMillis()),
                            fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                            isAsset = obj.optBoolean("isAsset", false),
                            assetPath = if (obj.has("assetPath") && !obj.isNull("assetPath")) obj.getString("assetPath") else null,
                            zoomLevel = obj.optDouble("zoomLevel", 1.0).toFloat(),
                            colorMode = colorMode,
                            layoutMode = layoutMode
                        )
                    )
                }
                list.sortedByDescending { it.lastOpenedTimestamp }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun serializeRecentDocuments(list: List<RecentDocument>): String {
            val array = JSONArray()
            for (doc in list) {
                val obj = JSONObject().apply {
                    put("uriString", doc.uriString)
                    put("title", doc.title)
                    put("pageCount", doc.pageCount)
                    put("lastReadPage", doc.lastReadPage)
                    put("scrollOffset", doc.scrollOffset)
                    put("lastOpenedTimestamp", doc.lastOpenedTimestamp)
                    put("fileSizeBytes", doc.fileSizeBytes)
                    put("isAsset", doc.isAsset)
                    doc.assetPath?.let { put("assetPath", it) }
                    put("zoomLevel", doc.zoomLevel.toDouble())
                    put("colorMode", doc.colorMode.name)
                    put("layoutMode", doc.layoutMode.name)
                }
                array.put(obj)
            }
            return array.toString()
        }
    }
}
