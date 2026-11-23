package com.antos.gaming.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.antos.gaming.ui.sidebar.SidebarContent
import org.junit.Rule
import org.junit.Test

class OverlayComposeViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sidebarDisplaysCorrectStatsAndActions() {
        var closeClicked = false

        composeTestRule.setContent {
            SidebarContent(
                fps = 120,
                temp = 42.5f,
                battery = 88,
                ram = 5.2f,
                onClose = { closeClicked = true }
            )
        }

        // Verify stats are rendered
        composeTestRule.onNodeWithText("120").assertIsDisplayed()
        composeTestRule.onNodeWithText("42.5°C").assertIsDisplayed()
        composeTestRule.onNodeWithText("88%").assertIsDisplayed()
        composeTestRule.onNodeWithText("5.2 GB").assertIsDisplayed()

        // Verify actions exist
        composeTestRule.onNodeWithText("🎥 RECORD").assertIsDisplayed()
        composeTestRule.onNodeWithText("📸 SCREENSHOT").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔕 GAMING MODE").assertIsDisplayed()
        
        // Verify Close button
        composeTestRule.onNodeWithText("CLOSE OVERLAY").performClick()
        assert(closeClicked)
    }
}
