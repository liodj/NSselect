package com.example.nsselect.generator

import com.example.nsselect.data.GeneratorSettings
import com.example.nsselect.data.ItemEntity
import kotlin.math.abs

object PlaylistEditor {
    fun candidates(playlist: List<ItemEntity>, songId: Int, catalogue: List<ItemEntity>, settings: GeneratorSettings): List<ItemEntity> {
        val song = playlist.firstOrNull { it.id == songId } ?: return emptyList()
        val ids = playlist.map { it.id }.toSet()
        val remaining = playlist.sumOf { it.durationSeconds.toLong() } - song.durationSeconds
        val lower = settings.targetSeconds.toLong() - settings.marginSeconds
        val upper = settings.targetSeconds.toLong() + settings.marginSeconds
        return catalogue.filter {
            !it.isExcluded && !it.isCategoryExcluded && it.id !in ids &&
                abs(it.durationSeconds.toLong() - song.durationSeconds) <= settings.replacementMarginSeconds &&
                remaining + it.durationSeconds in lower..upper
        }.sortedWith(compareBy<ItemEntity> { abs(it.durationSeconds.toLong() - song.durationSeconds) }.thenBy { it.id })
    }

    fun move(playlist: List<ItemEntity>, songId: Int, destinationId: Int): List<ItemEntity> {
        val from = playlist.indexOfFirst { it.id == songId }
        val to = playlist.indexOfFirst { it.id == destinationId }
        if (from < 0 || to < 0 || from == to) return playlist
        return playlist.toMutableList().apply { add(to, removeAt(from)) }
    }
}
