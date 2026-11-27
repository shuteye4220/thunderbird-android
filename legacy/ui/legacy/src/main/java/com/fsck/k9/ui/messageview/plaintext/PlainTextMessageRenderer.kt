package com.fsck.k9.ui.messageview.plaintext

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.provider.Browser
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import com.fsck.k9.ui.R
import com.google.android.material.textview.MaterialTextView

/**
 * Shows [TextBlock]s as native text views: one per paragraph or heading.
 *
 * Unlike the WebView used for HTML, native text can be read by character, word and line by any screen reader.
 * Headings are marked as accessibility headings. Links can be tapped, and each link is also offered as a
 * "Open link: <text>" action in the screen reader's actions menu.
 */
internal class PlainTextMessageRenderer(private val context: Context) {
    private val density = context.resources.displayMetrics.density
    private val accessibilityManager: AccessibilityManager? = context.getSystemService()

    fun render(container: LinearLayout, blocks: List<TextBlock>, textScale: Float) {
        container.removeAllViews()
        for (block in blocks) {
            container.addView(createTextView(block, textScale))
        }
    }

    private fun createTextView(block: TextBlock, textScale: Float): TextView {
        return MaterialTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            val horizontalPadding = dp(HORIZONTAL_PADDING_DP)
            val topPadding = dp(if (block.isHeading) HEADING_TOP_PADDING_DP else PARAGRAPH_PADDING_DP)
            setPadding(horizontalPadding, topPadding, horizontalPadding, dp(PARAGRAPH_PADDING_DP))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeFor(block) * textScale)

            if (block.isHeading) {
                setTypeface(typeface, Typeface.BOLD)
                ViewCompat.setAccessibilityHeading(this, true)
            }

            text = buildText(block)
            if (block.links.isNotEmpty()) {
                setUpLinks(this, block)
            }
        }
    }

    private fun buildText(block: TextBlock): CharSequence {
        if (block.links.isEmpty()) return block.text

        return SpannableString(block.text).apply {
            for (link in block.links) {
                setSpan(MessageLinkSpan(link.url), link.start, link.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpLinks(textView: TextView, block: TextBlock) {
        // Not using LinkMovementMethod: it makes every paragraph clickable, so screen readers would announce each one
        // as a button and a double-tap could open whichever link is in the middle of it.
        textView.setOnTouchListener { view, event -> handleLinkTouch(view as TextView, event) }

        for (link in block.links.take(MAX_LINK_ACTIONS)) {
            val linkText = block.text.substring(link.start, link.end).take(MAX_LINK_LABEL_LENGTH)
            val label = context.getString(R.string.message_view_plain_text_open_link, linkText)
            ViewCompat.addAccessibilityAction(textView, label) { _, _ ->
                openUrl(link.url)
                true
            }
        }
    }

    private fun handleLinkTouch(textView: TextView, event: MotionEvent): Boolean {
        // With explore by touch on, taps are handled by the screen reader (links are reachable via its actions).
        if (accessibilityManager?.isTouchExplorationEnabled == true) return false

        val action = event.actionMasked
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP) return false

        val offset = textView.getOffsetForPosition(event.x, event.y)
        val text = textView.text as? Spanned ?: return false
        val span = text.getSpans(offset, offset, MessageLinkSpan::class.java).firstOrNull() ?: return false

        if (action == MotionEvent.ACTION_UP) {
            span.onClick(textView)
        }
        return true
    }

    private fun openUrl(url: String) {
        // Same as links in the HTML view (see K9WebViewClient).
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
            putExtra(Browser.EXTRA_CREATE_NEW_TAB, true)
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }

        try {
            context.startActivity(intent)
        } catch (@Suppress("SwallowedException") e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_activity_not_found, Toast.LENGTH_LONG).show()
        }
    }

    private inner class MessageLinkSpan(private val url: String) : ClickableSpan() {
        override fun onClick(widget: View) {
            openUrl(url)
        }
    }

    private fun textSizeFor(block: TextBlock): Float = when (block.headingLevel) {
        0 -> BODY_TEXT_SIZE_SP
        1 -> 24f
        2 -> 22f
        3 -> 20f
        else -> 18f
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    private companion object {
        const val BODY_TEXT_SIZE_SP = 16f
        const val HORIZONTAL_PADDING_DP = 16
        const val PARAGRAPH_PADDING_DP = 4
        const val HEADING_TOP_PADDING_DP = 12
        const val MAX_LINK_ACTIONS = 24
        const val MAX_LINK_LABEL_LENGTH = 80
    }
}
