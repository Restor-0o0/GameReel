package com.restor.gamereel.feature.search

internal fun fuzzySearchMatch(text: String, query: String): Boolean {
    return fuzzySearchMatchNormalized(text.searchNormalized(), query.searchWords())
}

internal fun fuzzySearchMatchNormalized(normalizedText: String, needles: List<String>): Boolean {
    if (needles.isEmpty()) return true
    // Most queries are exact substrings. Only split the candidate when a word-prefix
    // or typo match is actually needed.
    var words: List<String>? = null

    return needles.all { needle ->
        needle in normalizedText ||
            (words ?: normalizedText.split(' ').also { words = it }).any { word ->
                word.startsWith(needle) ||
                    (needle.length >= 4 && editDistanceWithin(word, needle, typoLimit(needle)))
            }
    }
}

internal fun String.searchWords(): List<String> =
    searchNormalized().split(' ').filter { it.isNotEmpty() }

internal fun String.searchNormalized(): String = buildString(length) {
    var pendingSpace = false
    for (char in this@searchNormalized.lowercase()) {
        if (char.isLetterOrDigit()) {
            if (pendingSpace && isNotEmpty()) append(' ')
            append(if (char == 'ё') 'е' else char)
            pendingSpace = false
        } else {
            pendingSpace = true
        }
    }
}

private fun typoLimit(word: String): Int = if (word.length >= 7) 2 else 1

private fun editDistanceWithin(left: String, right: String, limit: Int): Boolean {
    if (kotlin.math.abs(left.length - right.length) > limit) return false
    var previous = IntArray(right.length + 1) { it }
    for (leftIndex in left.indices) {
        val current = IntArray(right.length + 1)
        current[0] = leftIndex + 1
        var rowMinimum = current[0]
        for (rightIndex in right.indices) {
            current[rightIndex + 1] =
                minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + if (left[leftIndex] == right[rightIndex]) 0 else 1,
                )
            rowMinimum = minOf(rowMinimum, current[rightIndex + 1])
        }
        if (rowMinimum > limit) return false
        previous = current
    }
    return previous[right.length] <= limit
}
