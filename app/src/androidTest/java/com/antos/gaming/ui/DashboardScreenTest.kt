package com.antos.gaming.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.antos.gaming.ui.dashboard.DashboardScreen
import com.antos.gaming.ui.dashboard.DashboardViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardDisplaysCoreElements() {
        // Since we are mocking ViewModel, we skip injecting actual dependencies
        // A full instrumentation test would use a TestViewModel
        val mockViewModel = mock<DashboardViewModel>()

        composeTestRule.setContent {
            DashboardScreen(viewModel = mockViewModel)
        }

        // Verify Header
        composeTestRule.onNodeWithText("ANTOS").assertIsDisplayed()
        composeTestRule.onNodeWithText("GAMING CORE").assertIsDisplayed()
        
        // Verify Performance Section
        composeTestRule.onNodeWithText("[ PERFORMANCE ]").assertIsDisplayed()
        
        // Verify Boost Button
        composeTestRule.onNodeWithText("ANTOS PERFORMANCE MODE").assertIsDisplayed()
        
        // Verify Library Section
        composeTestRule.onNodeWithText("GAME LIBRARY").assertIsDisplayed()
    }
}
