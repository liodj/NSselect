package com.example.nsselect

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nsselect.data.*
import com.example.nsselect.generator.PlaylistEditor
import com.example.nsselect.ui.GeneratorScreen
import com.example.nsselect.ui.ReorderablePlaylist
import com.example.nsselect.ui.theme.NSselectTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GeneratorUiTest {
    @get:Rule val compose = createComposeRule()
    private val initial = listOf(ItemEntity(1, "첫 곡", 120, "가요"), ItemEntity(2, "둘째 곡", 180, "가요"), ItemEntity(3, "피아노", 240, "연주곡"))
    @Test fun timeEditorsValidateSaveAndCancelAndReplacementShowsEligibleSongs() {
        var settings by mutableStateOf(GeneratorSettings(targetSeconds = 540, marginSeconds = 10))
        var playlist by mutableStateOf(initial)
        val catalogue = initial + listOf(ItemEntity(4, "교체 가능", 130, "가요"), ItemEntity(5, "전체 오차 초과", 135, "가요"), ItemEntity(6, "사용 해제", 120, "가요", isExcluded = true))
        compose.setContent { NSselectTheme { GeneratorScreen(settings, playlist, catalogue, false, null,
            { settings = it }, {}, { from, to -> playlist = PlaylistEditor.move(playlist, from, to) },
            { from, to -> playlist = playlist.map { if (it.id == from) catalogue.first { candidate -> candidate.id == to } else it } }) } }
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithContentDescription("목표 시간 수정").performClick()
        compose.onNodeWithText("초").performTextReplacement("60")
        compose.onNodeWithText("저장").assertIsNotEnabled()
        compose.onNodeWithText("초").performTextReplacement("0")
        compose.onNodeWithText("분").performTextReplacement("10")
        compose.onNodeWithText("취소").performClick()
        compose.runOnIdle { assertEquals(540, settings.targetSeconds) }
        compose.onNodeWithContentDescription("목표 시간 수정").performClick()
        compose.onNodeWithText("초").performTextReplacement("10")
        compose.onNodeWithText("저장").performClick()
        compose.runOnIdle { assertEquals(550, settings.targetSeconds) }
        compose.onNodeWithContentDescription("전체 오차범위 수정").performClick()
        compose.onNodeWithText("초").performTextReplacement("-1")
        compose.onNodeWithText("저장").assertIsNotEnabled()
        compose.onNodeWithText("초").performTextReplacement("0")
        compose.onNodeWithText("저장").performClick()
        compose.onNodeWithContentDescription("대체 목록 범위 수정").performClick()
        compose.onNodeWithText("초").performTextReplacement("20")
        compose.onNodeWithText("저장").performClick()
        compose.runOnIdle { assertEquals(20, settings.replacementMarginSeconds) }
        capture("generator")
        compose.onNodeWithText("1. 첫 곡").performClick()
        compose.onNodeWithText("길이 차이", substring = true).assertDoesNotExist()
        compose.onNodeWithText("교체 후 총 시간:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("4. 교체 가능").assertIsDisplayed()
        compose.onNodeWithText("5. 전체 오차 초과").assertDoesNotExist()
        compose.onNodeWithText("6. 사용 해제").assertDoesNotExist()
        capture("replacement")
        compose.onNodeWithText("4. 교체 가능").performClick()
        compose.runOnIdle { assertEquals(listOf(4, 2, 3), playlist.map { it.id }); assertEquals(550, playlist.sumOf { it.durationSeconds }) }
    }
    @Test fun draggingHandleMovesSongDownAndUpWithoutChangingTotal() {
        var playlist by mutableStateOf(initial)
        compose.setContent { NSselectTheme { GeneratorScreen(GeneratorSettings(targetSeconds = 540), playlist, initial, false, null,
            {}, {}, { from, to -> playlist = PlaylistEditor.move(playlist, from, to) }, { _, _ -> }) } }
        val first = compose.onNodeWithContentDescription("1. 첫 곡 순서 변경", useUnmergedTree = true)
        val second = compose.onNodeWithContentDescription("2. 둘째 곡 순서 변경", useUnmergedTree = true)
        val distance = second.fetchSemanticsNode().boundsInRoot.center.y - first.fetchSemanticsNode().boundsInRoot.center.y
        first.performTouchInput { swipe(center, center + Offset(0f, distance + 10f), 700) }
        compose.runOnIdle { assertEquals(listOf(2, 1, 3), playlist.map { it.id }) }
        compose.onNodeWithContentDescription("1. 첫 곡 순서 변경", useUnmergedTree = true).performTouchInput { swipe(center, center - Offset(0f, distance + 10f), 700) }
        compose.runOnIdle { assertEquals(initial, playlist); assertEquals(540, playlist.sumOf { it.durationSeconds }) }
    }
    @Test fun holdingDraggedSongAtEdgeScrollsAndMovesBeyondVisibleRows() {
        var playlist by mutableStateOf((1..20).map { ItemEntity(it, "곡 $it", 120, "가요") })
        compose.setContent { NSselectTheme { ReorderablePlaylist(playlist, true,
            { from, to -> playlist = PlaylistEditor.move(playlist, from, to) }, {}, Modifier.height(200.dp).testTag("drag-list")) } }
        val handle = compose.onNodeWithContentDescription("1. 곡 1 순서 변경", useUnmergedTree = true)
        val bottom = compose.onNodeWithTag("drag-list").fetchSemanticsNode().boundsInRoot.bottom
        val centerY = handle.fetchSemanticsNode().boundsInRoot.center.y
        handle.performTouchInput { down(center); moveTo(center + Offset(0f, bottom - centerY - 8f), delayMillis = 500) }
        compose.waitUntil(timeoutMillis = 5000) { playlist.indexOfFirst { it.id == 1 } >= 3 }
        compose.onNodeWithContentDescription("1. 곡 1 순서 변경", useUnmergedTree = true).performTouchInput { up() }
        compose.runOnIdle { assertEquals(20, playlist.size); assertEquals(20, playlist.map { it.id }.distinct().size) }
    }
    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.os.SystemClock.sleep(300)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "ui-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
