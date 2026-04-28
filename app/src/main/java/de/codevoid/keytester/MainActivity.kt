package de.codevoid.keytester

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ACTION_KEYPRESS = "com.thorkracing.wireddevices.keypress"
private const val MAX_LOG = 500

class MainActivity : ComponentActivity() {
    private val log = mutableStateListOf<KeyLogEntry>()
    private val broadcastDownTimes = mutableMapOf<Pair<String, Int>, Long>()
    private var nextId = 0L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras ?: return
            val deviceName = extras.getString("deviceName") ?: "<unknown>"
            val ts = System.currentTimeMillis()
            when {
                extras.containsKey("key_press") -> {
                    val keyCode = extras.getInt("key_press")
                    broadcastDownTimes[deviceName to keyCode] = ts
                    append(KeyLogEntry(
                        id = nextId++, wallTimeMs = ts, source = "bc",
                        action = "DOWN", keyCode = keyCode,
                        keySymbol = KeyEvent.keyCodeToString(keyCode),
                        deviceName = deviceName,
                        deviceClass = null, inputSource = null,
                        durationMs = null, rawExtras = null
                    ))
                }
                extras.containsKey("key_release") -> {
                    val keyCode = extras.getInt("key_release")
                    val duration = broadcastDownTimes.remove(deviceName to keyCode)?.let { ts - it }
                    append(KeyLogEntry(
                        id = nextId++, wallTimeMs = ts, source = "bc",
                        action = "UP", keyCode = keyCode,
                        keySymbol = KeyEvent.keyCodeToString(keyCode),
                        deviceName = deviceName,
                        deviceClass = null, inputSource = null,
                        durationMs = duration, rawExtras = null
                    ))
                }
                else -> {
                    val raw = extras.keySet().joinToString(" ") { "$it=${extras.get(it)}" }
                    append(KeyLogEntry(
                        id = nextId++, wallTimeMs = ts, source = "bc",
                        action = "?", keyCode = null, keySymbol = null,
                        deviceName = deviceName,
                        deviceClass = null, inputSource = null,
                        durationMs = null, rawExtras = raw
                    ))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                KeyTesterScreen(
                    log = log,
                    onClear = { log.clear(); broadcastDownTimes.clear() },
                    onShare = ::shareLog
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, IntentFilter(ACTION_KEYPRESS), RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, IntentFilter(ACTION_KEYPRESS))
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = when (event.action) {
            KeyEvent.ACTION_DOWN -> "DOWN"
            KeyEvent.ACTION_UP -> "UP"
            KeyEvent.ACTION_MULTIPLE -> "MULTIPLE"
            else -> event.action.toString()
        }
        val duration = if (event.action != KeyEvent.ACTION_DOWN) event.eventTime - event.downTime else null
        val device = event.device
        append(KeyLogEntry(
            id = nextId++,
            wallTimeMs = System.currentTimeMillis(),
            source = "hw",
            action = action,
            keyCode = event.keyCode,
            keySymbol = KeyEvent.keyCodeToString(event.keyCode),
            deviceName = device?.name,
            deviceClass = device?.sources?.let { sourceClassToString(it) },
            inputSource = sourceToString(event.source),
            durationMs = duration,
            rawExtras = null
        ))
        return super.dispatchKeyEvent(event)
    }

    private fun append(entry: KeyLogEntry) {
        log.add(entry)
        if (log.size > MAX_LOG) log.removeAt(0)
    }

    private fun shareLog() {
        val df = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val text = log.asReversed().joinToString("\n") { e ->
            buildString {
                append("${df.format(Date(e.wallTimeMs))} [${e.source}] ${e.action}")
                if (e.keyCode != null) append(" ${e.keyCode}/${e.keySymbol}")
                if (e.deviceName != null) append(" dev=\"${e.deviceName}\"")
                if (e.deviceClass != null) append(" class=${e.deviceClass}")
                if (e.inputSource != null) append(" src=${e.inputSource}")
                if (e.durationMs != null) append(" ${e.durationMs}ms")
                if (e.rawExtras != null) append(" {${e.rawExtras}}")
            }
        }
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, null
        ))
    }
}

private fun sourceClassToString(sources: Int): String = buildList {
    if (sources and InputDevice.SOURCE_CLASS_BUTTON != 0) add("button")
    if (sources and InputDevice.SOURCE_CLASS_POINTER != 0) add("pointer")
    if (sources and InputDevice.SOURCE_CLASS_TRACKBALL != 0) add("trackball")
    if (sources and InputDevice.SOURCE_CLASS_POSITION != 0) add("position")
    if (sources and InputDevice.SOURCE_CLASS_JOYSTICK != 0) add("joystick")
}.joinToString("+").ifEmpty { "0x${sources.toString(16)}" }

private fun sourceToString(source: Int): String = buildList {
    if (source and InputDevice.SOURCE_KEYBOARD != 0) add("keyboard")
    if (source and InputDevice.SOURCE_DPAD != 0) add("dpad")
    if (source and InputDevice.SOURCE_GAMEPAD != 0) add("gamepad")
    if (source and InputDevice.SOURCE_TOUCHSCREEN != 0) add("touchscreen")
    if (source and InputDevice.SOURCE_MOUSE != 0) add("mouse")
    if (source and InputDevice.SOURCE_STYLUS != 0) add("stylus")
    if (source and InputDevice.SOURCE_TRACKBALL != 0) add("trackball")
    if (source and InputDevice.SOURCE_TOUCHPAD != 0) add("touchpad")
    if (source and InputDevice.SOURCE_JOYSTICK != 0) add("joystick")
}.joinToString("+").ifEmpty { "0x${source.toString(16)}" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyTesterScreen(
    log: SnapshotStateList<KeyLogEntry>,
    onClear: () -> Unit,
    onShare: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Key Tester") },
                actions = {
                    TextButton(onClick = onShare) { Text("Share") }
                    TextButton(onClick = onClear) { Text("Clear") }
                }
            )
        }
    ) { padding ->
        if (log.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Waiting for input…", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                reverseLayout = true
            ) {
                items(log, key = { it.id }) { entry ->
                    LogEntryRow(entry)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: KeyLogEntry) {
    val df = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val actionColor = when (entry.action) {
        "DOWN" -> MaterialTheme.colorScheme.primary
        "UP" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    entry.action,
                    color = actionColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(entry.keySymbol ?: "?", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                Text(
                    "(${entry.keyCode ?: "?"})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                df.format(Date(entry.wallTimeMs)),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Row(modifier = Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Tag(entry.source)
            entry.deviceName?.let { Tag(it) }
            entry.deviceClass?.let { Tag(it) }
            entry.inputSource?.let { Tag(it) }
            entry.durationMs?.let { Tag("${it}ms", MaterialTheme.colorScheme.tertiaryContainer) }
            entry.rawExtras?.let { Tag(it, MaterialTheme.colorScheme.errorContainer) }
        }
    }
}

@Composable
private fun Tag(text: String, background: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Box(
        Modifier
            .background(background, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = contentColorFor(background)
        )
    }
}
