package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringRemindersScreen(viewModel: AssistantViewModel) {
    val remindersList by viewModel.recurringReminders.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Daily") }
    var time by remember { mutableStateOf("08:00") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECURRING OPS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonPink
                )

                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NeonPink.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = NeonPink)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (remindersList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recurring reminders configured.",
                                color = SoftTextGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                items(remindersList) { reminder ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBackgroundGlass)
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Repeat, null, tint = NeonPink, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = reminder.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "${reminder.type} at ${reminder.time}", fontSize = 12.sp, color = SoftTextGray)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteRecurringReminder(reminder) }) {
                                Icon(Icons.Default.Delete, null, tint = SoftTextGray)
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = CardBackgroundGlass,
                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                title = { Text("NEW RECURRING ALERT", color = NeonPink, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = type,
                            onValueChange = { type = it },
                            label = { Text("Type (Daily/Weekly/Monthly)", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Time (HH:MM)", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
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
                            if (title.isNotBlank()) {
                                viewModel.addRecurringReminder(title, type, time)
                                title = ""
                                showDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                    ) {
                        Text("SCHEDULE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("CANCEL", color = SoftTextGray)
                    }
                }
            )
        }
    }
}
