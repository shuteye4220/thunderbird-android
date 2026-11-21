package com.fsck.k9.ui.messagelist

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.fsck.k9.ui.messagelist.MessageListAccessibilityActionOrderStore.Companion.defaultOrder
import com.fsck.k9.ui.messagelist.MessageListAccessibilityActionOrderStore.Companion.move
import com.fsck.k9.ui.messagelist.MessageListAccessibilityActionOrderStore.Companion.normalizeOrder
import com.fsck.k9.ui.messagelist.MessageListItemAccessibilityAction.Archive
import com.fsck.k9.ui.messagelist.MessageListItemAccessibilityAction.Delete
import com.fsck.k9.ui.messagelist.MessageListItemAccessibilityAction.Reply
import com.fsck.k9.ui.messagelist.MessageListItemAccessibilityAction.ToggleRead
import kotlin.test.Test

class MessageListAccessibilityActionOrderStoreTest {

    @Test
    fun `missing value should use the default order`() {
        val testSubject = normalizeOrder(persisted = null)

        assertThat(testSubject).isEqualTo(defaultOrder)
    }

    @Test
    fun `persisted order should come first, followed by missing actions in default order`() {
        val testSubject = normalizeOrder(persisted = "Reply,Delete")

        assertThat(testSubject.take(3)).containsExactly(Reply, Delete, Archive)
        assertThat(testSubject.toSet()).isEqualTo(defaultOrder.toSet())
        assertThat(testSubject.size).isEqualTo(defaultOrder.size)
    }

    @Test
    fun `unknown and duplicate entries should be ignored`() {
        val testSubject = normalizeOrder(persisted = "Reply,NoSuchAction,Reply,,Delete")

        assertThat(testSubject.take(2)).containsExactly(Reply, Delete)
        assertThat(testSubject.size).isEqualTo(defaultOrder.size)
    }

    @Test
    fun `move should place the action at the target index`() {
        val testSubject = move(listOf(Delete, Archive, ToggleRead, Reply), Reply, toIndex = 0)

        assertThat(testSubject).containsExactly(Reply, Delete, Archive, ToggleRead)
    }

    @Test
    fun `move should clamp the target index`() {
        val testSubject = move(listOf(Delete, Archive, ToggleRead), Delete, toIndex = 10)

        assertThat(testSubject).containsExactly(Archive, ToggleRead, Delete)
    }
}
