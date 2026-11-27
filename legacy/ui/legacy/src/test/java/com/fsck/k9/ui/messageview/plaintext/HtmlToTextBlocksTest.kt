package com.fsck.k9.ui.messageview.plaintext

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test

class HtmlToTextBlocksTest {
    private val testSubject = HtmlToTextBlocks()

    @Test
    fun `headings and paragraphs should become separate blocks without formatting`() {
        val result = testSubject.convert(
            "<h1>Title</h1><p>Hello <b>bold</b>   <i>world</i></p><h3>Sub</h3><div>End</div>",
        )

        assertThat(result).containsExactly(
            TextBlock("Title", headingLevel = 1),
            TextBlock("Hello bold world"),
            TextBlock("Sub", headingLevel = 3),
            TextBlock("End"),
        )
    }

    @Test
    fun `links should keep their text range and address`() {
        val result = testSubject.convert("""<p>Visit <a href="https://example.com/a">our site</a> today</p>""")

        assertThat(result).containsExactly(
            TextBlock("Visit our site today", links = listOf(TextLink(6, 14, "https://example.com/a"))),
        )
    }

    @Test
    fun `unsafe links should be dropped but their text kept`() {
        val result = testSubject.convert(
            """<p><a href="javascript:alert(1)">Click</a> and <a href="file:///etc/passwd">this</a></p>""",
        )

        assertThat(result).containsExactly(TextBlock("Click and this"))
    }

    @Test
    fun `image links should use the alt text, or the address if there is none`() {
        val result = testSubject.convert(
            """<p><a href="https://a.example"><img src="x.png" alt="Logo"></a></p>""" +
                """<p><a href="mailto:me@example.com"><img src="y.png"></a></p>""",
        )

        assertThat(result).containsExactly(
            TextBlock("Logo", links = listOf(TextLink(0, 4, "https://a.example"))),
            TextBlock("me@example.com", links = listOf(TextLink(0, 14, "mailto:me@example.com"))),
        )
    }

    @Test
    fun `scripts, styles, hidden content and images should be dropped`() {
        val result = testSubject.convert(
            """<style>p { color: red }</style><script>evil()</script>""" +
                """<div style="display: none">Preheader</div><p hidden>Hidden</p>""" +
                """<img src="z.png" alt="Pic"><p>Shown</p>""",
        )

        assertThat(result).containsExactly(TextBlock("Shown"))
    }

    @Test
    fun `list items should get bullets`() {
        val result = testSubject.convert("<ul><li>One</li><li><p>Two</p></li></ul>")

        assertThat(result).containsExactly(TextBlock("• One"), TextBlock("• Two"))
    }

    @Test
    fun `line breaks should be kept within a paragraph`() {
        val result = testSubject.convert("<p>Line one<br>Line two</p>")

        assertThat(result).containsExactly(TextBlock("Line one\nLine two"))
    }

    @Test
    fun `preformatted text should keep its whitespace`() {
        val result = testSubject.convert("<pre>a  b\n  c</pre>")

        assertThat(result).containsExactly(TextBlock("a  b\n  c"))
    }

    @Test
    fun `empty message should produce no blocks`() {
        assertThat(testSubject.convert("<html><body> &nbsp; </body></html>")).isEmpty()
    }
}
