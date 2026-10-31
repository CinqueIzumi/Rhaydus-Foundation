package nl.rhaydus.buildlogic

/**
 * Finds comment text in a single source line. Both extractors return the text after the first comment
 * marker that sits outside a string literal, or null when the line has none; strings never span lines.
 */
internal object DocCommentExtractors {
    /**
     * Kotlin line-comment, block-comment and KDoc markers. A char literal is tracked apart from a string literal so a quote
     * character such as `'"'` cannot open a string that swallows the rest of the line. A line that already
     * starts with a comment marker or `*` is a whole-line or KDoc-continuation comment.
     */
    fun kotlinStyle(line: String): String? {
        val trimmed = line.trimStart()

        if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/**")) return trimmed

        var inString = false
        var inChar = false
        var index = 0

        while (index < line.length) {
            val char = line[index]

            when {
                inString && char == '\\' -> index += 2
                inString && char == '"' -> {
                    inString = false
                    index += 1
                }

                inString -> index += 1
                inChar && char == '\\' -> index += 2
                inChar && char == '\'' -> {
                    inChar = false
                    index += 1
                }

                inChar -> index += 1
                char == '"' -> {
                    inString = true
                    index += 1
                }

                char == '\'' -> {
                    inChar = true
                    index += 1
                }

                char == '/' && index + 1 < line.length && (line[index + 1] == '/' || line[index + 1] == '*') ->
                    return line.substring(index + 2)

                else -> index += 1
            }
        }

        return null
    }

    /**
     * A YAML `#` at the line start or after a space or tab, outside a quoted scalar. A single-quoted scalar
     * escapes a quote by doubling it; a double-quoted one escapes with a backslash.
     */
    fun yamlStyle(line: String): String? {
        var quote: Char? = null
        var index = 0

        while (index < line.length) {
            val char = line[index]

            when (quote) {
                '\'' -> when {
                    char != '\'' -> index += 1
                    index + 1 < line.length && line[index + 1] == '\'' -> index += 2
                    else -> {
                        quote = null
                        index += 1
                    }
                }

                '"' -> when {
                    char == '\\' -> index += 2
                    char == '"' -> {
                        quote = null
                        index += 1
                    }

                    else -> index += 1
                }

                else -> when {
                    char == '\'' || char == '"' -> {
                        quote = char
                        index += 1
                    }

                    char == '#' && (index == 0 || line[index - 1] == ' ' || line[index - 1] == '\t') ->
                        return line.substring(index + 1)

                    else -> index += 1
                }
            }
        }

        return null
    }
}
