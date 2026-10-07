package com.example.nsselect

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test

class TabNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigatesBetweenGeneratorDataDetailsAndSettings() {
        compose.onNodeWithText("목표 시간 설정").assertIsDisplayed()
        compose.onNodeWithText("데이터").performClick()
        compose.onNodeWithText("세부 설정").performClick()
        compose.onNodeWithContentDescription("데이터로 돌아가기").performClick()
        compose.onNodeWithText("세부 설정").assertIsDisplayed()
        compose.onNodeWithText("설정").performClick()
        compose.onNodeWithText("CSV 불러오기").assertIsDisplayed()
        compose.onNodeWithText("내보내기").assertIsDisplayed()
        compose.onAllNodesWithText("세부 설정").assertCountEquals(0)
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "ui-review").apply { mkdirs() }
        File(directory, "settings-tab.png").outputStream().use {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("생성기").performClick()
        compose.onNodeWithText("목표 시간 설정").assertIsDisplayed()
    }
}
