package com.vocalize.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.vocalize.app.data.local.entity.RepeatType
import com.vocalize.app.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderBottomSheet(
    currentReminderTime: Long?,
    currentRepeatType: RepeatType,
    currentCustomDays: String = "",
    onDismiss: () -> Unit,
    onSave: (Long, RepeatType, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRepeat by remember { mutableStateOf(currentRepeatType) }
    var customDays by remember { mutableStateOf(currentCustomDays) }

    val initialTime = currentReminderTime ?: Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTime)

    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = initialTime }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = initialTime }.get(Calendar.MINUTE)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, null, tint = VocalizeOrange, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Set Reminder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            // Date picker
            Text("Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            DatePicker(
                state = datePickerState,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Time row
            Text("Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            TimeInput(
                state = timePickerState,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Repeat type
            Text("Repeat", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatType.values().forEach { repeatType ->
                    FilterChip(
                        selected = selectedRepeat == repeatType,
                        onClick = { selectedRepeat = repeatType },
                        label = {
                            Text(
                                repeatType.name.replace("_", " ").lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            if (selectedRepeat == RepeatType.CUSTOM_DAYS) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customDays,
                    onValueChange = { customDays = it },
                    label = { Text("Days (1=Mon, 7=Sun, comma-separated)") },
                    placeholder = { Text("e.g. 1,3,5") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis ?: initialTime
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onSave(cal.timeInMillis, selectedRepeat, customDays)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VocalizeOrange)
                ) { Text("Set Reminder", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
