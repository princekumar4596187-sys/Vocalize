package com.vocalize.app.presentation.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vocalize.app.data.local.entity.ReminderLogEntity
import com.vocalize.app.presentation.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRemindersScreen(
    onNavigateBack: () -> Unit,
    viewModel: AllRemindersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedLog by remember { mutableStateOf<ReminderLogEntity?>(null) }
    var selectedTitle by remember { mutableStateOf("") }

    // Tick every second to update countdowns
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMs = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "All Reminders",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VocalizeAccentBlue)
            }
        } else if (uiState.upcoming.isEmpty() && uiState.past.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        tint = VocalizeGray400,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No reminders yet",
                        color = VocalizeGray400,
                        fontSize = 16.sp
                    )
                    Text(
                        "Set a reminder on any voice memo to see it here.",
                        color = VocalizeGray400,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Upcoming section
                if (uiState.upcoming.isNotEmpty()) {
                    item {
                        SectionHeader(
                            label = "Upcoming",
                            count = uiState.upcoming.size,
                            accentColor = VocalizeAccentBlue
                        )
                    }
                    items(uiState.upcoming, key = { it.reminder.id }) { item ->
                        val remaining = item.reminder.reminderTime - nowMs
                        ReminderItemCard(
                            item = item,
                            statusIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(VocalizeOrange.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Alarm,
                                        contentDescription = "Upcoming",
                                        tint = VocalizeOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            badge = {
                                Text(
                                    text = formatCountdown(remaining),
                                    color = VocalizeAccentBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            onDebugClick = null
                        )
                    }
                }

                // Past section
                if (uiState.past.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        SectionHeader(
                            label = "Past",
                            count = uiState.past.size,
                            accentColor = VocalizeGray400
                        )
                    }
                    items(uiState.past, key = { it.reminder.id }) { item ->
                        val fired = item.log != null
                        ReminderItemCard(
                            item = item,
                            statusIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (fired) VocalizeGreen.copy(alpha = 0.15f)
                                            else VocalizeRed.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (fired) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = if (fired) "Fired" else "Did not fire",
                                        tint = if (fired) VocalizeGreen else VocalizeRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            badge = {
                                Text(
                                    text = if (fired) "Delivered" else "No record",
                                    color = if (fired) VocalizeGreen else VocalizeRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            onDebugClick = {
                                selectedTitle = item.memoTitle
                                selectedLog = item.log ?: ReminderLogEntity(
                                    id = "no_log",
                                    reminderId = item.reminder.id,
                                    memoId = item.reminder.memoId,
                                    memoTitle = item.memoTitle,
                                    scheduledTime = item.reminder.reminderTime,
                                    firedTime = 0L,
                                    diagnostics = buildNoLogDiagnostics(item)
                                )
                            }
                        )
                    }
                }

                // Bottom padding
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // Debug Console dialog
    selectedLog?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedLog = null },
            containerColor = VocalizeCardDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        tint = VocalizeAccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Debug Console",
                        color = VocalizeWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = selectedTitle,
                        color = VocalizeGray200,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = VocalizeGray900,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = log.diagnostics,
                            color = VocalizeGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLog = null }) {
                    Text("Close", color = VocalizeAccentBlue)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(label: String, count: Int, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.width(6.dp))
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = count.toString(),
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun ReminderItemCard(
    item: ReminderItem,
    statusIcon: @Composable () -> Unit,
    badge: @Composable () -> Unit,
    onDebugClick: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = VocalizeCardDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            statusIcon()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.memoTitle,
                    color = VocalizeWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatDateTime(item.reminder.reminderTime),
                    color = VocalizeGray400,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(4.dp))
                badge()
            }
            if (onDebugClick != null) {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onDebugClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = "Debug console",
                        tint = VocalizeGray400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatCountdown(remainingMs: Long): String {
    if (remainingMs <= 0) return "Now"
    val totalSec = remainingMs / 1000
    val days = totalSec / 86400
    val hours = (totalSec % 86400) / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m ${seconds}s"
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatDateTime(timeMs: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(timeMs))

private fun buildNoLogDiagnostics(item: ReminderItem): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return buildString {
        appendLine("=== Reminder Did Not Fire ===")
        appendLine("Memo: ${item.memoTitle}")
        appendLine("Reminder ID: ${item.reminder.id}")
        appendLine("Scheduled: ${fmt.format(Date(item.reminder.reminderTime))}")
        appendLine()
        appendLine("=== Possible Causes ===")
        appendLine("• AlarmManager did not deliver the broadcast.")
        appendLine("• Device was in Doze mode and no alarm wakelock was held.")
        appendLine("• App was force-stopped by the user or system before alarm fired.")
        appendLine("• USE_EXACT_ALARM permission was revoked by the OS.")
        appendLine("• The reminder was cancelled or overwritten before firing.")
        appendLine()
        appendLine("=== Recommended Actions ===")
        appendLine("• Disable battery optimisation for Vocalize in Settings.")
        appendLine("• Grant 'Schedule exact alarms' permission.")
        appendLine("• Do not force-stop the app.")
    }.trimEnd()
}
