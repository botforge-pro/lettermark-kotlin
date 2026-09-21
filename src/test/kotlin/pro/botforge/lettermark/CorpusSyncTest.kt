package pro.botforge.lettermark

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

class CorpusSyncTest {
    private val leading =
        URI("https://raw.githubusercontent.com/botforge-pro/lettermark/main/cases.yaml")

    @Test
    fun theCopyIsWhatTheLeadingRepositoryHolds() {
        val published = leading.toURL().openStream().use { it.readBytes() }
        val carried =
            requireNotNull(this::class.java.getResourceAsStream("/cases.yaml")) {
                "the corpus is the contract and it is not on the classpath"
            }.use { it.readBytes() }

        assertThat(carried)
            .describedAs(
                "the copy of cases.yaml here is not the one at $leading: a case added to the " +
                    "contract and never copied leaves this port on its old behaviour with every " +
                    "test green. Run `make sync-corpus`.",
            ).isEqualTo(published)
    }
}
