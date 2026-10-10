package com.ludoproof.game

import com.ludoproof.game.ui.quickchat.QuickChatEmojiCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickChatEmojiCatalogTest {
    @Test
    fun showsTwentyFourUniqueEmojiInFourRowsOfSix() {
        assertEquals(24, QuickChatEmojiCatalog.EMOJIS.size)
        assertEquals(24, QuickChatEmojiCatalog.EMOJIS.distinct().size)
        assertEquals(
            listOf("👍", "😂", "😮", "😭", "😠", "🥳"),
            QuickChatEmojiCatalog.EMOJIS.take(6),
        )
        assertEquals(
            listOf("🤦", "😈", "🔥", "💪", "🤡", "👑"),
            QuickChatEmojiCatalog.EMOJIS.drop(12).take(6),
        )
        assertTrue(QuickChatEmojiCatalog.EMOJIS.contains("😡"))
    }
}
