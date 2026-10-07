package com.example.nsselect

import com.example.nsselect.data.*
import com.example.nsselect.generator.PlaylistEditor
import org.junit.Assert.*
import org.junit.Test

class PlaylistEditorTest {
    private val song = ItemEntity(1, "원곡", 120, "가요")
    private val other = ItemEntity(2, "다음곡", 180, "가요")
    @Test fun candidatesRespectBothInclusiveMarginsAndUsageAndDuplicates() {
        val catalogue = listOf(song, other,
            ItemEntity(3, "짧은 경계", 105, "가요"), ItemEntity(4, "긴 경계", 135, "가요"),
            ItemEntity(5, "곡 오차 밖", 136, "가요"), ItemEntity(6, "총 오차 밖", 110, "가요"),
            ItemEntity(7, "제외 곡", 120, "가요", isExcluded = true),
            ItemEntity(8, "제외 분류", 120, "가요", isCategoryExcluded = true),
            ItemEntity(9, "같은 길이", 120, "가요"))
        val settings = GeneratorSettings(targetSeconds = 310, marginSeconds = 10)
        assertEquals(listOf(9, 4), PlaylistEditor.candidates(listOf(song, other), 1, catalogue, settings).map { it.id })
        assertEquals(listOf(9, 6, 3, 4), PlaylistEditor.candidates(listOf(song, other), 1, catalogue, settings.copy(targetSeconds = 300, marginSeconds = 15)).map { it.id })
        assertEquals(listOf(9), PlaylistEditor.candidates(listOf(song, other), 1, catalogue, settings.copy(targetSeconds = 300, marginSeconds = 0, replacementMarginSeconds = 0)).map { it.id })
    }
    @Test fun largeDurationsUseLongArithmeticAndReorderingPreservesSongs() {
        val large = song.copy(durationSeconds = Int.MAX_VALUE)
        assertTrue(PlaylistEditor.candidates(listOf(large, other), 1, listOf(large.copy(id = 3)), GeneratorSettings()).isEmpty())
        val third = song.copy(id = 3)
        val list = listOf(song, other, third)
        val reordered = PlaylistEditor.move(list, 1, 3)
        assertEquals(listOf(2, 3, 1), reordered.map { it.id })
        assertEquals(list.toSet(), reordered.toSet())
        assertEquals(list, PlaylistEditor.move(reordered, 1, 2))
        assertEquals(list, PlaylistEditor.move(list, 99, 1))
    }
}
