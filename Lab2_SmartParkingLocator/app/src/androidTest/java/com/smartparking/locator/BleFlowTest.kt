package com.smartparking.locator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BleFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun logContains(text: String) =
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    @Test fun scanConnectSendReadDisconnect() {
        compose.onNodeWithTag("tab-ble").performClick()
        compose.onNodeWithTag("ble-state").assertIsDisplayed()
        compose.onNodeWithTag("ble-send").assertIsNotEnabled()

        compose.onNodeWithTag("ble-scan").performClick()
        compose.onNodeWithTag("ble-progress").assertIsDisplayed()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("ble-device").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("ble-connect").performClick()
        compose.waitUntil(5000) { logContains("GATT connected") }
        compose.onNodeWithTag("ble-send").assertIsEnabled()

        compose.onNodeWithTag("ble-send").performClick()
        compose.waitUntil(5000) { logContains("NOTIFY") }
        compose.waitUntil(5000) { compose.onNodeWithTag("ble-send").runCatching { assertIsEnabled() }.isSuccess }

        compose.onNodeWithTag("ble-read").performClick()
        compose.waitUntil(5000) { logContains("READ value") }
        compose.waitUntil(5000) { compose.onNodeWithTag("ble-disconnect").runCatching { assertIsEnabled() }.isSuccess }

        compose.onNodeWithTag("ble-disconnect").performClick()
        compose.waitUntil(5000) { logContains("GATT disconnected") }
        compose.onNodeWithTag("ble-send").assertIsNotEnabled()
    }
}
