package com.droid.pdfmaxing.ui.reader

enum class PdfViewLayoutMode(val displayName: String) {
    FIT_100("Fit 100%"),
    FIT_50("50% Overview"),
    TWO_PAGE("Two-Page Spread");

    fun next(): PdfViewLayoutMode = when (this) {
        FIT_100 -> FIT_50
        FIT_50 -> TWO_PAGE
        TWO_PAGE -> FIT_100
    }
}

object PdfTwoPageHelper {
    /**
     * Splits total pages into pairs for side-by-side spread viewing:
     * e.g. for 4 pages: [(0, 1), (2, 3)]
     * e.g. for 3 pages: [(0, 1), (2, null)]
     */
    fun createPagePairs(totalPageCount: Int): List<Pair<Int, Int?>> {
        if (totalPageCount <= 0) return emptyList()
        val pairs = mutableListOf<Pair<Int, Int?>>()
        var i = 0
        while (i < totalPageCount) {
            val left = i
            val right = if (i + 1 < totalPageCount) i + 1 else null
            pairs.add(Pair(left, right))
            i += 2
        }
        return pairs
    }
}
