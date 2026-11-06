package com.smartparking.locator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AppRoot(sensors: DashboardState, ble: BleViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val ui by ble.ui.collectAsState()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, Modifier.testTag("tab-sensors"), text = { Text("Сенсори") })
                Tab(tab == 1, { tab = 1 }, Modifier.testTag("tab-ble"), text = { Text("BLE") })
            }
            if (tab == 0) ParkingDashboard(sensors)
            else BleScreen(ui, StatusPacket.from(sensors.orientation), ble::scan, ble::connect, ble::disconnect, ble::send, ble::read)
        }
    }
}

private fun stateColor(state: BleState) = when (state) {
    BleState.Connected -> Color(0xFF146B62)
    BleState.DataTransfer -> Color(0xFF1F5FA8)
    BleState.Scanning, BleState.Connecting -> Color(0xFFC56C32)
    BleState.Disconnected -> Color(0xFF52676B)
}

@Composable
internal fun BleScreen(
    ui: BleUi,
    packet: String,
    onScan: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSend: (String) -> Unit,
    onRead: () -> Unit,
) {
    val connected = ui.state == BleState.Connected
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("BLE-зв'язок", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Віртуальне реле паркінгу (Mock)", color = Color(0xFF52676B))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(14.dp).background(stateColor(ui.state), CircleShape))
                Text("Статус: ${ui.state.label}", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("ble-state"))
            }
        }
        Button(onScan, Modifier.fillMaxWidth().testTag("ble-scan"),
            enabled = ui.state == BleState.Disconnected) { Text("Пошук пристроїв") }
        if (ui.state == BleState.Scanning) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("ble-progress"))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Знайдені пристрої", fontWeight = FontWeight.Bold)
                if (ui.devices.isEmpty()) Text("Список порожній. Натисніть «Пошук пристроїв».", color = Color(0xFF52676B))
                ui.devices.forEach { device ->
                    Row(Modifier.fillMaxWidth().testTag("ble-device"), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(device.name, fontWeight = FontWeight.Bold)
                            Text("${device.address} · ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall)
                        }
                        if (connected) OutlinedButton(onDisconnect, Modifier.testTag("ble-disconnect")) { Text("Відключити") }
                        else Button(onConnect, Modifier.testTag("ble-connect"),
                            enabled = ui.state == BleState.Disconnected) { Text("Підключити") }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ onSend(packet) }, Modifier.weight(1f).testTag("ble-send"), enabled = connected) { Text("Надіслати курс") }
            OutlinedButton(onRead, Modifier.weight(1f).testTag("ble-read"), enabled = connected) { Text("Читати статус") }
        }
        Text("Пакет: $packet", style = MaterialTheme.typography.bodySmall, color = Color(0xFF52676B))
        Column(Modifier.fillMaxWidth().background(Color(0xFF10243A), RoundedCornerShape(8.dp)).padding(12.dp)
            .testTag("ble-log")) {
            Text("Термінал", color = Color(0xFF91B2B4), fontSize = 12.sp)
            if (ui.log.isEmpty()) Text("—", color = Color(0xFFE1EDEE), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            ui.log.takeLast(12).forEach {
                Text(it, color = Color(0xFFE1EDEE), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
