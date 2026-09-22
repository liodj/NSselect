package com.example.nsselect.generator

import com.example.nsselect.data.ItemEntity
import kotlin.math.pow
import kotlin.random.Random

object PlaylistGenerator {

    fun generatePlaylist(
        validItems: List<ItemEntity>,
        targetSeconds: Int,
        marginSeconds: Int
    ): List<ItemEntity> {
        var attempts = 0
        
        while(attempts < 1000) {
            val shuffled = validItems.map { item ->
                val randomVal = Random.nextDouble()
                // A-Res algorithm core formula using 2-level weight
                val totalWeight = (item.weight * item.categoryWeight).coerceAtLeast(1)
                val score = randomVal.pow(1.0 / totalWeight)
                Pair(item, score)
            }.sortedByDescending { it.second }.map { it.first }
            
            val currentList = mutableListOf<ItemEntity>()
            var currentDuration = 0
            
            for(item in shuffled) {
                currentList.add(item)
                currentDuration += item.durationSeconds
                
                if (currentDuration in (targetSeconds - marginSeconds)..(targetSeconds + marginSeconds)) {
                    return currentList
                }
                
                if (currentDuration > targetSeconds + marginSeconds) {
                    break
                }
            }
            attempts++
        }
        return emptyList()
    }
}
