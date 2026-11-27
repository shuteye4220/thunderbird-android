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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.fsck.k9.ui.R
import com.fsck.k9.ui.messagelist.MessageListItemAccessibilityAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.Switch
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.atom.button.ButtonText
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme

/**
 * Lists the message list screen reader actions in their current order, each with a switch to turn it off.
 *
 * Like the messages themselves, each action is a single screen reader stop announced as
 * "Name, Shown/Hidden, Position x of y", with everything else in the screen reader's actions menu: "Turn off/on",
 * "Move above <previous>", "Move below <next>", "Move to top" and "Move to bottom". The switch and arrow buttons are
 * there for touch users and hidden from screen readers.
 */
@Composable
internal fun MessageListActionOrderScreen(
    actions: ImmutableList<MessageListItemAccessibilityAction>,
    hiddenActions: ImmutableSet<MessageListItemAccessibilityAction>,
    onMove: (action: MessageListItemAccessibilityAction, toIndex: Int) -> Unit,
    onShownChange: (action: MessageListItemAccessibilityAction, isShown: Boolean) -> Unit,
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
                    previousActionLabel = actions.getOrNull(index - 1)?.let { stringResource(it.settingsLabelRes) },
                    nextActionLabel = actions.getOrNull(index + 1)?.let { stringResource(it.settingsLabelRes) },
                    index = index,
                    count = actions.size,
                    isShown = action !in hiddenActions,
                    onMove = onMove,
                    onShownChange = onShownChange,
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
    previousActionLabel: String?,
    nextActionLabel: String?,
    index: Int,
    count: Int,
    isShown: Boolean,
    onMove: (action: MessageListItemAccessibilityAction, toIndex: Int) -> Unit,
    onShownChange: (action: MessageListItemAccessibilityAction, isShown: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canMoveUp = previousActionLabel != null
    val canMoveDown = nextActionLabel != null
    val position = stringResource(R.string.message_list_action_order_position, index + 1, count)
    val visibility = stringResource(
        if (isShown) R.string.message_list_action_order_shown else R.string.message_list_action_order_hidden,
    )
    val toggleLabel = stringResource(
        if (isShown) R.string.message_list_action_order_turn_off else R.string.message_list_action_order_turn_on,
    )
    val moveUpLabel = previousActionLabel?.let { stringResource(R.string.message_list_action_order_move_above, it) }
    val moveDownLabel = nextActionLabel?.let { stringResource(R.string.message_list_action_order_move_below, it) }
    val moveToTopLabel = stringResource(R.string.message_list_action_order_move_to_top)
    val moveToBottomLabel = stringResource(R.string.message_list_action_order_move_to_bottom)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BoltTheme.sizes.iconAvatar)
            .semantics(mergeDescendants = true) {
                stateDescription = "$visibility, $position"
                fun moveAction(label: String, toIndex: Int) = CustomAccessibilityAction(label) {
                    onMove(action, toIndex)
                    true
                }

                customActions = buildList {
                    add(
                        CustomAccessibilityAction(toggleLabel) {
                            onShownChange(action, !isShown)
                            true
                        },
                    )
                    if (moveUpLabel != null) add(moveAction(moveUpLabel, index - 1))
                    if (moveDownLabel != null) add(moveAction(moveDownLabel, index + 1))
                    if (canMoveUp) add(moveAction(moveToTopLabel, 0))
                    if (canMoveDown) add(moveAction(moveToBottomLabel, count - 1))
                }
            }
            .padding(start = BoltTheme.spacings.double, end = BoltTheme.spacings.default),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextBodyLarge(
            text = stringResource(action.settingsLabelRes),
            modifier = Modifier
                .weight(1f)
                .alpha(if (isShown) 1f else HIDDEN_ACTION_ALPHA),
        )
        ActionRowControls(
            isShown = isShown,
            onShownChange = { onShownChange(action, it) },
            onMoveUp = if (canMoveUp) ({ onMove(action, index - 1) }) else null,
            onMoveDown = if (canMoveDown) ({ onMove(action, index + 1) }) else null,
        )
    }
}

/**
 * The switch and arrow buttons for touch users. Hidden from screen readers, which get the same functions as the row's
 * state and custom actions instead.
 */
@Composable
private fun ActionRowControls(
    isShown: Boolean,
    onShownChange: (Boolean) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.default),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = isShown,
            onCheckedChange = onShownChange,
        )
        ButtonIcon(
            onClick = { onMoveUp?.invoke() },
            imageVector = Icons.Outlined.ExpandLess,
            enabled = onMoveUp != null,
        )
        ButtonIcon(
            onClick = { onMoveDown?.invoke() },
            imageVector = Icons.Outlined.ExpandMore,
            enabled = onMoveDown != null,
        )
    }
}

private const val HIDDEN_ACTION_ALPHA = 0.6f

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
