package com.example.nsselect.util

import com.example.nsselect.data.ItemEntity

object CsvParser {
    fun parseCsv(csvContent: String): List<ItemEntity> {
        val items = mutableListOf<ItemEntity>()
        val lines = csvContent.lines()
        
        var fallbackId = 1
        for (line in lines) {
            val cleanLine = line.trim()
            if (cleanLine.isBlank()) continue
            
            // 쉼표(,) 또는 탭(\t)으로 분리 (엑셀 복사/붙여넣기 호환)
            val parts = cleanLine.split(",", "\t").map { it.trim() }
            
            // 최소 3개 이상의 데이터가 있으면 파싱 시도 (번호가 생략되었을 수도 있으므로)
            if (parts.size >= 3) {
                try {
                    val hasId = parts.size >= 4
                    val parsedId = if (hasId) parts[0].toIntOrNull() else null
                    val id = parsedId ?: fallbackId
                    
                    val titleIndex = if (hasId) 1 else 0
                    val durationIndex = if (hasId) 2 else 1
                    val categoryIndex = if (hasId) 3 else 2

                    val title = parts[titleIndex]
                    val duration = parts[durationIndex].toInt()
                    val category = parts[categoryIndex]
                    
                    items.add(
                        ItemEntity(
                            id = id,
                            title = title,
                            durationSeconds = duration,
                            category = category,
                            weight = 5,
                            categoryWeight = 5,
                            isExcluded = false
                        )
                    )
                    
                    // 다음 fallbackId는 현재 할당된 id의 다음 번호로 설정하여 겹침 방지
                    fallbackId = id + 1
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return items
    }
}
