package com.bookyapa.app.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints

object Paginator {

    fun computePages(
        textMeasurer: TextMeasurer,
        text: String,
        style: TextStyle,
        constraints: Constraints,
    ): List<String> {
        if (text.isBlank()) return listOf(text)

        val result = textMeasurer.measure(
            text = AnnotatedString(text),
            style = style,
            constraints = constraints.copy(maxHeight = Constraints.Infinity),
        )

        if (result.lineCount == 0) return listOf("")

        val lineHeight = result.getLineBottom(0) - result.getLineTop(0)
        if (lineHeight <= 0f) return listOf(text)

        val availableHeight = constraints.maxHeight.toFloat()
        val maxLinesPerPage = (availableHeight / lineHeight).toInt().coerceAtLeast(1)

        val pages = mutableListOf<String>()
        var currentLine = 0
        while (currentLine < result.lineCount) {
            val endLine = minOf(currentLine + maxLinesPerPage, result.lineCount)
            val charStart = result.getLineStart(currentLine)
            val charEnd = result.getLineEnd(endLine - 1)
            pages.add(text.substring(charStart, charEnd))
            currentLine = endLine
        }
        return pages
    }
}
