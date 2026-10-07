package com.example.nsselect

import com.example.nsselect.util.CsvFormatException
import com.example.nsselect.util.CsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CsvParserTest {
    @Test
    fun parsesHeaderQuotedCommaAndEscapedQuote() {
        val items = CsvParser.parseCsv("번호,제목,길이(초),분류\n1,\"봄, 여름\",125,가요\n2,\"그가 \"\"온다\"\"\",180,발라드")

        assertEquals(2, items.size)
        assertEquals("봄, 여름", items[0].title)
        assertEquals("그가 \"온다\"", items[1].title)
        assertEquals(125, items[0].durationSeconds)
    }

    @Test
    fun rejectsDuplicateIdsInsteadOfReplacingSongs() {
        val error = assertThrows(CsvFormatException::class.java) {
            CsvParser.parseCsv("장,제목,길이,분류,A\n1,첫 곡,120,가요\n1,둘째 곡,180,가요")
        }
        assertEquals("3행: 번호 1 가 중복됩니다.", error.message)
    }

    @Test
    fun rejectsBadDurationInsteadOfImportingPartialFile() {
        val error = assertThrows(CsvFormatException::class.java) {
            CsvParser.parseCsv("장,제목,길이,분류,A\n1,정상,120,가요\n2,오류,0,가요")
        }
        assertEquals("3행: 길이는 1초 이상의 정수여야 합니다.", error.message)
    }

    @Test
    fun assignsIdsWithoutCollidingWithExplicitIds() {
        val items = CsvParser.parseCsv("장,제목,길이,분류,A\n1,첫 곡,120,가요\n번호 없는 곡,180,가요\n2,마지막 곡,200,가요")
        assertEquals(listOf(1, 3, 2), items.map { it.id })
    }

    @Test
    fun supportsTabSeparatedData() {
        val items = CsvParser.parseCsv("번호\t제목\t길이(초)\t분류\n4\t테스트\t95\t기타")
        assertEquals(4, items.single().id)
        assertEquals("기타", items.single().category)
    }

    @Test
    fun supportsUtf8BomAndNewlineInsideQuotedTitle() {
        val items = CsvParser.parseCsv("\uFEFF번호,제목,길이(초),분류\n1,\"첫 줄\n둘째 줄\",95,가요")
        assertEquals("첫 줄\n둘째 줄", items.single().title)
    }

    @Test
    fun skipsAnyFirstRowAndReadsOptionalWeights() {
        val items = CsvParser.parseCsv("장,제목,길이,분류,A\n1,A,280,가,10\n2,B,150,가,\n3,C,210,나\n4,D,240,나,1")
        assertEquals(listOf(10, 5, 5, 1), items.map { it.weight })
        assertEquals(listOf(280, 150, 210, 240), items.map { it.durationSeconds })
    }

    @Test
    fun rejectsWeightOutsideUiRange() {
        val error = assertThrows(CsvFormatException::class.java) {
            CsvParser.parseCsv("장,제목,길이,분류,A\n1,A,280,가,11")
        }
        assertEquals("2행: 빈도는 1~10 사이의 정수여야 합니다.", error.message)
    }

    @Test
    fun exportedCsvCanBeImportedWithTitlesContainingCommasAndQuotes() {
        val original = CsvParser.parseCsv("장,제목,길이,분류,A\n2,\"봄, \"\"여름\"\"\",180,가,7")
        val restored = CsvParser.parseCsv(CsvParser.toCsv(original))
        assertEquals(original, restored)
    }
}
