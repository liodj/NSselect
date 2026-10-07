package com.example.nsselect.util

import com.example.nsselect.data.ItemEntity

class CsvFormatException(message: String) : IllegalArgumentException(message)

object CsvParser {
    private data class Record(val line: Int, val fields: List<String>)

    fun parseCsv(csvContent: String): List<ItemEntity> {
        val content = csvContent.removePrefix("\uFEFF")
        val delimiter = detectDelimiter(content)
        val allRecords = readRecords(content, delimiter)
        if (allRecords.isEmpty()) throw CsvFormatException("파일에 첫 행이 없습니다.")
        val records = allRecords.drop(1).filter { record ->
            record.fields.any { it.isNotBlank() }
        }.toMutableList()

        if (records.isEmpty()) throw CsvFormatException("파일에 노래 데이터가 없습니다.")

        val explicitIds = mutableSetOf<Int>()
        records.forEach { record ->
            if (record.fields.size !in 3..5) {
                throw CsvFormatException("${record.line}행: 열은 3~5개여야 합니다.")
            }
            if (record.fields.size >= 4) {
                val id = record.fields[0].toIntOrNull()
                    ?: throw CsvFormatException("${record.line}행: 번호는 정수여야 합니다.")
                if (id <= 0) throw CsvFormatException("${record.line}행: 번호는 1 이상이어야 합니다.")
                if (!explicitIds.add(id)) throw CsvFormatException("${record.line}행: 번호 $id 가 중복됩니다.")
            }
        }

        var nextId = 1
        return records.map { record ->
            val offset = if (record.fields.size >= 4) 1 else 0
            val id = if (offset == 1) record.fields[0].toInt() else {
                while (nextId in explicitIds) nextId++
                nextId++
                nextId - 1
            }
            val title = record.fields[offset].trim()
            val duration = record.fields[offset + 1].trim().toIntOrNull()
            val category = record.fields[offset + 2].trim()
            val weightText = record.fields.getOrNull(4)?.trim().orEmpty()
            val weight = if (weightText.isEmpty()) 5 else weightText.toIntOrNull()
            if (title.isEmpty()) throw CsvFormatException("${record.line}행: 제목이 비어 있습니다.")
            if (duration == null || duration <= 0) {
                throw CsvFormatException("${record.line}행: 길이는 1초 이상의 정수여야 합니다.")
            }
            if (category.isEmpty()) throw CsvFormatException("${record.line}행: 분류가 비어 있습니다.")
            if (weight == null || weight !in 1..10) {
                throw CsvFormatException("${record.line}행: 빈도는 1~10 사이의 정수여야 합니다.")
            }
            ItemEntity(id = id, title = title, durationSeconds = duration, category = category, weight = weight)
        }
    }

    fun toCsv(items: List<ItemEntity>): String {
        fun escape(value: String): String = "\"${value.replace("\"", "\"\"")}\""
        val rows = items.sortedBy { it.id }.map { item ->
            "${item.id},${escape(item.title)},${item.durationSeconds},${escape(item.category)},${item.weight}"
        }
        return (listOf("번호,제목,길이(초),분류,빈도") + rows).joinToString("\r\n", postfix = "\r\n")
    }

    private fun detectDelimiter(content: String): Char {
        var quoted = false
        var commas = 0
        var tabs = 0
        for (char in content) {
            if (char == '"') quoted = !quoted
            if (!quoted && char == '\n') break
            if (!quoted && char == ',') commas++
            if (!quoted && char == '\t') tabs++
        }
        return if (tabs > commas) '\t' else ','
    }

    private fun readRecords(content: String, delimiter: Char): List<Record> {
        val records = mutableListOf<Record>()
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var line = 1
        var recordLine = 1
        var quoted = false
        var quoteClosed = false
        var i = 0

        fun finishField() {
            fields.add(field.toString().trim())
            field.clear()
            quoteClosed = false
        }

        fun finishRecord() {
            finishField()
            records.add(Record(recordLine, fields.toList()))
            fields.clear()
            recordLine = line + 1
        }

        while (i < content.length) {
            val char = content[i]
            when {
                char == '"' && quoted && i + 1 < content.length && content[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                char == '"' && quoted -> {
                    quoted = false
                    quoteClosed = true
                }
                char == '"' && field.isEmpty() && !quoteClosed -> quoted = true
                char == delimiter && !quoted -> finishField()
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && i + 1 < content.length && content[i + 1] == '\n') i++
                    finishRecord()
                    line++
                }
                else -> {
                    if (char == '"' || (quoteClosed && !char.isWhitespace())) {
                        throw CsvFormatException("${line}행: 따옴표 형식이 올바르지 않습니다.")
                    }
                    if (!quoteClosed) field.append(char)
                    if (char == '\n') line++
                }
            }
            i++
        }
        if (quoted) throw CsvFormatException("${recordLine}행: 닫히지 않은 따옴표가 있습니다.")
        if (field.isNotEmpty() || fields.isNotEmpty()) finishRecord()
        return records
    }
}
