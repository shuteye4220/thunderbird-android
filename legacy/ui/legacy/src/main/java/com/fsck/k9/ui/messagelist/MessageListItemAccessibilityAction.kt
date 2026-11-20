package com.fsck.k9.ui.messagelist

/**
 * Actions exposed to screen readers (TalkBack, Backtalk, etc.) on each message list item.
 *
 * These show up in the screen reader's "Actions" menu / actions rotor, so a user can delete, archive, star, etc.
 * a message without having to open it or perform a swipe gesture, similar to VoiceOver's actions rotor on iOS.
 *
 * The declaration order is the order in which actions are presented to the user, so the most common ones come
 * first.
 */
enum class MessageListItemAccessibilityAction {
    Delete,
    Archive,
    ToggleRead,
    ToggleStar,
    Reply,
    ReplyAll,
    Forward,
    Move,
    Copy,
    Spam,
    ToggleSelection,
}
