package com.fsck.k9.a11yprobe

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fsck.k9.view.MessageWebView
import com.fsck.k9.view.NonLockingScrollView
import java.util.concurrent.TimeoutException
import net.thunderbird.core.android.webkit.WebViewConfig
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Diagnostic, not a regular test: finds out which part of K-9's message view setup stops screen readers from moving
 * through message text by character and word.
 *
 * The same K-9-style HTML message is shown in several setups, from a plain WebView up to K-9's real configuration
 * (MessageWebView settings, inside K-9's NonLockingScrollView at full content height), plus a native TextView as a
 * control. For each, it acts like a screen reader through UiAutomation: finds the paragraph's accessibility node,
 * puts accessibility focus on it and asks for the next character and next word, recording what comes back.
 *
 * Results are logged with the tag "A11yProbe". The test only fails if it can't run at all.
 */
@RunWith(AndroidJUnit4::class)
class WebViewGranularityProbeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var uiAutomation: UiAutomation

    @Before
    fun setUp() {
        uiAutomation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        // Look like a screen reader to the WebView: spoken feedback, touch exploration, all events.
        uiAutomation.serviceInfo = uiAutomation.serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
            flags = flags or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
        }
    }

    @Test
    fun probeAllSetups() {
        val webViewPackage = WebView.getCurrentWebViewPackage()
        log("Android API ${Build.VERSION.SDK_INT}")
        log("WebView ${webViewPackage?.packageName} ${webViewPackage?.versionName}")
        log("Columns: setup | node class | granularities | next-at-granularity action | 3x next char | 3x next word")

        for (setup in ProbeSetup.entries) {
            val result = try {
                probe(setup)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                "ERROR ${e.javaClass.simpleName}: ${e.message}"
            }
            log("${setup.label} | $result")
        }
    }

    private fun probe(setup: ProbeSetup): String {
        ActivityScenario.launch(ProbeActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContentView(setup.createContent(activity)) }

            val node = waitForNode(setup) ?: return "paragraph node NOT FOUND in accessibility tree"
            val granularities = describeGranularities(node.movementGranularities)
            val hasNextAction = node.actionList.any {
                it.id == AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
            }

            val characters = traverse(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER)
            val words = traverse(node, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD)

            return "${node.className} | $granularities | ${if (hasNextAction) "yes" else "NO"} | $characters | $words"
        }
    }

    private fun waitForNode(setup: ProbeSetup): AccessibilityNodeInfo? {
        val deadline = SystemClock.uptimeMillis() + NODE_TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            val root = uiAutomation.rootInActiveWindow
            val node = root?.let { findDeepestNodeWithText(it, NEEDLE) }
            if (node != null && (setup.isNative || node.className != WebView::class.java.name)) return node
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        return null
    }

    private fun findDeepestNodeWithText(node: AccessibilityNodeInfo, needle: String): AccessibilityNodeInfo? {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findDeepestNodeWithText(child, needle)?.let { return it }
        }
        return node.takeIf { it.text?.contains(needle) == true }
    }

    /**
     * Focuses [node] like a screen reader would, then asks three times for the next unit of [granularity].
     */
    private fun traverse(node: AccessibilityNodeInfo, granularity: Int): String {
        node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
        val focused = node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        SystemClock.sleep(FOCUS_SETTLE_MS)

        val steps = (1..TRAVERSAL_STEPS).map {
            var performed = false
            val event = try {
                uiAutomation.executeAndWaitForEvent(
                    {
                        performed = node.performAction(
                            AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                            Bundle().apply {
                                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT, granularity)
                                putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, false)
                            },
                        )
                    },
                    { it.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY },
                    EVENT_TIMEOUT_MS,
                )
            } catch (@Suppress("SwallowedException") e: TimeoutException) {
                null
            }

            when {
                event != null -> "\"${event.traversedText()}\""
                performed -> "accepted, no event"
                else -> "refused"
            }
        }

        return (if (focused) "" else "(focus refused) ") + steps.joinToString(", ")
    }

    private fun AccessibilityEvent.traversedText(): String {
        val fullText = text.joinToString(separator = "")
        val from = fromIndex.coerceIn(0, fullText.length)
        val to = toIndex.coerceIn(from, fullText.length)
        return fullText.substring(from, to)
    }

    private fun describeGranularities(mask: Int): String {
        val names = listOf(
            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER to "char",
            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD to "word",
            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE to "line",
            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH to "paragraph",
            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE to "page",
        ).filter { (flag, _) -> mask and flag != 0 }.map { it.second }

        return names.joinToString(separator = "+").ifEmpty { "NONE" }
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }

    /**
     * The setups compared, from a plain WebView to K-9's real message view configuration.
     */
    private enum class ProbeSetup(
        val label: String,
        val useK9Settings: Boolean = false,
        val inK9ScrollView: Boolean = false,
        val javaScript: Boolean = false,
        val overviewMode: Boolean = true,
        val zoom: Boolean = true,
        val isNative: Boolean = false,
    ) {
        NativeTextView("Native TextView (control)", isNative = true),
        PlainWebView("Plain WebView, full screen"),
        PlainWebViewInScrollView("Plain WebView in K-9 scroll view", inK9ScrollView = true),
        K9Settings("K-9 WebView settings, full screen", useK9Settings = true),
        K9AsShipped("K-9 as shipped (settings + scroll view)", useK9Settings = true, inK9ScrollView = true),
        K9WithJavaScript(
            "K-9 as shipped + JavaScript on",
            useK9Settings = true,
            inK9ScrollView = true,
            javaScript = true,
        ),
        K9WithoutOverview(
            "K-9 as shipped, no wide viewport/overview",
            useK9Settings = true,
            inK9ScrollView = true,
            overviewMode = false,
        ),
        K9WithoutZoom("K-9 as shipped, zoom off", useK9Settings = true, inK9ScrollView = true, zoom = false),
        ;

        fun createContent(activity: ProbeActivity): View {
            val content = if (isNative) {
                TextView(activity).apply { text = PARAGRAPH }
            } else {
                createWebView(activity).apply {
                    loadDataWithBaseURL("about:blank", K9_STYLE_HTML, "text/html", "utf-8", null)
                }
            }

            return if (inK9ScrollView) {
                // Mirrors message.xml: NonLockingScrollView > LinearLayout > (header, WebView at full content height)
                NonLockingScrollView(activity).apply {
                    addView(
                        LinearLayout(activity).apply {
                            orientation = LinearLayout.VERTICAL
                            addView(TextView(activity).apply { text = "Message header" })
                            addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        },
                    )
                }
            } else {
                FrameLayout(activity).apply {
                    addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            }
        }

        private fun createWebView(activity: ProbeActivity): WebView {
            val webView = if (useK9Settings) {
                MessageWebView(activity).apply {
                    configure(WebViewConfig(useDarkMode = false, autoFitWidth = true, textZoom = 100))
                }
            } else {
                WebView(activity)
            }

            webView.settings.apply {
                if (javaScript) javaScriptEnabled = true
                if (!overviewMode) {
                    useWideViewPort = false
                    loadWithOverviewMode = false
                }
                if (!zoom) {
                    setSupportZoom(false)
                    builtInZoomControls = false
                }
            }
            return webView
        }
    }

    private companion object {
        const val TAG = "A11yProbe"
        const val NEEDLE = "quick brown fox"
        const val PARAGRAPH = "The quick brown fox jumps over the lazy dog."
        const val NODE_TIMEOUT_MS = 15_000L
        const val POLL_INTERVAL_MS = 250L
        const val FOCUS_SETTLE_MS = 300L
        const val EVENT_TIMEOUT_MS = 3_000L
        const val TRAVERSAL_STEPS = 3

        // Same structure as DisplayHtml.wrapMessageContent() with K-9's global message CSS.
        val K9_STYLE_HTML = """
            <html dir="auto">
              <head>
                <meta name="viewport" content="width=device-width"/>
                <style>
                  body { font-size: 0.9rem; }
                  .k9mail { display: block; user-select: auto; -webkit-user-select: auto; }
                  .k9mail.k9mail-main {
                    box-sizing: border-box; width: 100%; overflow-wrap: break-word; padding: 0 8px;
                  }
                </style>
              </head>
              <body class="k9mail k9mail-main">
                <h2>Weekly update</h2>
                <p>$PARAGRAPH</p>
                <p>Second paragraph with <a href="https://example.com">a link</a> in it.</p>
              </body>
            </html>
        """.trimIndent()
    }
}
