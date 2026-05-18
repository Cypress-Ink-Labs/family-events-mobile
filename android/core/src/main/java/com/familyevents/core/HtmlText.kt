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
        val decoded: String? = when {
            entity == "amp" -> "&"
            entity == "lt" -> "<"
            entity == "gt" -> ">"
            entity == "quot" -> "\""
            entity == "apos" -> "'"
            entity == "nbsp" -> " "
            entity.startsWith("#x") || entity.startsWith("#X") -> {
                val hex = entity.substring(2)
                if (hex.isEmpty()) null
                else hex.toLongOrNull(16)?.let { cp ->
                    if (cp in 0..0x10FFFF && cp !in 0xD800..0xDFFF) String(Character.toChars(cp.toInt())) else null
                }
            }
            entity.startsWith("#") -> {
                val dec = entity.substring(1)
                if (dec.isEmpty()) null
                else dec.toLongOrNull(10)?.let { cp ->
                    if (cp in 0..0x10FFFF && cp !in 0xD800..0xDFFF) String(Character.toChars(cp.toInt())) else null
                }
            }
            else -> null
        }
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
