package com.restor.gamereel.feature.ideas

/** A portable image card; the full, unabridged idea is available in the text export. */
internal fun ideaSvg(text: String): String {
    fun escaped(value: String) = value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;")
    val lines = text.lines().flatMap { source ->
        if (source.isBlank()) listOf("") else {
            val result = mutableListOf<String>()
            var current = ""
            source.split(' ').forEach { word ->
                if (current.isNotEmpty() && current.length + word.length + 1 > 45) {
                    result += current
                    current = word
                } else current = if (current.isEmpty()) word else "$current $word"
            }
            result + current
        }
    }.take(40)
    val height = (200 + lines.size * 44).coerceAtLeast(480)
    return buildString {
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1080\" height=\"$height\" viewBox=\"0 0 1080 $height\">")
        append("<rect width=\"1080\" height=\"$height\" rx=\"48\" fill=\"#202838\"/>")
        append("<rect x=\"52\" y=\"52\" width=\"8\" height=\"${height - 104}\" fill=\"#C5AEFF\"/>")
        append("<text x=\"92\" y=\"98\" fill=\"#C5AEFF\" font-family=\"sans-serif\" font-size=\"24\">GameReel</text>")
        lines.forEachIndexed { index, line ->
            val size = if (index == 0) 36 else 27
            val color = if (index == 0) "#FFFFFF" else "#E8DFFF"
            append("<text x=\"92\" y=\"${164 + index * 44}\" fill=\"$color\" font-family=\"sans-serif\" font-size=\"$size\">")
            append(escaped(line))
            append("</text>")
        }
        append("</svg>")
    }
}
