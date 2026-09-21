package pro.botforge.lettermark

import java.text.Normalizer

/**
 * The letters drawn in the square for [name], as a reader of that writing system expects them.
 *
 * The result is empty when the name holds nothing drawable; the caller then paints the square and
 * leaves it blank rather than inventing a placeholder character.
 */
public fun initials(name: String): String {
    val normalized = Normalizer.normalize(name, Normalizer.Form.NFC)

    val units = mutableListOf<String>()
    for (word in wordsOf(normalized)) {
        val unit = firstSyllableOf(word) ?: continue
        units.add(upperKeepingLength(unit))
        if (units.size == 2 || writesAsOneSyllableWhole(unit)) break
    }
    if (units.isEmpty()) {
        return Normalizer.normalize(firstDrawableCluster(normalized), Normalizer.Form.NFC)
    }
    return Normalizer.normalize(units.joinToString(""), Normalizer.Form.NFC)
}

/**
 * How many colours the caller paints, and which of them a thing is drawn on.
 *
 * The colours themselves live with the rest of the design — a theme, a colour resource file, a
 * stylesheet — and this type only says which of them to reach for.
 *
 * A count below 1 means the code and the theme disagree, which is a broken invariant rather than a
 * value with an answer, so building one stops the program instead of handing out a slot no colour
 * is painted for.
 */
public class Palette(
    public val slots: Int,
) {
    init {
        require(slots >= 1) { "lettermark: a palette of $slots colours has no slot to hand out" }
    }

    /**
     * Which colour [id] is drawn on, from `0` to `slots - 1`.
     *
     * The same id always answers the same slot, so a thing keeps its colour between screens and
     * between runs — for as long as the palette holds the same number of colours. Painting one
     * more or one fewer moves almost everything to a different colour, which readers notice.
     */
    public fun slot(id: Long): Int {
        val slots = slots.toLong()
        return (((id % slots) + slots) % slots).toInt()
    }
}

private fun wordsOf(name: String): List<String> {
    val out = mutableListOf<String>()
    val current = StringBuilder()
    var index = 0
    while (index < name.length) {
        val code = name.codePointAt(index)
        if (isWordBreak(code)) {
            if (current.isNotEmpty()) {
                out.add(current.toString())
                current.setLength(0)
            }
        } else {
            current.appendCodePoint(code)
        }
        index += Character.charCount(code)
    }
    if (current.isNotEmpty()) out.add(current.toString())
    return out
}

private fun isWordBreak(code: Int): Boolean =
    when (code) {
        0x0085, 0x00A0, 0x1680, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000 -> true
        else -> code in 0x0009..0x000D || code == 0x0020 || code in 0x2000..0x200A
    }

private fun firstSyllableOf(word: String): String? {
    val clusters = clustersOf(word)
    for ((index, cluster) in clusters.withIndex()) {
        if (!opensAUnit(cluster.codePointAt(0))) continue
        return grownIntoASyllable(cluster, clusters.subList(index + 1, clusters.size))
    }
    return null
}

private fun grownIntoASyllable(
    start: String,
    rest: List<String>,
): String {
    var unit = start
    var index = 0
    while (index < rest.size) {
        if (!isVirama(lastCodePoint(unit)) && !isPrefixVowel(unit)) return unit
        unit += rest[index]
        index++
    }
    return unit
}

private fun isPrefixVowel(unit: String): Boolean {
    if (unit.codePointCount(0, unit.length) != 1) return false
    val code = unit.codePointAt(0)
    return code in 0x0E40..0x0E44 || code in 0x0EC0..0x0EC4
}

private fun isVirama(code: Int): Boolean =
    when (code) {
        0x094D, 0x09CD, 0x0A4D, 0x0ACD, 0x0B4D, 0x0BCD, 0x0C4D, 0x0CCD,
        0x0D4D, 0x0DCA, 0x0F84, 0x1039, 0x17D2,
        -> true
        else -> false
    }

private fun opensAUnit(code: Int): Boolean =
    when (Character.getType(code).toByte()) {
        Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
        Character.OTHER_LETTER, Character.DECIMAL_DIGIT_NUMBER,
        -> true
        else -> false
    }

private fun writesAsOneSyllableWhole(unit: String): Boolean =
    when (unit.codePointAt(0)) {
        in 0x0600..0x06FF, in 0x0700..0x074F, in 0x0750..0x077F, in 0x07C0..0x07FF,
        in 0x0860..0x086F, in 0x08A0..0x08FF, in 0xFB50..0xFDFF, in 0xFE70..0xFEFF,
        in 0x1E900..0x1E95F,
        in 0x1100..0x11FF, in 0x3130..0x318F, in 0xA960..0xA97F, in 0xAC00..0xD7FF,
        in 0xFFA0..0xFFDC,
        in 0x2E80..0x2EFF, in 0x3400..0x4DBF, in 0x4E00..0x9FFF, in 0xF900..0xFAFF,
        in 0x20000..0x3134F,
        in 0x3040..0x30FF, in 0x31F0..0x31FF, in 0xFF66..0xFF9F,
        in 0x1800..0x18AF,
        -> true
        else -> false
    }

private fun upperKeepingLength(unit: String): String {
    if (isGeorgianMkhedruli(unit.codePointAt(0))) return unit
    val uppercased = unit.uppercase()
    if (uppercased.codePointCount(0, uppercased.length) > unit.codePointCount(0, unit.length)) {
        return unit
    }
    return uppercased
}

private fun isGeorgianMkhedruli(code: Int): Boolean = code in 0x10D0..0x10FF

private fun firstDrawableCluster(name: String): String {
    for (cluster in clustersOf(name)) {
        val code = cluster.codePointAt(0)
        if (isWordBreak(code) || isMarkOrControl(code)) continue
        return cluster
    }
    return ""
}

private fun isMarkOrControl(code: Int): Boolean =
    when (Character.getType(code).toByte()) {
        Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK,
        Character.CONTROL, Character.FORMAT, Character.SURROGATE, Character.PRIVATE_USE,
        Character.UNASSIGNED,
        -> true
        else -> false
    }

private fun clustersOf(text: String): List<String> = Graphemes.clusters(text)

private fun lastCodePoint(unit: String): Int = unit.codePointBefore(unit.length)
