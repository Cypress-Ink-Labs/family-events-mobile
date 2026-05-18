package com.familyevents.core

/**
 * Decode common HTML entities in [input] without an XML parser.
 * Returns [input] unchanged when it contains no entities.
 * Handles named entities (amp, lt, gt, quot, apos, nbsp), decimal `&#NNNN;`,
 * and hex `&#xHHHH;`. Malformed entities pass through verbatim.
 */
fun decodeHtmlEntities(input: String): String {
    if (input.isEmpty() || '&' !in input) return input

    val sb = StringBuilder(input.length)
    var i = 0
    while (i < input.length) {
        val ch = input[i]
        if (ch != '&') {
            sb.append(ch)
            i++
            continue
        }
        val semi = input.indexOf(';', i + 1)
        if (semi == -1) {
            sb.append(ch)
            i++
            continue
        }
        val entity = input.substring(i + 1, semi)
        val decoded = decodeNamedEntity(entity) ?: decodeNumericEntity(entity)
        if (decoded != null) {
            sb.append(decoded)
            i = semi + 1
        } else {
            sb.append(ch)
            i++
        }
    }
    return sb.toString()
}

private fun decodeNamedEntity(entity: String): String? = when (entity) {
    "amp" -> "&"
    "lt" -> "<"
    "gt" -> ">"
    "quot" -> "\""
    "apos" -> "'"
    "nbsp" -> " "
    else -> null
}

private fun decodeNumericEntity(entity: String): String? = when {
    entity.startsWith("#x") || entity.startsWith("#X") -> {
        val hex = entity.substring(2)
        if (hex.isEmpty()) null else hex.toLongOrNull(16)?.let(::validateCodePoint)
    }
    entity.startsWith("#") -> {
        val dec = entity.substring(1)
        if (dec.isEmpty()) null else dec.toLongOrNull(10)?.let(::validateCodePoint)
    }
    else -> null
}

private fun validateCodePoint(cp: Long): String? =
    if (cp in 0L..0x10FFFFL && cp !in 0xD800L..0xDFFFL) {
        String(Character.toChars(cp.toInt()))
    } else {
        null
    }
