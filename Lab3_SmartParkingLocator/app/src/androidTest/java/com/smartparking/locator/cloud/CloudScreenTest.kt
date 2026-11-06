package com.smartparking.locator.cloud

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsEachStatusAndForwardsActions() {
        val actions = mutableListOf<CloudAction>()
        val state = CloudState(status = SyncStatus.Pending, pending = 3, detail = "Немає інтернету, у черзі: 3")
        compose.setContent {
            MaterialTheme { CloudContent(state) { actions += it } }
        }
        compose.onNodeWithTag("cloud-status").assertTextEquals("Статус: Pending")
        compose.onNodeWithTag("cloud-counters").assertTextEquals("Відправлено: 0   ·   У черзі: 3")
        compose.onNodeWithTag("cloud-detail").assertIsDisplayed()
        compose.onNodeWithTag("cloud-send").performClick()
        compose.onNodeWithTag("cloud-auto").performClick()
        compose.waitForIdle()
        assertEquals(listOf(CloudAction.SendNow, CloudAction.SetAuto(false)), actions)
    }
}
