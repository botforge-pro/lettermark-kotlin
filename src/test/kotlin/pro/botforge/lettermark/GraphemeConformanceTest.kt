package pro.botforge.lettermark

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class GraphemeConformanceTest {
    private val cases: List<ConformanceCase> by lazy {
        val text =
            requireNotNull(this::class.java.getResourceAsStream("/GraphemeBreakTest.txt")) {
                "the conformance file is not on the classpath"
            }.use { it.readBytes().decodeToString() }
        text.lineSequence().mapNotNull(::parse).toList()
    }

    @Test
    fun theTableAndTheSuiteDescribeTheSameUnicode() {
        val header = cases.let { requireNotNull(suiteVersion()) }
        assertThat(Graphemes.unicodeVersion)
            .describedAs(
                "the table was generated from one Unicode version and the conformance suite " +
                    "comes from another, so a passing run would prove nothing",
            ).isEqualTo(header)
    }

    @TestFactory
    fun everyCaseTheStandardNames(): Collection<DynamicTest> =
        cases.map { one ->
            DynamicTest.dynamicTest(one.label) {
                assertThat(Graphemes.clusters(one.text)).isEqualTo(one.clusters)
            }
        }

    private fun suiteVersion(): String? {
        val text =
            requireNotNull(this::class.java.getResourceAsStream("/GraphemeBreakTest.txt")) {
                "the conformance file is not on the classpath"
            }.use { it.readBytes().decodeToString() }
        val first = text.lineSequence().first()
        return Regex("GraphemeBreakTest-([0-9.]+)\\.txt").find(first)?.groupValues?.get(1)
    }

    private fun parse(line: String): ConformanceCase? {
        val body = line.substringBefore('#').trim()
        if (body.isEmpty()) return null

        val clusters = mutableListOf<String>()
        val current = StringBuilder()
        for (token in body.split(' ').filter { it.isNotEmpty() }) {
            when (token) {
                "÷" -> {
                    if (current.isNotEmpty()) {
                        clusters.add(current.toString())
                        current.setLength(0)
                    }
                }
                "×" -> Unit
                else -> current.appendCodePoint(token.toInt(16))
            }
        }
        if (clusters.isEmpty()) return null
        return ConformanceCase(line.substringBefore('#').trim(), clusters.joinToString(""), clusters)
    }

    private data class ConformanceCase(
        val label: String,
        val text: String,
        val clusters: List<String>,
    )
}
