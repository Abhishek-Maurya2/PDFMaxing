package com.droid.pdfmaxing.data.pdf

import android.graphics.Bitmap
import androidx.collection.LruCache

class PdfPageCache(maxMemoryBytes: Int = (Runtime.getRuntime().maxMemory() / 4).toInt().coerceAtLeast(48 * 1024 * 1024)) {

    private val pageBitmapCache = object : LruCache<String, Bitmap>(maxMemoryBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    private val thumbnailCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun getPageBitmap(docId: String, pageIndex: Int, widthBucket: Int): Bitmap? {
        return pageBitmapCache.get("$docId-$pageIndex-$widthBucket")
    }

    fun putPageBitmap(docId: String, pageIndex: Int, widthBucket: Int, bitmap: Bitmap) {
        pageBitmapCache.put("$docId-$pageIndex-$widthBucket", bitmap)
    }

    fun getThumbnail(docId: String, pageIndex: Int): Bitmap? {
        return thumbnailCache.get("$docId-$pageIndex")
    }

    fun putThumbnail(docId: String, pageIndex: Int, bitmap: Bitmap) {
        thumbnailCache.put("$docId-$pageIndex", bitmap)
    }

    fun clear() {
        pageBitmapCache.evictAll()
        thumbnailCache.evictAll()
    }
}
