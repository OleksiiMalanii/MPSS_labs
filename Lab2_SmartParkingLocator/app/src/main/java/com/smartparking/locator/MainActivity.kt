package com.smartparking.locator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartparking.locator.ble.BleScreen
import com.smartparking.locator.ble.BleViewModel
import com.smartparking.locator.ble.StatusPacket
import com.smartparking.locator.monitor.MonitorAction
import com.smartparking.locator.monitor.MonitorContent
import com.smartparking.locator.monitor.MonitorViewModel

class MainActivity : ComponentActivity() {
    // Ті самі ViewModel переживають поворот екрана.
    private val monitor: MonitorViewModel by viewModels()
    private val ble: BleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent { MaterialTheme(colorScheme = lightColorScheme(
            primary = Color(0xFF146B62), background = Color(0xFFF2F6F7))) {
            AppRoot(monitor, ble)
        } }
    }

    // Екран видно — слухаємо сенсори, екран сховано — відписуємось.
    override fun onResume() {
        super.onResume()
        monitor.onAction(MonitorAction.Start)
    }

    override fun onPause() {
        monitor.onAction(MonitorAction.Stop)
        super.onPause()
    }
}

@Composable
private fun AppRoot(monitor: MonitorViewModel, ble: BleViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val sensors by monitor.state.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, Modifier.testTag("tab-sensors"), text = { Text("Сенсори") })
                Tab(tab == 1, { tab = 1 }, Modifier.testTag("tab-ble"), text = { Text("BLE") })
            }
            if (tab == 0) MonitorContent(sensors)
            else BleScreen(ble, StatusPacket.from(sensors.orientation))
        }
    }
}
