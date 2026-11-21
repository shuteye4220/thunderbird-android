package com.fsck.k9.ui.settings.messagelistactions

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.fsck.k9.ui.R
import com.fsck.k9.ui.messagelist.MessageListItemAccessibilityAction
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme

/**
 * Lists the message list screen reader actions in their current order.
 *
 * Each action is a single screen reader stop announcing its position, with "Move up", "Move down", "Move to top" and
 * "Move to bottom" as custom actions. The arrow buttons are there for touch users and hidden from screen readers.
 */
@Composable
internal fun MessageListActionOrderScreen(
    actions: ImmutableList<MessageListItemAccessibilityAction>,
    onMove: (action: MessageListItemAccessibilityAction, toIndex: Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = BoltTheme.spacings.default),
        ) {
            item(key = "description") {
                TextBodyMedium(
                    text = stringResource(R.string.message_list_action_order_settings_description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = BoltTheme.spacings.double, vertical = BoltTheme.spacings.default),
                )
            }

            itemsIndexed(
                items = actions,
                key = { _, action -> action.name },
            ) { index, action ->
                ActionOrderRow(
                    action = action,
                    index = index,
                    count = actions.size,
                    onMove = onMove,
                    modifier = Modifier.animateItem(),
                )
            }

            item(key = "reset") {
                ButtonText(
                    text = stringResource(R.string.message_list_action_order_reset),
                    onClick = onReset,
                    modifier = Modifier.padding(horizontal = BoltTheme.spacings.default),
                )
            }
        }
    }
}

@Composable
private fun ActionOrderRow(
    action: MessageListItemAccessibilityAction,
    index: Int,
    count: Int,
    onMove: (action: MessageListItemAccessibilityAction, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canMoveUp = index > 0
    val canMoveDown = index < count - 1
    val position = stringResource(R.string.message_list_action_order_position, index + 1, count)
    val moveUpLabel = stringResource(R.string.message_list_action_order_move_up)
    val moveDownLabel = stringResource(R.string.message_list_action_order_move_down)
    val moveToTopLabel = stringResource(R.string.message_list_action_order_move_to_top)
    val moveToBottomLabel = stringResource(R.string.message_list_action_order_move_to_bottom)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BoltTheme.sizes.iconAvatar)
            .semantics(mergeDescendants = true) {
                stateDescription = position
                fun moveAction(label: String, toIndex: Int) = CustomAccessibilityAction(label) {
                    onMove(action, toIndex)
                    true
                }

                customActions = buildList {
                    if (canMoveUp) add(moveAction(moveUpLabel, index - 1))
                    if (canMoveDown) add(moveAction(moveDownLabel, index + 1))
                    if (canMoveUp) add(moveAction(moveToTopLabel, 0))
                    if (canMoveDown) add(moveAction(moveToBottomLabel, count - 1))
                }
            }
            .padding(start = BoltTheme.spacings.double, end = BoltTheme.spacings.default),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextBodyLarge(
            text = stringResource(action.settingsLabelRes),
            modifier = Modifier.weight(1f),
        )
        Row(
            // Exposed as custom actions on the row instead.
            modifier = Modifier.clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        ) {
            ButtonIcon(
                onClick = { onMove(action, index - 1) },
                imageVector = Icons.Outlined.ExpandLess,
                enabled = canMoveUp,
                contentDescription = moveUpLabel,
            )
            ButtonIcon(
                onClick = { onMove(action, index + 1) },
                imageVector = Icons.Outlined.ExpandMore,
                enabled = canMoveDown,
                contentDescription = moveDownLabel,
            )
        }
    }
}

@get:StringRes
internal val MessageListItemAccessibilityAction.settingsLabelRes: Int
    get() = when (this) {
        MessageListItemAccessibilityAction.Delete -> R.string.delete_action
        MessageListItemAccessibilityAction.Archive -> R.string.archive_action
        MessageListItemAccessibilityAction.ToggleRead -> R.string.message_list_action_toggle_read
        MessageListItemAccessibilityAction.ToggleStar -> R.string.message_list_action_toggle_star
        MessageListItemAccessibilityAction.Reply -> R.string.reply_action
        MessageListItemAccessibilityAction.ReplyAll -> R.string.reply_all_action
        MessageListItemAccessibilityAction.Forward -> R.string.forward_action
        MessageListItemAccessibilityAction.Move -> R.string.move_action
        MessageListItemAccessibilityAction.Copy -> R.string.copy_action
        MessageListItemAccessibilityAction.Spam -> R.string.spam_action
        MessageListItemAccessibilityAction.ToggleSelection -> R.string.message_list_action_toggle_selection
    }
