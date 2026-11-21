package com.fsck.k9.ui.messagelist

import android.content.Context
import androidx.core.content.edit

/**
 * Stores the user's preferred order of the screen reader actions offered on message list items, and which of them
 * are hidden.
 *
 * These are kept in their own preferences file and are not part of the settings export.
 */
class MessageListAccessibilityActionOrderStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<MessageListItemAccessibilityAction> {
        return normalizeOrder(preferences.getString(KEY_ORDER, null))
    }

    fun save(order: List<MessageListItemAccessibilityAction>) {
        preferences.edit {
            putString(KEY_ORDER, normalize(order).joinToString(separator = SEPARATOR) { it.name })
        }
    }

    fun loadHidden(): Set<MessageListItemAccessibilityAction> {
        return parse(preferences.getString(KEY_HIDDEN, null)).toSet()
    }

    fun saveHidden(hidden: Set<MessageListItemAccessibilityAction>) {
        preferences.edit {
            putString(KEY_HIDDEN, hidden.sortedBy { it.ordinal }.joinToString(separator = SEPARATOR) { it.name })
        }
    }

    /**
     * Restores the default order and shows all actions.
     */
    fun reset() {
        preferences.edit {
            remove(KEY_ORDER)
            remove(KEY_HIDDEN)
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "message_list_accessibility_actions"
        private const val KEY_ORDER = "order"
        private const val KEY_HIDDEN = "hidden"
        private const val SEPARATOR = ","

        val defaultOrder: List<MessageListItemAccessibilityAction> = MessageListItemAccessibilityAction.entries

        /**
         * Parses a persisted order. Unknown or duplicate entries are dropped, and actions missing from the persisted
         * value (e.g. ones added in a later version) are appended in their default order.
         */
        fun normalizeOrder(persisted: String?): List<MessageListItemAccessibilityAction> {
            return normalize(parse(persisted))
        }

        /**
         * Parses a comma-separated list of action names, ignoring unknown entries.
         */
        internal fun parse(persisted: String?): List<MessageListItemAccessibilityAction> {
            return persisted
                ?.split(SEPARATOR)
                ?.mapNotNull { name -> MessageListItemAccessibilityAction.entries.firstOrNull { it.name == name } }
                .orEmpty()
        }

        private fun normalize(
            order: List<MessageListItemAccessibilityAction>,
        ): List<MessageListItemAccessibilityAction> {
            val distinct = order.distinct()
            return distinct + defaultOrder.filterNot { it in distinct }
        }

        /**
         * Returns a copy of [order] with [action] moved to [toIndex] (clamped to the list bounds).
         */
        fun move(
            order: List<MessageListItemAccessibilityAction>,
            action: MessageListItemAccessibilityAction,
            toIndex: Int,
        ): List<MessageListItemAccessibilityAction> {
            if (action !in order) return order

            val result = order.toMutableList()
            result.remove(action)
            result.add(toIndex.coerceIn(0, result.size), action)
            return result
        }
    }
}
