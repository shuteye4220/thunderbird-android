package com.fsck.k9.ui.settings.messagelistactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.fsck.k9.ui.messagelist.MessageListAccessibilityActionOrderStore
import com.fsck.k9.ui.messagelist.MessageListItemAccessibilityAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import org.koin.android.ext.android.inject

/**
 * Lets users choose the order of the screen reader actions offered on message list items.
 */
class MessageListActionOrderSettingsFragment : Fragment() {
    private val orderStore: MessageListAccessibilityActionOrderStore by inject()
    private val themeProvider: FeatureThemeProvider by inject()

    private var order: ImmutableList<MessageListItemAccessibilityAction> by mutableStateOf(
        MessageListAccessibilityActionOrderStore.defaultOrder.toImmutableList(),
    )
    private var hidden: ImmutableSet<MessageListItemAccessibilityAction> by mutableStateOf(persistentSetOf())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        order = orderStore.load().toImmutableList()
        hidden = orderStore.loadHidden().toImmutableSet()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                themeProvider.WithTheme {
                    MessageListActionOrderScreen(
                        actions = order,
                        hiddenActions = hidden,
                        onMove = ::onMove,
                        onShownChange = ::onShownChange,
                        onReset = ::onReset,
                    )
                }
            }
        }
    }

    private fun onMove(action: MessageListItemAccessibilityAction, toIndex: Int) {
        val newOrder = MessageListAccessibilityActionOrderStore.move(order, action, toIndex)
        if (newOrder == order) return

        order = newOrder.toImmutableList()
        orderStore.save(newOrder)
    }

    private fun onShownChange(action: MessageListItemAccessibilityAction, isShown: Boolean) {
        val newHidden = if (isShown) hidden - action else hidden + action
        if (newHidden == hidden) return

        hidden = newHidden.toImmutableSet()
        orderStore.saveHidden(newHidden)
    }

    private fun onReset() {
        orderStore.reset()
        order = MessageListAccessibilityActionOrderStore.defaultOrder.toImmutableList()
        hidden = persistentSetOf()
    }
}
