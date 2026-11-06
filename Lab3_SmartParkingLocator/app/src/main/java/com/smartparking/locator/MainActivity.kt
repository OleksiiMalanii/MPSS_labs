package com.smartparking.locator

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

data class Vector3(val x: Float, val y: Float, val z: Float) {
    fun array() = floatArrayOf(x, y, z)
}

data class DashboardState(
    val acceleration: Vector3? = null,
    val magnetic: Vector3? = null,
    val orientation: Orientation? = null,
    val listening: Boolean = false,
    val error: String? = null,
    val accuracy: Int = -1,
)

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var manager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private val ble: BleViewModel by viewModels()
    private val cloud: CloudViewModel by viewModels {
        val app = applicationContext
        viewModelFactory { initializer {
            CloudViewModel(CloudRepository(CloudClient.create(log = CloudViewModel::log), { isOnline(app) }, CloudViewModel::log))
        } }
    }
    internal var dashboard by mutableStateOf(DashboardState())
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        manager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        setContent { MaterialTheme(colorScheme = lightColorScheme(
            primary = Color(0xFF146B62), background = Color(0xFFF2F6F7))) {
            AppRoot(dashboard, ble, cloud)
        } }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cloud.messages.collect { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Нові відліки після повернення.
        dashboard = DashboardState()
        val accelerationReady = accelerometer?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        } ?: false
        val magneticReady = magnetometer?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        } ?: false
        val error = when {
            !accelerationReady -> "Акселерометр недоступний"
            !magneticReady -> "Магнітометр недоступний"
            else -> null
        }
        dashboard = dashboard.copy(listening = accelerationReady || magneticReady, error = error)
        Log.i("ParkingSensors", "onResume: accelerometer=$accelerationReady magnetometer=$magneticReady")
    }

    override fun onPause() {
        manager.unregisterListener(this)
        dashboard = dashboard.copy(listening = false, orientation = null)
        Log.i("ParkingSensors", "onPause: listeners unregistered")
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!dashboard.listening || event.values.size < 3) return
        // Android повторно використовує event.values.
        val value = Vector3(event.values[0], event.values[1], event.values[2])
        val next = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> { cloud.onSample(value); dashboard.copy(acceleration = value) }
            Sensor.TYPE_MAGNETIC_FIELD -> dashboard.copy(magnetic = value)
            else -> return
        }
        val orientation = if (next.acceleration != null && next.magnetic != null)
            OrientationEngine.calculate(next.acceleration.array(), next.magnetic.array(), display?.rotation ?: 0)
        else null
        dashboard = next.copy(orientation = orientation)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) dashboard = dashboard.copy(accuracy = accuracy)
    }
}

/** Є активна мережа з інтернетом, яку система вважає робочою. */
private fun isOnline(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private fun number(value: Float) = if (value.isFinite())
    String.format(Locale.US, "%.1f", if (abs(value) < 0.05f) 0f else value) else "—"

@Composable
internal fun ParkingDashboard(state: DashboardState) {
    val orientation = state.orientation
    val warning = orientation?.let { HeadingMath.excessiveTilt(it.pitch, it.roll) } ?: false
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Smart Parking Locator", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Орієнтація авто", color = Color(0xFF52676B))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("МАГНІТНИЙ КУРС", style = MaterialTheme.typography.labelLarge)
                    Compass(orientation?.azimuth)
                    Text(orientation?.let { "${number(it.azimuth)}° · ${HeadingMath.direction(it.azimuth)}" }
                        ?: "Орієнтація недоступна", fontSize = 27.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("heading"))
                    Text(state.error ?: when {
                        !state.listening -> "Зчитування призупинено"
                        state.acceleration == null || state.magnetic == null -> "Очікую дані сенсорів"
                        orientation == null -> "Непридатні дані або вертикальне положення"
                        warning -> "Нахил понад 15° — вирівняйте пристрій"
                        else -> "Пристрій вирівняний"
                    }, color = if (warning || state.error != null) Color(0xFF9B4A13) else MaterialTheme.colorScheme.primary)
                }
            }
            SensorCard("Акселерометр", "м/с²", state.acceleration)
            SensorCard("Магнітометр", "мкТл", state.magnetic)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Нахил пристрою", fontWeight = FontWeight.Bold)
                    Text(orientation?.let { "Pitch ${number(it.pitch)}°   ·   Roll ${number(it.roll)}°" } ?: "Pitch —   ·   Roll —")
                    LinearProgressIndicator(progress = { orientation?.let {
                        (max(abs(it.pitch), abs(it.roll)) / 90f).coerceIn(0f, 1f)
                    } ?: 0f }, modifier = Modifier.fillMaxWidth(),
                        color = if (warning) Color(0xFFC56C32) else MaterialTheme.colorScheme.primary)
                    Text("Поріг попередження: |pitch| або |roll| > 15°", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
                Text("Низька точність магнітометра. Віддаліть пристрій від металу та відкалібруйте його.",
                    color = Color(0xFF9B4A13), style = MaterialTheme.typography.bodySmall)
            Text("Верхня грань поточного екрана задає напрямок авто. Курс відносно магнітної півночі. GPS не використовується.",
                style = MaterialTheme.typography.bodySmall, color = Color(0xFF52676B))
        }
    }
}

@Composable
private fun SensorCard(title: String, unit: String, value: Vector3?) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(unit, color = Color(0xFF52676B))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("X" to value?.x, "Y" to value?.y, "Z" to value?.z).forEach { (axis, reading) ->
                    Text("$axis  ${reading?.let(::number) ?: "—"}", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun Compass(azimuth: Float?) {
    var previous by remember { mutableFloatStateOf(azimuth ?: 0f) }
    var target by remember { mutableFloatStateOf(azimuth ?: 0f) }
    LaunchedEffect(azimuth) { if (azimuth != null) {
        target += HeadingMath.signedDifference(previous, azimuth)
        previous = azimuth
    } }
    val angle by animateFloatAsState(target, label = "compass")
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(24.dp)) {
            val radius = size.minDimension / 2f
            drawCircle(Color(0xFFE1EDEE), radius, style = Stroke(3.dp.toPx()))
            for (tick in 0 until 24) rotate(tick * 15f) {
                drawLine(Color(0xFF91B2B4), Offset(center.x, center.y - radius),
                    Offset(center.x, center.y - radius + if (tick % 6 == 0) 13.dp.toPx() else 5.dp.toPx()), 2.dp.toPx())
            }
            if (azimuth != null) rotate(angle) {
                val needle = Path().apply {
                    moveTo(center.x, center.y - radius * 0.8f)
                    lineTo(center.x - radius * 0.17f, center.y + radius * 0.35f)
                    lineTo(center.x, center.y + radius * 0.2f)
                    lineTo(center.x + radius * 0.17f, center.y + radius * 0.35f)
                    close()
                }
                drawPath(needle, Color(0xFF146B62))
                drawCircle(Color.White, 4.dp.toPx())
            }
        }
        Text("Пн", Modifier.align(Alignment.TopCenter), fontSize = 13.sp)
        Text("Пд", Modifier.align(Alignment.BottomCenter), fontSize = 13.sp)
        Text("Зх", Modifier.align(Alignment.CenterStart), fontSize = 13.sp)
        Text("Сх", Modifier.align(Alignment.CenterEnd), fontSize = 13.sp)
    }
}
