package com.vocalize.app.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vocalize.app.presentation.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class WidgetRecorderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VocalizeTheme {
                WidgetRecorderScreen(onFinish = { finish() })
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Activity goes behind another app — handled by ViewModel lifecycle
    }
}

@Composable
fun WidgetRecorderScreen(
    onFinish: () -> Unit,
    viewModel: WidgetRecorderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto-start recording when the overlay appears
    LaunchedEffect(Unit) {
        delay(200)
        viewModel.startRecording()
    }

    // Auto-close once saved
    LaunchedEffect(uiState.phase) {
        if (uiState.phase == WidgetRecorderPhase.SAVED) {
            delay(800)
            onFinish()
        }
    }

    // Translucent dim background that fills the whole screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            color = VocalizeGray800,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                when (uiState.phase) {
                    WidgetRecorderPhase.READY,
                    WidgetRecorderPhase.RECORDING -> RecordingPhase(uiState, viewModel, onFinish)

                    WidgetRecorderPhase.STOPPED,
                    WidgetRecorderPhase.SAVING -> SavePhase(uiState, viewModel, onFinish)

                    WidgetRecorderPhase.SAVED -> SavedConfirmation()

                    WidgetRecorderPhase.ERROR -> ErrorPhase(uiState.errorMessage, onFinish)
                }
            }
        }
    }
}

@Composable
private fun RecordingPhase(
    uiState: WidgetRecorderUiState,
    viewModel: WidgetRecorderViewModel,
    onFinish: () -> Unit
) {
    val isRecording = uiState.phase == WidgetRecorderPhase.RECORDING

    // Pulsing animation for the record indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isRecording) "Recording…" else "Starting…",
            color = VocalizeWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        TextButton(onClick = {
            viewModel.cancelRecording()
            onFinish()
        }) {
            Text("Cancel", color = VocalizeGray400, fontSize = 13.sp)
        }
    }

    Spacer(Modifier.height(28.dp))

    // Pulse ring + timer
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulse ring
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(VocalizeRed.copy(alpha = 0.18f))
        )
        // Inner red circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(VocalizeRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Recording",
                tint = VocalizeWhite,
                modifier = Modifier.size(34.dp)
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    // Elapsed timer
    Text(
        text = formatElapsed(uiState.elapsedMs),
        color = VocalizeWhite,
        fontSize = 28.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Text(
        text = "Tap stop to add details and save",
        color = VocalizeGray400,
        fontSize = 11.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )

    Spacer(Modifier.height(28.dp))

    // Stop button
    Button(
        onClick = { viewModel.stopRecording() },
        colors = ButtonDefaults.buttonColors(containerColor = VocalizeRed),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Icon(Icons.Default.Stop, contentDescription = null, tint = VocalizeWhite)
        Spacer(Modifier.width(8.dp))
        Text("Stop Recording", color = VocalizeWhite, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavePhase(
    uiState: WidgetRecorderUiState,
    viewModel: WidgetRecorderViewModel,
    onFinish: () -> Unit
) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showReminderMenu by remember { mutableStateOf(false) }

    // Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Save Memo",
            color = VocalizeWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = formatElapsed(uiState.duration),
            color = VocalizeGray400,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    }

    Spacer(Modifier.height(6.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(16.dp))

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title field
        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::setTitle,
            label = { Text("Title", color = VocalizeGray400) },
            placeholder = { Text("Voice Memo", color = VocalizeGray400.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VocalizeAccentBlue,
                unfocusedBorderColor = VocalizeGray400.copy(alpha = 0.4f),
                focusedTextColor = VocalizeWhite,
                unfocusedTextColor = VocalizeWhite
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Note field
        OutlinedTextField(
            value = uiState.note,
            onValueChange = viewModel::setNote,
            label = { Text("Note (optional)", color = VocalizeGray400) },
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VocalizeAccentBlue,
                unfocusedBorderColor = VocalizeGray400.copy(alpha = 0.4f),
                focusedTextColor = VocalizeWhite,
                unfocusedTextColor = VocalizeWhite
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Category picker
        if (uiState.categories.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = showCategoryMenu,
                onExpandedChange = { showCategoryMenu = it }
            ) {
                OutlinedTextField(
                    value = uiState.categories.find { it.id == uiState.selectedCategoryId }?.name ?: "No category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category", color = VocalizeGray400) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VocalizeAccentBlue,
                        unfocusedBorderColor = VocalizeGray400.copy(alpha = 0.4f),
                        focusedTextColor = VocalizeWhite,
                        unfocusedTextColor = VocalizeWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                    modifier = Modifier.background(VocalizeCardDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("No category", color = VocalizeGray400) },
                        onClick = { viewModel.setCategory(null); showCategoryMenu = false }
                    )
                    uiState.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name, color = VocalizeWhite) },
                            onClick = { viewModel.setCategory(cat.id); showCategoryMenu = false },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            try { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                                            catch (_: Exception) { VocalizeGray400 }
                                        )
                                )
                            }
                        )
                    }
                }
            }
        }

        // Reminder picker
        ExposedDropdownMenuBox(
            expanded = showReminderMenu,
            onExpandedChange = { showReminderMenu = it }
        ) {
            OutlinedTextField(
                value = uiState.reminderPreset.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Reminder", color = VocalizeGray400) },
                leadingIcon = {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = VocalizeOrange, modifier = Modifier.size(18.dp))
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showReminderMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VocalizeAccentBlue,
                    unfocusedBorderColor = VocalizeGray400.copy(alpha = 0.4f),
                    focusedTextColor = VocalizeWhite,
                    unfocusedTextColor = VocalizeWhite
                ),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = showReminderMenu,
                onDismissRequest = { showReminderMenu = false },
                modifier = Modifier.background(VocalizeCardDark)
            ) {
                ReminderPreset.entries.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.label, color = if (preset == ReminderPreset.NONE) VocalizeGray400 else VocalizeWhite) },
                        onClick = { viewModel.setReminderPreset(preset); showReminderMenu = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.cancelRecording()
                    onFinish()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VocalizeGray400),
                border = androidx.compose.foundation.BorderStroke(1.dp, VocalizeGray400.copy(alpha = 0.4f))
            ) {
                Text("Discard", fontSize = 14.sp)
            }

            Button(
                onClick = { viewModel.saveMemo() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VocalizeAccentBlue),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = VocalizeWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun SavedConfirmation() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(VocalizeGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = VocalizeGreen,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Saved!",
            color = VocalizeGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            "Your memo has been added.",
            color = VocalizeGray400,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ErrorPhase(message: String?, onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = VocalizeRed, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text("Recording Failed", color = VocalizeRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            message ?: "Check microphone permission in Settings.",
            color = VocalizeGray400,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = VocalizeGray400.copy(alpha = 0.3f))
        ) {
            Text("Close", color = VocalizeWhite)
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
