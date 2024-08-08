package com.example.alarmclock

import android.app.TimePickerDialog
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.alarmclock.ui.theme.AlarmClockTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlarmClockTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmClockApp()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmClockApp() {
    var showAlarmRingingDialog by remember { mutableStateOf(false) }
    var ringingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val currentTime = remember { mutableStateOf(LocalTime.now()) }
    val alarms = remember { mutableStateOf(listOf<Alarm>()) }
    val context = LocalContext.current // Get the context using LocalContext.current
    var mediaPlayer: MediaPlayer? = null
    LaunchedEffect(key1 = Unit) {
        while (true) {
            currentTime.value = LocalTime.now()


            alarms.value.forEach { alarm ->
                val currentHour = currentTime.value.hour
                val currentMinute = currentTime.value.minute
                if (alarm.isEnabled && alarm.time.hour == currentHour && alarm.time.minute == currentMinute) {
                    // Trigger alarm
                    mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound)
                    mediaPlayer?.start()

                    ringingAlarm = alarm
                    showAlarmRingingDialog = true

                    Toast.makeText(context, "Alarm is ringing!", Toast.LENGTH_LONG).show()

                    // Wait for 30 seconds, then stop the alarm if not dismissed or snoozed
                    delay(30000)
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    showAlarmRingingDialog = false

                // or when the user interacts with it (e.g., snooze or dismiss)
                }
            }

            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Alarm Clock") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {
        Text(

        text = currentTime.value.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
        style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showDialog = true }) {
            Text("Set Alarm")
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Alarm list
            alarms.value.forEach { alarm ->
                AlarmItem(alarm, onToggle = { updatedAlarm ->
                    // Update the alarm in the alarms list
                    alarms.value = alarms.value.map { if (it == alarm) updatedAlarm else it }
                })
            }
            if (showDialog) {
                AlarmSettingDialog(
                    onDismiss = { showDialog = false },
                    onSave = { newAlarm ->
                        alarms.value = alarms.value + newAlarm
                    }
                )
            }

            if (showAlarmRingingDialog) {
                ringingAlarm?.let { alarm ->
                    AlarmRingingDialog(
                        alarm = alarm,
                        onSnooze = {
                            // Stop the current alarm
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null

                            // Snooze the alarm
                            snoozeAlarm(alarm, alarms)
                            showAlarmRingingDialog = false
                        },
                        onDismiss = {
                            // Stop the current alarm
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null

                            // Dismiss the alarm
                            dismissAlarm(alarm, alarms)
                            showAlarmRingingDialog = false
                        }
                    )
                }
            }

    }
    }
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}

data class Alarm(
    val time: LocalTime,
    val tone: String, // Placeholder for tone selection
    val isEnabled: Boolean = true
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AlarmItem(alarm: Alarm, onToggle: (Alarm) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = alarm.time.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.weight(1f)) // Push toggle to the right
        Switch(
            checked = alarm.isEnabled,
            onCheckedChange = { onToggle(alarm.copy(isEnabled = it)) }
        )
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AlarmSettingDialog(onDismiss: () -> Unit, onSave: (Alarm) -> Unit) {
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Alarm") },
        text = {
            TimePicker(
                selectedTime = selectedTime,
                onTimeChange = { newTime -> selectedTime = newTime }
            )
        },
        confirmButton = {
            Button(onClick = {
                onSave(Alarm(time = selectedTime, tone = "Default")) // Placeholder for tone
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimePicker(selectedTime: LocalTime, onTimeChange: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val hour = selectedTime.hour
    val minute = selectedTime.minute

    val timePickerDialog = TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeChange(LocalTime.of(selectedHour, selectedMinute))
        },
        hour,
        minute,
        true // 24-hour format
    )

    Button(onClick = { timePickerDialog.show() }) {
        Text(selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")))
    }
}
@Composable
fun AlarmRingingDialog(alarm: Alarm, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alarm Ringing") },
        text = { Text("What would you like to do?") },
        confirmButton = {
            Button(onClick = onSnooze) {
                Text("Snooze")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun snoozeAlarm(alarm: Alarm, alarms: MutableState<List<Alarm>>, snoozeMinutes: Long = 5) {
    val snoozedTime = alarm.time.plusMinutes(snoozeMinutes)
    val updatedAlarm = alarm.copy(time = snoozedTime)
    alarms.value = alarms.value.map { if (it == alarm) updatedAlarm else it }
}

fun dismissAlarm(alarm: Alarm, alarms: MutableState<List<Alarm>>) {
    val updatedAlarm = alarm.copy(isEnabled = false)
    alarms.value = alarms.value.map { if (it == alarm) updatedAlarm else it }
}


@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AlarmClockApp()
}