package com.smartparking.locator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.smartparking.locator.monitor.MonitorAction
import com.smartparking.locator.monitor.MonitorScreen
import com.smartparking.locator.monitor.MonitorViewModel

class MainActivity : ComponentActivity() {
    private val vm: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent { MaterialTheme(colorScheme = lightColorScheme(
            primary = Color(0xFF146B62), background = Color(0xFFF2F6F7))) {
            MonitorScreen(vm)
        } }
    }

    // Екран видно — слухаємо сенсори, екран сховано — відписуємось.
    override fun onResume() {
        super.onResume()
        vm.onAction(MonitorAction.Start)
    }

    override fun onPause() {
        vm.onAction(MonitorAction.Stop)
        super.onPause()
    }
}
