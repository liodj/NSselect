package com.example.nsselect

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nsselect.data.ItemEntity
import com.example.nsselect.data.SettingsPreset
import com.example.nsselect.ui.DetailSettingsScreen
import com.example.nsselect.ui.PresetPanel
import com.example.nsselect.ui.theme.NSselectTheme
import com.example.nsselect.viewmodel.PresetUiState
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PresetUiTest {
    @get:Rule val compose = createComposeRule()
    private val initial = SettingsPreset(1, "평소", 0, 2, 1)

    @Test
    fun savesNamesAndRequiresConfirmationForOverwriteAndDelete() {
        var state by mutableStateOf(PresetUiState(presets = listOf(initial)))
        var savedName = ""
        var appliedId = 0L
        var overwrittenId = 0L
        var deletedId = 0L
        compose.setContent {
            NSselectTheme {
                DetailSettingsScreen(
                    listOf(ItemEntity(1, "첫 곡", 120, "가요"), ItemEntity(2, "둘째 곡", 180, "가요")),
                    onBack = {}, onCategoryUseChange = { _, _ -> }, onCategoryWeightChange = { _, _ -> },
                    onSongUseChange = { _, _ -> }, onSongWeightChange = { _, _ -> },
                    presetControls = {
                        PresetPanel(state, true,
                            onSelect = { state = state.copy(selectedId = it) },
                            onSave = { name ->
                                savedName = name
                                state = state.copy(presets = state.presets + initial.copy(id = 2, name = name), selectedId = 2)
                            },
                            onApply = { appliedId = it }, onOverwrite = { overwrittenId = it },
                            onDelete = { id -> deletedId = id; state = state.copy(presets = state.presets.filter { it.id != id }, selectedId = null) }
                        )
                    }
                )
            }
        }
        compose.onNodeWithText("새로 저장").performClick()
        compose.onNodeWithText("프리셋 이름").performTextInput("평소")
        compose.onNodeWithText("저장").assertIsNotEnabled()
        compose.onNodeWithText("프리셋 이름").performTextReplacement("  운동  ")
        compose.onNodeWithText("저장").performClick()
        compose.runOnIdle { assertEquals("운동", savedName) }
        compose.onNodeWithText("적용").performClick()
        compose.runOnIdle { assertEquals(2L, appliedId) }
        saveScreenshot()
        compose.onNodeWithText("덮어쓰기").performClick()
        compose.onNodeWithText("취소").performClick()
        compose.runOnIdle { assertEquals(0L, overwrittenId) }
        compose.onNodeWithText("덮어쓰기").performClick()
        compose.onNode(hasText("덮어쓰기") and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertEquals(2L, overwrittenId) }
        compose.onNodeWithText("삭제").performClick()
        compose.onNodeWithText("취소").performClick()
        compose.runOnIdle { assertEquals(0L, deletedId) }
        compose.onNodeWithText("삭제").performClick()
        compose.onNode(hasText("삭제") and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertEquals(2L, deletedId); assertEquals(1, state.presets.size) }
        compose.onNodeWithContentDescription("프리셋 목록").performClick()
        compose.onNodeWithText("평소").performClick()
        compose.runOnIdle { assertEquals(1L, state.selectedId) }
    }

    @Test
    fun emptySongListStillAllowsDeletingSavedPresets() {
        compose.setContent {
            NSselectTheme {
                PresetPanel(PresetUiState(listOf(initial), selectedId = 1), false,
                    onSelect = {}, onSave = {}, onApply = {}, onOverwrite = {}, onDelete = {})
            }
        }
        compose.onNodeWithText("새로 저장").assertIsNotEnabled()
        compose.onNodeWithText("적용").assertIsNotEnabled()
        compose.onNodeWithText("덮어쓰기").assertIsNotEnabled()
        compose.onNodeWithText("삭제").assertIsEnabled()
    }

    private fun saveScreenshot() {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "ui-review").apply { mkdirs() }
        File(directory, "presets.png").outputStream().use {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
