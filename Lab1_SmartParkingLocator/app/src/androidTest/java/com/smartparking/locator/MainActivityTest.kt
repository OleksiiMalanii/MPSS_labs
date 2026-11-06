package com.smartparking.locator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.smartparking.locator.monitor.MonitorViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val state get() = ViewModelProvider(compose.activity)[MonitorViewModel::class.java].state.value

    @Test fun dashboardShowsRequiredSections() {
        compose.onNodeWithText("Smart Parking Locator").assertIsDisplayed()
        compose.onNodeWithText("Акселерометр").assertIsDisplayed()
        compose.onNodeWithText("Магнітометр").assertIsDisplayed()
    }
    @Test fun listenersStopAndRestartWithLifecycle() {
        compose.waitUntil(10000) {
            state.acceleration != null && state.magnetic != null
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.onActivity {
            assertFalse(state.listening)
            assertNull(state.orientation)
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(10000) {
            state.acceleration != null && state.magnetic != null
        }
        compose.activityRule.scenario.onActivity { assertTrue(state.listening) }
    }
}
