package com.smartparking.locator.cloud

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

private fun statusColor(status: SyncStatus) = when (status) {
    SyncStatus.Ok -> Color(0xFF146B62)
    SyncStatus.Failed -> Color(0xFFB3261E)
    SyncStatus.Pending -> Color(0xFFC56C32)
    SyncStatus.Sending -> Color(0xFF1F5FA8)
    SyncStatus.Idle -> Color(0xFF52676B)
}

@Composable
fun CloudScreen(vm: CloudViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    CloudContent(state, vm::onAction)
}

// Чиста функція: State на вході, Action на виході.
@Composable
internal fun CloudContent(state: CloudState, onAction: (CloudAction) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Хмарна синхронізація", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Firebase Realtime Database · /sensor_reports", color = Color(0xFF52676B), style = MaterialTheme.typography.bodySmall)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(18.dp).background(statusColor(state.status), CircleShape).testTag("cloud-dot"))
                    Text("Статус: ${state.status.label}", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("cloud-status"))
                }
                Text(state.detail, modifier = Modifier.testTag("cloud-detail"))
                Text("Відправлено: ${state.sent}   ·   У черзі: ${state.pending}", modifier = Modifier.testTag("cloud-counters"))
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Автоматична відправка", fontWeight = FontWeight.Bold)
                    Text("раз на ${SendPolicy.INTERVAL_MS / 1000} с або при зміні на ${SendPolicy.DELTA} м/с²",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF52676B))
                }
                Switch(state.auto, { onAction(CloudAction.SetAuto(it)) }, Modifier.testTag("cloud-auto"))
            }
        }
        Button({ onAction(CloudAction.SendNow) }, Modifier.fillMaxWidth().testTag("cloud-send")) { Text("Надіслати зараз") }
        Column(Modifier.fillMaxWidth().background(Color(0xFF10243A), RoundedCornerShape(8.dp))
            .padding(12.dp)) {
            Text("Останній JSON", color = Color(0xFF91B2B4), fontSize = 12.sp)
            Text(state.lastPayload, color = Color(0xFFE1EDEE), fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                modifier = Modifier.testTag("cloud-json"))
        }
    }
}
