package net.thunderbird.feature.navigation.drawer.dropdown.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import net.thunderbird.feature.navigation.drawer.dropdown.R

/**
 * Screen reader description of an unread/starred count badge, e.g. "224 unread, 3 starred".
 *
 * The visual badge shows abbreviated numbers ("99+") and unlabeled icons, so screen readers get the full counts
 * instead. Returns an empty string if there is nothing to announce.
 */
@Composable
internal fun countsContentDescription(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
): String = buildList {
    if (unreadCount > 0) {
        add(pluralStringResource(R.plurals.navigation_drawer_dropdown_unread_count, unreadCount, unreadCount))
    }
    if (showStarredCount && starredCount > 0) {
        add(pluralStringResource(R.plurals.navigation_drawer_dropdown_starred_count, starredCount, starredCount))
    }
}.joinToString(separator = ", ")
