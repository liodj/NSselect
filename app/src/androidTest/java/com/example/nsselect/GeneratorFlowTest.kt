package com.example.nsselect

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nsselect.data.*
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GeneratorFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun generatorSavesAndRestoresTimePresetThenGeneratesPlaylist() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = AppDatabase.getDatabase(context)
        val songs = runBlocking { database.itemDao().getAllItems() }
        val time = runBlocking { database.generatorSettingsDao().get() ?: GeneratorSettings() }
        var presetId: Long? = null
        try {
            runBlocking {
                database.itemDao().deleteAllItems()
                database.itemDao().insertItems(listOf(ItemEntity(1, "첫 곡", 120, "가요"), ItemEntity(2, "둘째 곡", 180, "가요"), ItemEntity(3, "피아노", 240, "연주곡")))
                database.generatorSettingsDao().save(GeneratorSettings(targetSeconds = 540, marginSeconds = 10))
            }
            compose.waitUntil { compose.onAllNodesWithText("9분 0초").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("대체 목록 범위 수정").performClick()
            compose.onNodeWithText("초").performTextReplacement("20")
            compose.onNodeWithText("저장").performClick()
            compose.onNodeWithText("프리셋").performClick()
            compose.onNodeWithText("새로 저장").performClick()
            compose.onNodeWithText("프리셋 이름").performTextInput("생성기 시간 테스트")
            compose.onNodeWithText("저장").performClick()
            compose.waitUntil { runBlocking { database.presetDao().getPresetByName("생성기 시간 테스트") } != null }
            val preset = runBlocking { database.presetDao().getPresetByName("생성기 시간 테스트")!! }
            presetId = preset.id
            assertEquals(540, preset.targetSeconds); assertEquals(10, preset.marginSeconds); assertEquals(20, preset.replacementMarginSeconds)
            compose.onNodeWithText("닫기").performClick()
            compose.onNodeWithContentDescription("목표 시간 수정").performClick()
            compose.onNodeWithText("분").performTextReplacement("10")
            compose.onNodeWithText("저장").performClick()
            compose.waitUntil { compose.onAllNodesWithText("10분 0초").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("프리셋").performClick()
            compose.onNodeWithText("적용").performClick()
            compose.waitUntil { runBlocking { database.generatorSettingsDao().get()?.targetSeconds } == 540 }
            compose.onNodeWithText("닫기").performClick()
            compose.onNodeWithText("9분 0초").assertIsDisplayed()
            compose.onNodeWithText("±20초").assertIsDisplayed()
            compose.onNodeWithText("랜덤 목록 생성").performClick()
            compose.waitUntil(timeoutMillis = 5000) { compose.onAllNodesWithText("3곡 · 9분 0초").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("오차범위 충족").assertDoesNotExist()
            compose.onNodeWithText("곡을 눌러 교체", substring = true).assertDoesNotExist()
            compose.onNodeWithText("랜덤 목록 생성").assertIsEnabled()
            compose.mainClock.advanceTimeBy(500)
            compose.waitForIdle()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            android.os.SystemClock.sleep(300)
            val directory = File(context.filesDir, "ui-review").apply { mkdirs() }
            File(directory, "generator-app.png").outputStream().use { InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            runBlocking {
                presetId?.let { database.presetDao().deletePreset(it) }
                database.itemDao().deleteAllItems()
                database.itemDao().insertItems(songs)
                database.generatorSettingsDao().save(time)
            }
        }
    }
}
