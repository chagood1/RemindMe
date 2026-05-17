package com.example.remindme

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.remindme.data.ReminderDatabase
import com.example.remindme.data.ReminderEntity
import com.example.remindme.notifications.ReminderScheduler
import com.example.remindme.ui.theme.RemindMeTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            RemindMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReminderScreen()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

class ReminderViewModel(
    private val database: ReminderDatabase,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {
    val reminders: StateFlow<List<ReminderEntity>> =
        database.reminderDao().getAllReminders().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addReminder(title: String, dueDateTimeMillis: Long) {
        viewModelScope.launch {
            val reminderId = database.reminderDao().insertReminder(
                ReminderEntity(
                    title = title,
                    dueDateTimeMillis = dueDateTimeMillis,
                    isCompleted = false
                )
            ).toInt()

            reminderScheduler.schedule(
                ReminderEntity(
                    id = reminderId,
                    title = title,
                    dueDateTimeMillis = dueDateTimeMillis,
                    isCompleted = false
                )
            )
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderScheduler.cancel(reminder)
            database.reminderDao().deleteReminder(reminder)
        }
    }
}

class ReminderViewModelFactory(
    private val database: ReminderDatabase,
    private val reminderScheduler: ReminderScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(database, reminderScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen() {
    val context = LocalContext.current
    val database = remember { ReminderDatabase.getDatabase(context) }
    val reminderScheduler = remember { ReminderScheduler(context) }
    val viewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(database, reminderScheduler)
    )

    val reminderText = remember { mutableStateOf("") }
    val selectedDateTimeText = remember { mutableStateOf("") }
    val selectedDateTimeMillis = remember { mutableLongStateOf(0L) }
    val reminders by viewModel.reminders.collectAsState()

    val showDatePicker = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            utcCalendar.timeInMillis = millis

                            val calendar = Calendar.getInstance()
                            calendar.clear()
                            calendar.set(
                                utcCalendar.get(Calendar.YEAR),
                                utcCalendar.get(Calendar.MONTH),
                                utcCalendar.get(Calendar.DAY_OF_MONTH)
                            )

                            val currentTime = Calendar.getInstance()

                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    calendar.set(Calendar.SECOND, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)

                                    selectedDateTimeMillis.longValue = calendar.timeInMillis
                                    selectedDateTimeText.value = formatDateTime(calendar.timeInMillis)
                                },
                                currentTime.get(Calendar.HOUR_OF_DAY),
                                currentTime.get(Calendar.MINUTE),
                                false
                            ).show()
                        }
                        showDatePicker.value = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Text(
            text = "RemindMe",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Simple personal reminders",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = reminderText.value,
                    onValueChange = { reminderText.value = it },
                    label = { Text("Reminder") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker.value = true }
                ) {
                    OutlinedTextField(
                        value = selectedDateTimeText.value,
                        onValueChange = {},
                        label = { Text("Date & Time") },
                        placeholder = { Text("Tap to choose") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (reminderText.value.isNotBlank() &&
                            selectedDateTimeMillis.longValue > 0L
                        ) {
                            viewModel.addReminder(
                                title = reminderText.value.trim(),
                                dueDateTimeMillis = selectedDateTimeMillis.longValue
                            )
                            reminderText.value = ""
                            selectedDateTimeText.value = ""
                            selectedDateTimeMillis.longValue = 0L
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Add Reminder",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your Reminders",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (reminders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No reminders yet.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onDelete = { viewModel.deleteReminder(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderRow(
    reminder: ReminderEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatDateTime(reminder.dueDateTimeMillis),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

private fun formatDateTime(millis: Long): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}