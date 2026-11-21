package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLargeAutoResize
import net.thunderbird.components.ui.bolt.atom.text.TextBodyMedium
import net.thunderbird.components.ui.bolt.atom.text.TextDisplayMediumAutoResize
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.feature.navigation.drawer.dropdown.R
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.AnimatedExpandIcon
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.getDisplayAccountName

@Composable
internal fun AccountView(
    account: DisplayAccount,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit,
    showAccountSelection: Boolean,
    isShowAnimations: Boolean,
    modifier: Modifier = Modifier,
    nextAccountName: String? = null,
) {
    AccountLayout(
        onClick = onClick,
        isExpanded = showAccountSelection,
        // Tapping the avatar switches to the next account. Screen readers get that as a named action on the whole
        // row instead of an unlabeled button.
        switchAccountActionLabel = if (!showAccountSelection && nextAccountName != null) {
            stringResource(R.string.navigation_drawer_dropdown_account_view_switch_to_account, nextAccountName)
        } else {
            null
        },
        onSwitchAccount = onAvatarClick,
        modifier = modifier,
    ) {
        if (showAccountSelection) {
            AccountSelectionView()
        } else {
            AccountSelectedView(
                account = account,
                onAvatarClick = onAvatarClick,
                isShowAnimations = isShowAnimations,
            )
        }

        AnimatedExpandIcon(
            isExpanded = showAccountSelection,
            isShowAnimations = isShowAnimations,
            modifier = Modifier.padding(end = BoltTheme.spacings.double),
            tint = BoltTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun RowScope.AccountSelectedView(
    account: DisplayAccount,
    onAvatarClick: () -> Unit,
    isShowAnimations: Boolean,
) {
    AnimatedContent(
        targetState = account,
        transitionSpec = {
            if (isShowAnimations) {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally(animationSpec = snap()) { 0 } + fadeIn(animationSpec = snap())) togetherWith
                    (slideOutHorizontally(animationSpec = snap()) { 0 } + fadeOut(animationSpec = snap()))
            }
        },
        label = "AccountSelectedContent",
        contentKey = { it.id },
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) { targetAccount ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double),
            // While animating to another account, hide the outgoing one so screen readers only announce the new one.
            modifier = if (targetAccount.id != account.id) Modifier.clearAndSetSemantics {} else Modifier,
        ) {
            AccountAvatar(
                account = targetAccount,
                onClick = { onAvatarClick() },
                selected = false,
                // Exposed as a custom action on the row, see AccountLayout.
                modifier = Modifier.clearAndSetSemantics {},
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.half),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val name = getDisplayAccountName(targetAccount)
                TextBodyLargeAutoResize(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(name)
                        }
                    },
                    softWrap = true,
                    maxLines = 2,
                )
                if (targetAccount is MailDisplayAccount && targetAccount.name != targetAccount.email) {
                    TextBodyMedium(
                        text = targetAccount.email,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.AccountSelectionView() {
    TextBodyLarge(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(stringResource(R.string.navigation_drawer_dropdown_avount_view_selection_title))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    )
}

@Composable
private fun AccountLayout(
    onClick: () -> Unit,
    isExpanded: Boolean,
    switchAccountActionLabel: String?,
    onSwitchAccount: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val clickLabel = stringResource(
        if (isExpanded) {
            R.string.navigation_drawer_dropdown_account_view_hide_account_list
        } else {
            R.string.navigation_drawer_dropdown_account_view_show_account_list
        },
    )
    val expandedState = stringResource(
        if (isExpanded) {
            R.string.navigation_drawer_dropdown_account_view_expanded
        } else {
            R.string.navigation_drawer_dropdown_account_view_collapsed
        },
    )

    Box(
        modifier = modifier
            .clickable(
                onClickLabel = clickLabel,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                stateDescription = expandedState
                // Announce the new account name when switching accounts from the row's actions.
                liveRegion = LiveRegionMode.Polite
                if (switchAccountActionLabel != null) {
                    customActions = listOf(
                        CustomAccessibilityAction(switchAccountActionLabel) {
                            onSwitchAccount()
                            true
                        },
                    )
                }
            }
            .padding(
                top = BoltTheme.spacings.default,
                start = BoltTheme.spacings.triple,
                end = BoltTheme.spacings.double,
                bottom = BoltTheme.spacings.default,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BoltTheme.sizes.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double),
        ) {
            content()
        }
    }
}

@Composable
fun getDisplayCutOutHorizontalInsetPadding(): WindowInsets {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return WindowInsets.displayCutout.only(if (isRtl) WindowInsetsSides.Right else WindowInsetsSides.Left)
}
