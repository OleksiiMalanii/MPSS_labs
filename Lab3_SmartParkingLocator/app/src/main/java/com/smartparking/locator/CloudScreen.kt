package com.smartparking.locator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun statusColor(status: SyncStatus) = when (status) {
    SyncStatus.Ok -> Color(0xFF146B62)
    SyncStatus.Failed -> Color(0xFFB3261E)
    SyncStatus.Pending -> Color(0xFFC56C32)
    SyncStatus.Sending -> Color(0xFF1F5FA8)
    SyncStatus.Idle -> Color(0xFF52676B)
}

@Composable
internal fun CloudScreen(ui: CloudUi, onAuto: (Boolean) -> Unit, onSendNow: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Хмарна синхронізація", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Firebase Realtime Database · /${CloudConfig.PATH.removeSuffix(".json")}", color = Color(0xFF52676B), style = MaterialTheme.typography.bodySmall)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(18.dp).background(statusColor(ui.status), CircleShape).testTag("cloud-dot"))
                    Text("Статус: ${ui.status.label}", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("cloud-status"))
                }
                Text(ui.detail, modifier = Modifier.testTag("cloud-detail"))
                Text("Відправлено: ${ui.sent}   ·   У черзі: ${ui.pending}", modifier = Modifier.testTag("cloud-counters"))
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Автоматична відправка", fontWeight = FontWeight.Bold)
                    Text("раз на ${CloudConfig.INTERVAL_MS / 1000} с або при зміні на ${CloudConfig.DELTA} м/с²",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF52676B))
                }
                Switch(ui.auto, onAuto, Modifier.testTag("cloud-auto"))
            }
        }
        Button(onSendNow, Modifier.fillMaxWidth().testTag("cloud-send")) { Text("Надіслати зараз") }
        Column(Modifier.fillMaxWidth().background(Color(0xFF10243A), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(12.dp)) {
            Text("Останній JSON", color = Color(0xFF91B2B4), fontSize = 12.sp)
            Text(ui.lastPayload, color = Color(0xFFE1EDEE), fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                modifier = Modifier.testTag("cloud-json"))
        }
    }
}
