package com.vocalize.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vocalize.app.data.local.entity.TagEntity
import com.vocalize.app.presentation.theme.VocalizeAccentBlue
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSelectionBottomSheet(
    tags: List<TagEntity>,
    selectedTagIds: Set<String>,
    onDismiss: () -> Unit,
    onToggleTag: (String) -> Unit,
    onCreateTag: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newTagName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Tag memo", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                label = { Text("New tag") },
                placeholder = { Text("e.g. Meeting") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (newTagName.isNotBlank()) {
                        onCreateTag(newTagName.trim())
                        newTagName = ""
                    }
                },
                enabled = newTagName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Create tag")
            }

            Spacer(Modifier.height(20.dp))
            Text("Assigned tags", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            tags.forEach { tag ->
                FilterChip(
                    selected = selectedTagIds.contains(tag.id),
                    onClick = { onToggleTag(tag.id) },
                    label = { Text(tag.name) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
