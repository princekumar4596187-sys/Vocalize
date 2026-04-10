package com.vocalize.app.presentation.recorder

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.vocalize.app.presentation.components.WaveformView
import com.vocalize.app.presentation.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecorderScreen(
    onClose: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: RecorderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val micPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    // Animate record button
    val recordButtonScale by animateFloatAsState(
        targetValue = if (uiState.isRecording) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessHigh),
        label = "record_scale"
    )
    val recordButtonColor by animateColorAsState(
        targetValue = if (uiState.isRecording) Color(0xFFDC2626) else VocalizeRed,
        animationSpec = tween(300),
        label = "record_color"
    )

    // Infinite pulsing ring when recording
    val infiniteTransition = rememberInfiniteTransition(label = "record_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VocalizeGray900, VocalizeGray800)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.cancelRecording()
                        onClose()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Cancel", tint = Color.White)
                }

                Text(
                    text = if (uiState.isRecording) "Recording..." else if (uiState.isStopped) "Preview" else "New Memo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                AnimatedVisibility(visible = uiState.isStopped) {
                    IconButton(
                        onClick = {
                            viewModel.save()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(VocalizeGreen.copy(0.2f), CircleShape)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = VocalizeGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, "Save", tint = VocalizeGreen)
                        }
                    }
                }

                if (!uiState.isStopped) {
                    Spacer(Modifier.size(44.dp))
                }
            }

            // Timer display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatRecordingTime(uiState.elapsedMs),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isRecording) VocalizeRed else Color.White,
                    letterSpacing = 4.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // Waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                WaveformView(
                    amplitudes = uiState.amplitudeHistory,
                    isRecording = uiState.isRecording,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.weight(1f))

            // Record button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing ring (only when recording)
                if (uiState.isRecording) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha)
                            .background(VocalizeRed.copy(alpha = 0.3f), CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(recordButtonScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(recordButtonColor, recordButtonColor.copy(alpha = 0.8f))
                            ),
                            shape = if (uiState.isRecording) RoundedCornerShape(24.dp) else CircleShape
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!micPermission.status.isGranted) {
                                micPermission.launchPermissionRequest()
                                return@clickable
                            }
                            if (uiState.isRecording) {
                                viewModel.stopRecording()
                            } else {
                                viewModel.startRecording()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (uiState.isRecording) "Stop" else "Record",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Preview / save section (shown after stopping)
            AnimatedVisibility(
                visible = uiState.isStopped,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(0.06f),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(24.dp)
                ) {
                    // Playback preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { viewModel.togglePreviewPlayback() },
                            modifier = Modifier
                                .size(52.dp)
                                .background(VocalizeRed.copy(0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (uiState.isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Preview",
                                tint = VocalizeRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Title field
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("Title (optional)") },
                        placeholder = {
                            Text(
                                uiState.transcription.ifBlank { "Voice Memo" },
                                color = Color.White.copy(0.4f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VocalizeRed,
                            focusedLabelColor = VocalizeRed
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Note field
                    OutlinedTextField(
                        value = uiState.textNote,
                        onValueChange = { viewModel.updateNote(it) },
                        label = { Text("Add a note (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VocalizeRed,
                            focusedLabelColor = VocalizeRed
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Set reminder toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (uiState.setReminderAfterSave) VocalizeOrange else Color.White.copy(0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Set reminder after saving",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(0.8f)
                            )
                        }
                        Switch(
                            checked = uiState.setReminderAfterSave,
                            onCheckedChange = { viewModel.toggleSetReminder(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = VocalizeOrange, checkedTrackColor = VocalizeOrange.copy(0.3f))
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Save button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.save(onSaved = onSaved)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VocalizeRed),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Save Memo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatRecordingTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"
}
