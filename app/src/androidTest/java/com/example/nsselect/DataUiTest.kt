package com.example.nsselect

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nsselect.data.ItemEntity
import com.example.nsselect.ui.DataTableScreen
import com.example.nsselect.ui.DetailSettingsScreen
import com.example.nsselect.ui.theme.NSselectTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DataUiTest {
    @get:Rule val compose = createComposeRule()
    private val sampleSongs = listOf(
        ItemEntity(1, "첫 곡", 120, "가요"),
        ItemEntity(2, "긴 제목도 표에서 읽을 수 있는 두 번째 노래", 185, "가요", weight = 8),
        ItemEntity(3, "피아노 연주", 240, "연주곡")
    )

    @Test
    fun dataTableExpandsCategoriesAndHidesWeights() {
        var openedDetails = false
        compose.setContent {
            NSselectTheme { DataTableScreen(sampleSongs, onOpenDetails = { openedDetails = true }, onUpdateSong = {}) }
        }
        compose.onNodeWithText("첫 곡").assertDoesNotExist()
        compose.onNodeWithContentDescription("가요 펼치기").performClick()
        compose.onNodeWithText("첫 곡").assertIsDisplayed()
        compose.onNodeWithText("02:00").assertIsDisplayed()
        compose.onAllNodesWithText("빈도", substring = true).assertCountEquals(0)
        saveScreenshot("data-table")
        compose.onNodeWithText("세부 설정").performClick()
        compose.runOnIdle { assertEquals(true, openedDetails) }
        compose.onNodeWithContentDescription("가요 접기").performClick()
        compose.onNodeWithText("첫 곡").assertDoesNotExist()
    }

    @Test
    fun categoryAndSongSlidersSaveOnlyOnConfirmation() {
        var categoryWeight = 5
        var songWeight = 5
        var currentSongs by mutableStateOf(sampleSongs)
        compose.setContent {
            NSselectTheme {
                DetailSettingsScreen(
                    currentSongs, onBack = {}, onCategoryUseChange = { _, _ -> },
                    onCategoryWeightChange = { category, value ->
                        categoryWeight = value
                        currentSongs = currentSongs.map { if (it.category == category) it.copy(categoryWeight = value) else it }
                    },
                    onSongUseChange = { _, _ -> }, onSongWeightChange = { id, value ->
                        songWeight = value
                        currentSongs = currentSongs.map { if (it.id == id) it.copy(weight = value) else it }
                    }
                )
            }
        }
        compose.onNodeWithContentDescription("가요 분류 빈도 조정").performClick()
        compose.onNodeWithContentDescription("가요 분류 빈도 슬라이더")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
        compose.onNodeWithText("저장").performClick()
        compose.runOnIdle { assertEquals(9, categoryWeight) }
        compose.onNodeWithContentDescription("가요 분류 빈도 조정")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "9 / 10"))

        compose.onNodeWithContentDescription("가요 펼치기").performClick()
        saveScreenshot("detail-settings")
        compose.onNodeWithContentDescription("1. 첫 곡 빈도 조정").performClick()
        compose.onNodeWithContentDescription("1. 첫 곡 빈도 슬라이더")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
        compose.onNodeWithText("취소").performClick()
        compose.runOnIdle { assertEquals(5, songWeight) }

        compose.onNodeWithContentDescription("1. 첫 곡 빈도 조정").performClick()
        compose.onNodeWithContentDescription("1. 첫 곡 빈도 슬라이더")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(10f) }
        compose.onNodeWithText("기본값 5").performClick()
        saveScreenshot("weight-slider")
        compose.onNodeWithText("저장").performClick()
        compose.runOnIdle { assertEquals(5, songWeight) }
    }

    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "ui-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
