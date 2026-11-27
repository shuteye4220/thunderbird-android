package com.fsck.k9.ui.messageview.plaintext

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeFilter
import org.jsoup.select.NodeFilter.FilterResult
import org.jsoup.select.NodeTraversor

/**
 * A paragraph or heading of a message displayed as plain text.
 *
 * @property headingLevel 1 to 6 for headings, 0 for normal paragraphs.
 */
internal data class TextBlock(
    val text: String,
    val headingLevel: Int = 0,
    val links: List<TextLink> = emptyList(),
) {
    val isHeading: Boolean get() = headingLevel > 0
}

/**
 * A link covering `text.substring(start, end)` of a [TextBlock].
 */
internal data class TextLink(val start: Int, val end: Int, val url: String)

/**
 * Converts a message's HTML into plain text paragraphs, keeping only headings and links.
 *
 * Everything else (images, styling, layout, scripts, hidden content) is dropped. Only `http`, `https`, `mailto` and
 * `tel` links are kept. The result is meant to be shown in native text views, which screen readers can read by
 * character, word and line.
 */
internal class HtmlToTextBlocks {
    fun convert(html: String): List<TextBlock> {
        val body = Jsoup.parse(html).body()
        val collector = BlockCollector()
        NodeTraversor.filter(collector, body)
        collector.flush()
        return collector.blocks
    }

    private class OpenLink(var start: Int, val url: String?)

    @Suppress("TooManyFunctions")
    private class BlockCollector : NodeFilter {
        val blocks = mutableListOf<TextBlock>()

        private val text = StringBuilder()
        private val links = mutableListOf<TextLink>()
        private val openLinks = ArrayDeque<OpenLink>()
        private var headingLevel = 0
        private var preDepth = 0
        private var pendingBullet = false

        override fun head(node: Node, depth: Int): FilterResult {
            when (node) {
                is TextNode -> appendText(node.wholeText)
                is Element -> return enterElement(node)
            }
            return FilterResult.CONTINUE
        }

        override fun tail(node: Node, depth: Int): FilterResult {
            if (node is Element) exitElement(node)
            return FilterResult.CONTINUE
        }

        private fun enterElement(element: Element): FilterResult {
            val tag = element.normalName()
            if (tag in SKIPPED_TAGS || element.isHidden()) return FilterResult.SKIP_ENTIRELY

            when {
                tag in HEADING_TAGS -> {
                    flush()
                    headingLevel = tag[1].digitToInt()
                }
                tag == "br" -> appendLineBreak()
                tag == "li" -> {
                    flush()
                    pendingBullet = true
                }
                tag == "img" -> if (openLinks.isNotEmpty()) appendText(element.attr("alt"))
                tag == "a" -> openLinks.addLast(OpenLink(text.length, element.attr("href").toSafeUrl()))
                tag == "pre" -> {
                    flush()
                    preDepth++
                }
                tag in CELL_TAGS -> appendSeparator()
                tag in BLOCK_TAGS -> flush()
            }
            return FilterResult.CONTINUE
        }

        private fun exitElement(element: Element) {
            val tag = element.normalName()
            when {
                tag in HEADING_TAGS -> {
                    flush()
                    headingLevel = 0
                }
                tag == "a" -> closeLink()
                tag == "pre" -> {
                    flush()
                    preDepth--
                }
                tag == "li" || tag in BLOCK_TAGS -> flush()
            }
        }

        private fun appendText(rawText: String) {
            if (preDepth > 0) {
                appendContent(rawText.replace("\r\n", "\n"))
                return
            }

            var collapsed = rawText.replace(WHITESPACE, " ")
            if (text.isEmpty() || text.last().isWhitespace()) {
                collapsed = collapsed.trimStart()
            }
            if (collapsed.isNotEmpty()) appendContent(collapsed)
        }

        private fun appendContent(content: String) {
            if (pendingBullet && content.isNotBlank()) {
                text.append(BULLET)
                pendingBullet = false
            }
            text.append(content)
        }

        private fun appendLineBreak() {
            if (text.isEmpty()) return
            while (text.isNotEmpty() && text.last() == ' ') text.setLength(text.length - 1)
            if (!text.endsWith("\n\n")) text.append('\n')
        }

        private fun appendSeparator() {
            if (text.isNotEmpty() && !text.last().isWhitespace()) text.append(' ')
        }

        private fun closeLink() {
            val link = openLinks.removeLastOrNull() ?: return
            val url = link.url ?: return

            if (text.substring(link.start).isBlank()) {
                // A link without text (e.g. an image without alt text) would be unreachable, so show its address.
                appendText(url.removePrefix("mailto:").removePrefix("tel:"))
            }
            addLink(link.start, text.length, url)
        }

        private fun addLink(start: Int, end: Int, url: String?) {
            if (url != null && end > start) links.add(TextLink(start, end, url))
        }

        fun flush() {
            // Links spanning more than one block are split at block boundaries.
            for (link in openLinks) {
                addLink(link.start, text.length, link.url)
            }

            val raw = text.toString()
            text.setLength(0)
            val linksInBlock = links.toList()
            links.clear()
            for (link in openLinks) link.start = 0

            val start = raw.indexOfFirst { !it.isWhitespace() }
            if (start == -1) return
            val end = raw.indexOfLast { !it.isWhitespace() } + 1

            val trimmedLinks = linksInBlock.mapNotNull { link ->
                val linkStart = link.start.coerceAtLeast(start) - start
                val linkEnd = link.end.coerceAtMost(end) - start
                if (linkEnd > linkStart) link.copy(start = linkStart, end = linkEnd) else null
            }
            blocks.add(TextBlock(raw.substring(start, end), headingLevel, trimmedLinks))
        }

        private fun Element.isHidden(): Boolean {
            if (hasAttr("hidden")) return true
            val style = attr("style").lowercase().replace(" ", "")
            return "display:none" in style || "visibility:hidden" in style
        }

        private fun String.toSafeUrl(): String? {
            val url = trim()
            val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
            return url.takeIf { scheme in SAFE_SCHEMES }
        }
    }

    private companion object {
        const val BULLET = "• "
        val WHITESPACE = Regex("[\\s\\u00A0]+")
        val SAFE_SCHEMES = setOf("http", "https", "mailto", "tel")
        val SKIPPED_TAGS = setOf("head", "script", "style", "title", "template", "noscript", "svg", "object", "iframe")
        val HEADING_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")
        val CELL_TAGS = setOf("td", "th")
        val BLOCK_TAGS = setOf(
            "p", "div", "section", "article", "header", "footer", "main", "aside", "nav", "blockquote", "ul", "ol",
            "dl", "dt", "dd", "table", "thead", "tbody", "tfoot", "tr", "center", "address", "figure", "figcaption",
            "hr", "form", "fieldset",
        )
    }
}
