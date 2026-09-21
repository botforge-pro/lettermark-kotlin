package pro.botforge.lettermark

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

@Serializable
data class Corpus(
    val version: Int,
    val initials: List<InitialsCase>,
    val slot: List<SlotCase>,
)

@Serializable
data class InitialsCase(
    @SerialName("case") val label: String,
    val name: String,
    val expect: String,
    @SerialName("expect_codepoints") val expectCodepoints: String? = null,
)

@Serializable
data class SlotCase(
    @SerialName("case") val label: String,
    val id: Long,
    val slots: Int,
    val expect: Int,
)

class CorpusTest {
    private val corpus: Corpus by lazy {
        val stream =
            requireNotNull(this::class.java.getResourceAsStream("/cases.yaml")) {
                "the corpus is the contract and it is not on the classpath"
            }
        val text = stream.use { it.readBytes().decodeToString() }
        Yaml.default.decodeFromString(Corpus.serializer(), text)
    }

    @TestFactory
    fun lettersTheCorpusNames(): Collection<DynamicTest> =
        corpus.initials.map { one ->
            DynamicTest.dynamicTest(one.label) {
                val got = initials(one.name)
                assertThat(got).describedAs(one.name).isEqualTo(one.expect)
                if (one.expectCodepoints != null) {
                    assertThat(codepointsOf(got))
                        .describedAs(
                            "the letters read the same but are written differently, which a port " +
                                "that skips normalising would also pass",
                        ).isEqualTo(one.expectCodepoints)
                }
            }
        }

    @TestFactory
    fun slotsTheCorpusNames(): Collection<DynamicTest> =
        corpus.slot.map { one ->
            DynamicTest.dynamicTest(one.label) {
                val palette = Palette(one.slots)
                assertThat(palette.slots).isEqualTo(one.slots)
                assertThat(palette.slot(one.id)).describedAs(one.label).isEqualTo(one.expect)
            }
        }

    @Test
    fun theCorpusArrivedWhole() {
        assertThat(corpus.version).isEqualTo(2)
        assertThat(corpus.initials).isNotEmpty()
        assertThat(corpus.slot).isNotEmpty()
    }

    @Test
    fun aPaletteThatPaintsNothingIsRefused() {
        for (slots in listOf(0, -1)) {
            assertThrows<IllegalArgumentException>(
                "a palette of $slots colours means the code and the theme disagree, and a slot " +
                    "handed out now would be a colour nobody painted",
            ) { Palette(slots) }
        }
    }

    private fun codepointsOf(text: String): String =
        text
            .codePoints()
            .toArray()
            .joinToString(" ") { "%04X".format(it) }
}
