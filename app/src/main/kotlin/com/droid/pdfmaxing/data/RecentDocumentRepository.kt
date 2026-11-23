package com.droid.pdfmaxing.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droid.pdfmaxing.data.model.RecentDocument
import kotlinx.coroutines.flow.Flow
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

    suspend fun updateLastReadPage(uriString: String, page: Int, scrollOffset: Int = 0) {
        context.dataStore.edit { preferences ->
            val currentList = parseRecentDocuments(preferences[recentsKey] ?: "[]").toMutableList()
            val index = currentList.indexOfFirst { it.uriString == uriString }
            if (index != -1) {
                val existing = currentList[index]
                currentList[index] = existing.copy(
                    lastReadPage = page,
                    scrollOffset = scrollOffset,
                    lastOpenedTimestamp = System.currentTimeMillis()
                )
                // Move updated document to the top
                val updated = currentList.removeAt(index)
                currentList.add(0, updated)
                preferences[recentsKey] = serializeRecentDocuments(currentList)
            }
        }
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
                assetPath = "sample_guide.pdf"
            )
        }

        private fun parseRecentDocuments(json: String): List<RecentDocument> {
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<RecentDocument>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
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
                            assetPath = if (obj.has("assetPath")) obj.getString("assetPath") else null
                        )
                    )
                }
                list.sortedByDescending { it.lastOpenedTimestamp }
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun serializeRecentDocuments(list: List<RecentDocument>): String {
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
                }
                array.put(obj)
            }
            return array.toString()
        }
    }
}
