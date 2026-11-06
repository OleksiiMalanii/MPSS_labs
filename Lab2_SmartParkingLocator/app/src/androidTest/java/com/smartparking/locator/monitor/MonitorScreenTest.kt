package com.smartparking.locator.monitor

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartparking.locator.helpers.Orientation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MonitorScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun missingSensorShowsErrorAndNoHeading() {
        compose.setContent { MaterialTheme {
            MonitorContent(MonitorState(error = "Магнітометр недоступний", listening = true))
        } }
        compose.onNodeWithText("Магнітометр недоступний").assertIsDisplayed()
        compose.onNodeWithText("Орієнтація недоступна").assertIsDisplayed()
        compose.onNodeWithTag("heading").assertTextEquals("Орієнтація недоступна")
    }
    @Test fun excessiveTiltShowsWarning() {
        compose.setContent { MaterialTheme {
            MonitorContent(MonitorState(acceleration = Vector3(0f, 4.9f, 8.5f),
                magnetic = Vector3(0f, 1.6f, -47.1f), orientation = Orientation(0f, -30f, 0f), tiltWarning = true, listening = true))
        } }
        compose.onNodeWithText("Нахил понад 15° — вирівняйте пристрій").assertIsDisplayed()
    }
}
