package pro.botforge.lettermark

internal object Graphemes {
    const val OTHER = 0
    const val CR = 1
    const val LF = 2
    const val CONTROL = 3
    const val EXTEND = 4
    const val ZWJ = 5
    const val REGIONAL_INDICATOR = 6
    const val PREPEND = 7
    const val SPACING_MARK = 8
    const val HANGUL_L = 9
    const val HANGUL_V = 10
    const val HANGUL_T = 11
    const val HANGUL_LV = 12
    const val HANGUL_LVT = 13

    const val EXT_PICT = 0x10
    const val INCB_LINKER = 0x20
    const val INCB_CONSONANT = 0x40
    const val INCB_EXTEND = 0x80

    private val starts: IntArray
    private val ends: IntArray
    private val values: IntArray

    /** The Unicode version whose segmentation this port answers by. */
    val unicodeVersion: String

    init {
        val bytes =
            requireNotNull(Graphemes::class.java.getResourceAsStream("/graphemes.bin")) {
                "the grapheme table is not on the classpath; run `make unicode-sync`"
            }.use { it.readBytes() }

        var cursor = bytes.indexOf('\n'.code.toByte())
        unicodeVersion = String(bytes, 0, cursor, Charsets.US_ASCII)
        cursor++

        val startList = ArrayList<Int>(2048)
        val endList = ArrayList<Int>(2048)
        val valueList = ArrayList<Int>(2048)
        var previousEnd = -1
        while (cursor < bytes.size) {
            var gap = 0
            var shift = 0
            while (true) {
                val byte = bytes[cursor++].toInt()
                gap = gap or ((byte and 0x7F) shl shift)
                if (byte and 0x80 == 0) break
                shift += 7
            }
            var span = 0
            shift = 0
            while (true) {
                val byte = bytes[cursor++].toInt()
                span = span or ((byte and 0x7F) shl shift)
                if (byte and 0x80 == 0) break
                shift += 7
            }
            val start = previousEnd + 1 + gap
            val end = start + span
            startList.add(start)
            endList.add(end)
            valueList.add(bytes[cursor++].toInt() and 0xFF)
            previousEnd = end
        }
        starts = startList.toIntArray()
        ends = endList.toIntArray()
        values = valueList.toIntArray()
    }

    fun propertiesOf(code: Int): Int {
        var low = 0
        var high = starts.size - 1
        while (low <= high) {
            val middle = (low + high) ushr 1
            when {
                code < starts[middle] -> high = middle - 1
                code > ends[middle] -> low = middle + 1
                else -> return values[middle]
            }
        }
        return OTHER
    }

    /** The text split into extended grapheme clusters, by the rules of [unicodeVersion]. */
    fun clusters(text: String): List<String> {
        if (text.isEmpty()) return emptyList()

        val out = ArrayList<String>(text.length)
        val state = Scan()
        var clusterStart = 0
        var index = 0
        while (index < text.length) {
            val code = text.codePointAt(index)
            val width = Character.charCount(code)
            if (index > 0 && state.breaksBefore(code)) {
                out.add(text.substring(clusterStart, index))
                clusterStart = index
            }
            state.accept(code)
            index += width
        }
        out.add(text.substring(clusterStart))
        return out
    }

    private class Scan {
        private var previous = -1
        private var regionalIndicators = 0
        private var pictographic = 0
        private var consonant = false
        private var linker = false

        fun breaksBefore(code: Int): Boolean {
            val before = previous
            val now = propertiesOf(code)
            val beforeClass = before and 0x0F
            val nowClass = now and 0x0F

            if (beforeClass == CR && nowClass == LF) return false
            if (beforeClass == CR || beforeClass == LF || beforeClass == CONTROL) return true
            if (nowClass == CR || nowClass == LF || nowClass == CONTROL) return true

            if (beforeClass == HANGUL_L &&
                (nowClass == HANGUL_L || nowClass == HANGUL_V || nowClass == HANGUL_LV || nowClass == HANGUL_LVT)
            ) {
                return false
            }
            if ((beforeClass == HANGUL_LV || beforeClass == HANGUL_V) &&
                (nowClass == HANGUL_V || nowClass == HANGUL_T)
            ) {
                return false
            }
            if ((beforeClass == HANGUL_LVT || beforeClass == HANGUL_T) && nowClass == HANGUL_T) return false

            if (nowClass == EXTEND || nowClass == ZWJ) return false
            if (nowClass == SPACING_MARK) return false
            if (beforeClass == PREPEND) return false

            if (consonant && linker && now and INCB_CONSONANT != 0) return false
            if (pictographic == AFTER_ZWJ && now and EXT_PICT != 0) return false
            if (nowClass == REGIONAL_INDICATOR && regionalIndicators % 2 == 1) return false

            return true
        }

        fun accept(code: Int) {
            val now = propertiesOf(code)
            val nowClass = now and 0x0F

            regionalIndicators = if (nowClass == REGIONAL_INDICATOR) regionalIndicators + 1 else 0

            pictographic =
                when {
                    pictographic == AFTER_PICTOGRAPH && nowClass == EXTEND -> AFTER_PICTOGRAPH
                    pictographic == AFTER_PICTOGRAPH && nowClass == ZWJ -> AFTER_ZWJ
                    now and EXT_PICT != 0 -> AFTER_PICTOGRAPH
                    else -> NONE
                }

            when {
                now and INCB_CONSONANT != 0 -> {
                    consonant = true
                    linker = false
                }
                consonant && now and INCB_LINKER != 0 -> linker = true
                consonant && now and INCB_EXTEND != 0 -> Unit
                else -> {
                    consonant = false
                    linker = false
                }
            }

            previous = now
        }

        private companion object {
            const val NONE = 0
            const val AFTER_PICTOGRAPH = 1
            const val AFTER_ZWJ = 2
        }
    }
}
