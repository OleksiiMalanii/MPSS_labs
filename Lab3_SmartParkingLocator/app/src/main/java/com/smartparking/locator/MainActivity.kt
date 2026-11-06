package com.smartparking.locator

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.smartparking.locator.ble.BleScreen
import com.smartparking.locator.ble.BleViewModel
import com.smartparking.locator.ble.StatusPacket
import com.smartparking.locator.cloud.CloudAction
import com.smartparking.locator.cloud.CloudEvent
import com.smartparking.locator.cloud.CloudScreen
import com.smartparking.locator.cloud.CloudViewModel
import com.smartparking.locator.monitor.MonitorAction
import com.smartparking.locator.monitor.MonitorContent
import com.smartparking.locator.monitor.MonitorViewModel

class MainActivity : ComponentActivity() {
    // Ті самі ViewModel переживають поворот екрана.
    private val monitor: MonitorViewModel by viewModels()
    private val ble: BleViewModel by viewModels()
    private val cloud: CloudViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent { MaterialTheme(colorScheme = lightColorScheme(
            primary = Color(0xFF146B62), background = Color(0xFFF2F6F7))) {
            AppRoot(monitor, ble, cloud)
        } }
    }

    // Екран видно — слухаємо сенсори, екран сховано — відписуємось.
    override fun onResume() {
        super.onResume()
        monitor.onAction(MonitorAction.Start)
        cloud.onAction(CloudAction.Start)
    }

    override fun onPause() {
        monitor.onAction(MonitorAction.Stop)
        cloud.onAction(CloudAction.Stop)
        super.onPause()
    }
}

@Composable
private fun AppRoot(monitor: MonitorViewModel, ble: BleViewModel, cloud: CloudViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val sensors by monitor.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Повідомлення хмари показую на будь-якій вкладці, поки застосунок на екрані.
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            cloud.events.collect { event ->
                when (event) {
                    is CloudEvent.ShowMessage -> Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, Modifier.testTag("tab-sensors"), text = { Text("Сенсори") })
                Tab(tab == 1, { tab = 1 }, Modifier.testTag("tab-ble"), text = { Text("BLE") })
                Tab(tab == 2, { tab = 2 }, Modifier.testTag("tab-cloud"), text = { Text("Хмара") })
            }
            when (tab) {
                0 -> MonitorContent(sensors)
                1 -> BleScreen(ble, StatusPacket.from(sensors.orientation))
                else -> CloudScreen(cloud)
            }
        }
    }
}
