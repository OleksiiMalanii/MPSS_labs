package com.smartparking.locator.ble

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartparking.locator.helpers.ConnectionState

private fun stateColor(state: ConnectionState) = when (state) {
    ConnectionState.Connected -> Color(0xFF146B62)
    ConnectionState.DataTransfer -> Color(0xFF1F5FA8)
    ConnectionState.Scanning, ConnectionState.Connecting -> Color(0xFFC56C32)
    ConnectionState.Disconnected -> Color(0xFF52676B)
}

@Composable
fun BleScreen(vm: BleViewModel, packet: String) {
    val state by vm.state.collectAsStateWithLifecycle()
    BleContent(state, packet, vm::onAction)
}

// Чиста функція: State на вході, Action на виході.
@Composable
internal fun BleContent(state: BleState, packet: String, onAction: (BleAction) -> Unit) {
    val connected = state.connection == ConnectionState.Connected
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("BLE-зв'язок", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Віртуальне реле паркінгу (Mock)", color = Color(0xFF52676B))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(14.dp).background(stateColor(state.connection), CircleShape))
                Text("Статус: ${state.connection.label}", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("ble-state"))
            }
        }
        Button({ onAction(BleAction.Scan) }, Modifier.fillMaxWidth().testTag("ble-scan"),
            enabled = state.connection == ConnectionState.Disconnected) { Text("Пошук пристроїв") }
        if (state.connection == ConnectionState.Scanning) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("ble-progress"))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Знайдені пристрої", fontWeight = FontWeight.Bold)
                if (state.devices.isEmpty()) Text("Список порожній. Натисніть «Пошук пристроїв».", color = Color(0xFF52676B))
                state.devices.forEach { device ->
                    Row(Modifier.fillMaxWidth().testTag("ble-device"), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(device.name, fontWeight = FontWeight.Bold)
                            Text("${device.address} · ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall)
                        }
                        if (connected) OutlinedButton({ onAction(BleAction.Disconnect) }, Modifier.testTag("ble-disconnect")) { Text("Відключити") }
                        else Button({ onAction(BleAction.Connect) }, Modifier.testTag("ble-connect"),
                            enabled = state.connection == ConnectionState.Disconnected) { Text("Підключити") }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ onAction(BleAction.Send(packet)) }, Modifier.weight(1f).testTag("ble-send"), enabled = connected) { Text("Надіслати курс") }
            OutlinedButton({ onAction(BleAction.Read) }, Modifier.weight(1f).testTag("ble-read"), enabled = connected) { Text("Читати статус") }
        }
        Text("Пакет: $packet", style = MaterialTheme.typography.bodySmall, color = Color(0xFF52676B))
        Column(Modifier.fillMaxWidth().background(Color(0xFF10243A), RoundedCornerShape(8.dp)).padding(12.dp)
            .testTag("ble-log")) {
            Text("Термінал", color = Color(0xFF91B2B4), fontSize = 12.sp)
            if (state.log.isEmpty()) Text("—", color = Color(0xFFE1EDEE), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            state.log.takeLast(12).forEach {
                Text(it, color = Color(0xFFE1EDEE), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
