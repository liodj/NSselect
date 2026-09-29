package com.example.nsselect

import com.example.nsselect.data.ItemEntity
import com.example.nsselect.generator.PlaylistGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistGeneratorTest {
    @Test
    fun returnsEachSongOnceWithinRequestedRange() {
        val songs = listOf(
            ItemEntity(1, "첫 곡", 60, "가요"),
            ItemEntity(2, "둘째 곡", 60, "가요")
        )
        val result = PlaylistGenerator.generatePlaylist(songs, 120, 0)

        assertEquals(120, result.sumOf { it.durationSeconds })
        assertEquals(2, result.map { it.id }.distinct().size)
    }

    @Test
    fun returnsEmptyWhenNoCombinationCanFit() {
        val songs = listOf(ItemEntity(1, "긴 곡", 200, "가요"))
        assertTrue(PlaylistGenerator.generatePlaylist(songs, 60, 10).isEmpty())
    }
}
