package com.vocalize.app.presentation.category

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.vocalize.app.data.local.entity.CategoryEntity
import com.vocalize.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    onBack: () -> Unit,
    viewModel: CategoryManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manage Categories",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add category", tint = VocalizeRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VocalizeRed)
            }
            return@Scaffold
        }

        if (uiState.categories.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Label,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No categories yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Category")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "${uiState.categories.size} categories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            itemsIndexed(uiState.categories, key = { _, c -> c.id }) { index, category ->
                CategoryItem(
                    category = category,
                    index = index,
                    onEdit = { editingCategory = category },
                    onDelete = { viewModel.deleteCategory(category) }
                )
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            title = "New Category",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, colorHex, icon ->
                viewModel.addCategory(name, colorHex, icon)
                showAddDialog = false
            }
        )
    }

    editingCategory?.let { cat ->
        CategoryDialog(
            title = "Edit Category",
            initialName = cat.name,
            initialColor = cat.colorHex,
            initialIcon = cat.iconName,
            onDismiss = { editingCategory = null },
            onConfirm = { name, colorHex, icon ->
                viewModel.updateCategory(cat.copy(name = name, colorHex = colorHex, iconName = icon))
                editingCategory = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryItem(
    category: CategoryEntity,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val enterAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 50L)
        enterAnim.animateTo(1f, tween(300))
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    val categoryColor = runCatching {
        Color(android.graphics.Color.parseColor(category.colorHex))
    }.getOrDefault(VocalizeRed)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(enterAnim.value)
            .offset(y = ((1f - enterAnim.value) * 20).dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(categoryColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(categoryColor, CircleShape)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = category.colorHex,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Category") },
            text = { Text("Delete \"${category.name}\"? Memos with this category will be uncategorised.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CategoryDialog(
    title: String,
    initialName: String = "",
    initialColor: String = "#EF4444",
    initialIcon: String = "label",
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String, icon: String) -> Unit
) {
    val presetColors = listOf(
        "#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#8B5CF6",
        "#EC4899", "#06B6D4", "#84CC16", "#F97316", "#6B7280"
    )

    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Category name") },
                    isError = nameError,
                    supportingText = if (nameError) ({ Text("Name required") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VocalizeRed,
                        focusedLabelColor = VocalizeRed
                    )
                )

                Text(
                    "Colour",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetColors) { color ->
                        val c = runCatching { Color(android.graphics.Color.parseColor(color)) }
                            .getOrDefault(VocalizeRed)
                        Box(
                            modifier = Modifier
                                .size(if (selectedColor == color) 38.dp else 32.dp)
                                .background(c, CircleShape)
                                .border(
                                    width = if (selectedColor == color) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    onConfirm(name.trim(), selectedColor, initialIcon)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VocalizeRed)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
