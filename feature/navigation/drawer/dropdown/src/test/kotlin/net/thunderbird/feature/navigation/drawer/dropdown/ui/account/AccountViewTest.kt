package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onAllNodesWithText
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import net.thunderbird.feature.navigation.drawer.dropdown.R
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData

internal class AccountViewTest : ComposeTest() {

    @Test
    fun `collapsed header should describe its state, click and switch account action`() {
        var avatarClicks = 0
        setAccountView(
            showAccountSelection = false,
            nextAccountName = NEXT_ACCOUNT_NAME,
            onAvatarClick = { avatarClicks++ },
        )

        val testSubject = header(R.string.navigation_drawer_dropdown_account_view_collapsed)
        val config = testSubject.fetchSemanticsNode().config

        assertThat(config.getOrNull(SemanticsActions.OnClick)?.label)
            .isEqualTo(getString(R.string.navigation_drawer_dropdown_account_view_show_account_list))
        val customActions = config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertThat(customActions.map { it.label }).isEqualTo(listOf("Switch to $NEXT_ACCOUNT_NAME"))

        customActions.single().action()

        assertThat(avatarClicks).isEqualTo(1)
    }

    @Test
    fun `expanded header should describe its state and offer no switch account action`() {
        setAccountView(showAccountSelection = true, nextAccountName = NEXT_ACCOUNT_NAME)

        val testSubject = header(R.string.navigation_drawer_dropdown_account_view_expanded)
        val config = testSubject.fetchSemanticsNode().config

        assertThat(config.getOrNull(SemanticsActions.OnClick)?.label)
            .isEqualTo(getString(R.string.navigation_drawer_dropdown_account_view_hide_account_list))
        assertThat(config.getOrNull(SemanticsActions.CustomActions)).isNull()
    }

    @Test
    fun `header should offer no switch account action when there is only one account`() {
        setAccountView(showAccountSelection = false, nextAccountName = null)

        val config = header(R.string.navigation_drawer_dropdown_account_view_collapsed).fetchSemanticsNode().config

        assertThat(config.getOrNull(SemanticsActions.CustomActions)).isNull()
    }

    @Test
    fun `avatar monogram should not be exposed to screen readers`() {
        setAccountView(showAccountSelection = false, nextAccountName = NEXT_ACCOUNT_NAME)

        composeTestRule.onAllNodesWithText(MONOGRAM).assertCountEquals(0)
    }

    private fun header(stateResId: Int): SemanticsNodeInteraction =
        composeTestRule.onNode(hasClickAction() and hasStateDescription(getString(stateResId)))

    private fun setAccountView(
        showAccountSelection: Boolean,
        nextAccountName: String?,
        onAvatarClick: () -> Unit = {},
    ) {
        setContentWithTheme {
            AccountView(
                account = FakeData.MAIL_DISPLAY_ACCOUNT,
                onClick = {},
                onAvatarClick = onAvatarClick,
                showAccountSelection = showAccountSelection,
                isShowAnimations = false,
                nextAccountName = nextAccountName,
            )
        }
    }

    private companion object {
        const val NEXT_ACCOUNT_NAME = "Work"
        const val MONOGRAM = "AC"
    }
}
