package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Task
import com.example.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerScreen(viewModel: AssistantViewModel) {
    val tasksList by viewModel.tasks.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    
    var taskTitle by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf("Medium") }
    var taskLabel by remember { mutableStateOf("Personal") }
    var taskDueDate by remember { mutableStateOf("") }
    var taskNotes by remember { mutableStateOf("") }

    LaunchedEffect(editingTask) {
        if (editingTask != null) {
            taskTitle = editingTask!!.title
            taskPriority = editingTask!!.priority
            taskLabel = editingTask!!.label
            taskDueDate = editingTask!!.dueDate
            taskNotes = editingTask!!.notes
        } else {
            taskTitle = ""
            taskPriority = "Medium"
            taskLabel = "Personal"
            taskDueDate = ""
            taskNotes = ""
        }
    }

    var selectedTab by remember { mutableStateOf("Pending") }

    val filteredTasks = when (selectedTab) {
        "Pending" -> tasksList.filter { !it.isCompleted && !it.isDeleted }
        "Completed" -> tasksList.filter { it.isCompleted && !it.isDeleted }
        else -> tasksList.filter { !it.isDeleted }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TASK LOG",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan
                )

                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NeonCyan.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = NeonCyan)
                }
            }

            // Tabs for Pending vs Completed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141624))
            ) {
                listOf("Pending", "Completed", "All").forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = tab }
                            .background(if (isSelected) NeonCyan else Color.Transparent)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.Black else SoftTextGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Task List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks found here.",
                                color = SoftTextGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                items(filteredTasks) { task ->
                    TaskCard(
                        task = task,
                        onCompleteToggle = { viewModel.toggleTaskCompletion(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        onEdit = {
                            editingTask = task
                            showDialog = true
                        }
                    )
                }
            }
        }

        // Add/Edit Task Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showDialog = false
                    editingTask = null
                },
                containerColor = CardBackgroundGlass,
                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                title = { Text(if (editingTask != null) "EDIT STRATEGIC TASK" else "NEW STRATEGIC TASK", color = NeonCyan, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Task Title", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Priority selection row
                        Text("Priority", color = SoftTextGray, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Low", "Medium", "High").forEach { pr ->
                                val isSelected = taskPriority == pr
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonCyan else Color(0xFF141624))
                                        .clickable { taskPriority = pr }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pr,
                                        color = if (isSelected) Color.Black else SoftTextGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = taskLabel,
                            onValueChange = { taskLabel = it },
                            label = { Text("Label (e.g. Work, Health)", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = taskDueDate,
                            onValueChange = { taskDueDate = it },
                            label = { Text("Due Date (YYYY-MM-DD)", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = taskNotes,
                            onValueChange = { taskNotes = it },
                            label = { Text("Subnotes / Subtasks details", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                if (editingTask != null) {
                                    viewModel.updateTask(editingTask!!.copy(
                                        title = taskTitle,
                                        priority = taskPriority,
                                        label = taskLabel,
                                        dueDate = taskDueDate,
                                        notes = taskNotes
                                    ))
                                } else {
                                    viewModel.addTask(
                                        title = taskTitle,
                                        priority = taskPriority,
                                        label = taskLabel,
                                        dueDate = taskDueDate,
                                        notes = taskNotes,
                                        isRepeating = false,
                                        repeatInterval = "None"
                                    )
                                }
                                showDialog = false
                                editingTask = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text(if (editingTask != null) "UPDATE TASK" else "ADD TASK", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showDialog = false
                        editingTask = null
                    }) {
                        Text("CANCEL", color = SoftTextGray)
                    }
                }
            )
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onCompleteToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val decoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
    val priorityColor = when (task.priority) {
        "High" -> NeonPink
        "Medium" -> NeonCyan
        else -> NeonGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Checkbox circle
                IconButton(onClick = onCompleteToggle) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle Complete",
                        tint = if (task.isCompleted) NeonGreen else SoftTextGray,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "#${task.id} ${task.title}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) SoftTextGray else Color.White,
                        textDecoration = decoration
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Priority Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(priorityColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.priority + " Priority",
                                color = priorityColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (task.dueDate.isNotBlank()) {
                            Text(
                                text = "Due: ${task.dueDate}",
                                color = SoftTextGray,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF141624))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.label,
                                color = SoftTextGray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.notes,
                            fontSize = 12.sp,
                            color = SoftTextGray
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit task",
                        tint = SoftTextGray
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = SoftTextGray
                    )
                }
            }
        }
    }
}
