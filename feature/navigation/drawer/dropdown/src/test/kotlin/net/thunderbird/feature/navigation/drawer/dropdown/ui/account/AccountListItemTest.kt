package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithText
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import kotlin.test.Test
import net.thunderbird.feature.navigation.drawer.dropdown.R
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData

internal class AccountListItemTest : ComposeTest() {

    @Test
    fun `account should be read with full unread and starred counts`() {
        val account = FakeData.MAIL_DISPLAY_ACCOUNT.copy(unreadMessageCount = 224, starredMessageCount = 1)
        setAccountListItem(account, showStarredCount = true)

        onNodeWithText(FakeData.DISPLAY_NAME).assert(hasContentDescription("224 unread, 1 starred"))
    }

    @Test
    fun `starred count should not be read when hidden`() {
        val account = FakeData.MAIL_DISPLAY_ACCOUNT.copy(unreadMessageCount = 5, starredMessageCount = 3)
        setAccountListItem(account, showStarredCount = false)

        onNodeWithText(FakeData.DISPLAY_NAME).assert(hasContentDescription("5 unread"))
    }

    @Test
    fun `account error should be read`() {
        val account = FakeData.MAIL_DISPLAY_ACCOUNT.copy(hasError = true)
        setAccountListItem(account, showStarredCount = false)

        onNodeWithText(FakeData.DISPLAY_NAME)
            .assert(hasContentDescription(getString(R.string.navigation_drawer_dropdown_account_error)))
    }

    @Test
    fun `avatar monogram should not be exposed to screen readers`() {
        setAccountListItem(FakeData.MAIL_DISPLAY_ACCOUNT, showStarredCount = false)

        composeTestRule.onAllNodesWithText("AC", useUnmergedTree = true).assertCountEquals(0)
    }

    private fun setAccountListItem(account: MailDisplayAccount, showStarredCount: Boolean) {
        setContentWithTheme {
            AccountListItem(
                account = account,
                onClick = {},
                selected = false,
                showStarredCount = showStarredCount,
            )
        }
    }
}
