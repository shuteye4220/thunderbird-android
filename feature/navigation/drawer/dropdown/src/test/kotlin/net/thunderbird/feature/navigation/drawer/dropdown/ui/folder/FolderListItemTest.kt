package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.onNodeWithText
import app.k9mail.core.ui.compose.testing.setContentWithTheme
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.feature.mail.folder.FolderType
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.navigation.drawer.dropdown.R
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.DisplayTreeFolder
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.MailDisplayFolder

internal class FolderListItemTest : ComposeTest() {

    @Test
    fun `clicking a folder should open it`() {
        val folder = createFolder(isPlaceholder = false)
        val clickedFolders = mutableListOf<DisplayFolder>()
        setFolderListItem(folder, onClick = { clickedFolders += it })

        onNodeWithText(FOLDER_NAME).performClick()

        assertThat(clickedFolders).containsExactly(folder)
    }

    @Test
    fun `clicking a placeholder folder should not open it`() {
        val clickedFolders = mutableListOf<DisplayFolder>()
        setFolderListItem(createFolder(isPlaceholder = true), onClick = { clickedFolders += it })

        onNodeWithText(FOLDER_NAME).performClick()

        assertThat(clickedFolders).isEmpty()
    }

    @Test
    fun `folder with subfolders should expose collapsed state, total counts and a show subfolders action`() {
        setFolderListItemWithSubfolder()

        val testSubject = onNodeWithText(FOLDER_NAME)
        testSubject.assert(hasStateDescription(getString(R.string.navigation_drawer_dropdown_account_view_collapsed)))
        testSubject.assert(hasContentDescription("9 unread"))
        val customActions = testSubject.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertThat(customActions.map { it.label })
            .containsExactly(getString(R.string.navigation_drawer_dropdown_folder_expand_subfolders))
        composeTestRule.onAllNodesWithText(CHILD_FOLDER_NAME).assertCountEquals(0)

        composeTestRule.runOnIdle { customActions.single().action() }

        onNodeWithText(FOLDER_NAME)
            .assert(hasStateDescription(getString(R.string.navigation_drawer_dropdown_account_view_expanded)))
        composeTestRule.onAllNodesWithText(CHILD_FOLDER_NAME).assertCountEquals(1)
    }

    @Test
    fun `folder without subfolders should not expose an expanded state or actions`() {
        setFolderListItem(createFolder(isPlaceholder = false), onClick = {})

        val config = onNodeWithText(FOLDER_NAME).fetchSemanticsNode().config
        assertThat(config.getOrNull(SemanticsProperties.StateDescription)).isNull()
        assertThat(config.getOrNull(SemanticsActions.CustomActions)).isNull()
    }

    private fun setFolderListItemWithSubfolder() {
        val parent = createFolder(isPlaceholder = false).copy(unreadMessageCount = 4)
        val child = createFolder(isPlaceholder = false).copy(
            folder = Folder(
                id = 2,
                name = "$FOLDER_NAME/$CHILD_FOLDER_NAME",
                type = FolderType.REGULAR,
                isLocalOnly = false,
            ),
            unreadMessageCount = 5,
        )
        val treeFolder = DisplayTreeFolder(
            displayFolder = parent,
            displayName = FOLDER_NAME,
            totalUnreadCount = 9,
            totalStarredCount = 0,
            children = persistentListOf(
                DisplayTreeFolder(
                    displayFolder = child,
                    displayName = CHILD_FOLDER_NAME,
                    totalUnreadCount = 5,
                    totalStarredCount = 0,
                    children = persistentListOf(),
                ),
            ),
        )
        setContentWithTheme {
            FolderListItem(
                displayFolder = parent,
                onClick = {},
                showStarredCount = false,
                folderNameFormatter = FolderNameFormatter(LocalResources.current),
                selectedFolderId = null,
                treeFolder = treeFolder,
            )
        }
    }

    private fun setFolderListItem(folder: MailDisplayFolder, onClick: (DisplayFolder) -> Unit) {
        setContentWithTheme {
            FolderListItem(
                displayFolder = folder,
                onClick = onClick,
                showStarredCount = false,
                folderNameFormatter = FolderNameFormatter(LocalResources.current),
                selectedFolderId = null,
            )
        }
    }

    private fun createFolder(isPlaceholder: Boolean) = MailDisplayFolder(
        accountId = ACCOUNT_ID,
        folder = Folder(id = 1, name = FOLDER_NAME, type = FolderType.REGULAR, isLocalOnly = false),
        isInTopGroup = false,
        unreadMessageCount = 0,
        starredMessageCount = 0,
        pathDelimiter = "/",
        isPlaceholder = isPlaceholder,
    )

    private companion object {
        const val FOLDER_NAME = "Turing"
        const val CHILD_FOLDER_NAME = "Lovelace"
    }
}
