package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.organism.drawer.NavigationDrawerItemBadge
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.navigation.drawer.dropdown.R
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.labelForCount

@Composable
internal fun AccountListItemBadge(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    AccountCountAndStarredBadge(
        unreadCount = unreadCount,
        starredCount = starredCount,
        showStarredCount = showStarredCount,
        modifier = modifier,
    )
}

@Composable
private fun AccountCountAndStarredBadge(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    // The badge shows abbreviated numbers and unlabeled icons, so give screen readers the full counts instead.
    val countsDescription = buildList {
        if (unreadCount > 0) {
            add(pluralStringResource(R.plurals.navigation_drawer_dropdown_unread_count, unreadCount, unreadCount))
        }
        if (showStarredCount && starredCount > 0) {
            add(pluralStringResource(R.plurals.navigation_drawer_dropdown_starred_count, starredCount, starredCount))
        }
    }.joinToString(separator = ", ")

    Column(
        modifier = modifier.clearAndSetSemantics {
            if (countsDescription.isNotEmpty()) {
                contentDescription = countsDescription
            }
        },
        horizontalAlignment = Alignment.End,
    ) {
        val resources = LocalResources.current

        if (unreadCount > 0) {
            NavigationDrawerItemBadge(
                label = labelForCount(
                    count = unreadCount,
                    resources = resources,
                ),
                imageVector = if (showStarredCount) Icons.Filled.Dot else null,
            )
        }

        if (showStarredCount && starredCount > 0) {
            Spacer(modifier = Modifier.Companion.width(BoltTheme.spacings.half))
            NavigationDrawerItemBadge(
                label = labelForCount(
                    count = starredCount,
                    resources = resources,
                ),
                imageVector = Icons.Filled.Star,
            )
        }
    }
}
