package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(viewModel: AssistantViewModel) {
    val habitsList by viewModel.habits.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var habitName by remember { mutableStateOf("") }
    var habitType by remember { mutableStateOf("Meditation") }

    val todayStr = viewModel.getTodayDateString()

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
                    text = "HABIT TERMINAL",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonGreen
                )

                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NeonGreen.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = NeonGreen)
                }
            }

            // Habits list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (habitsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Start by adding a productive habit.",
                                color = SoftTextGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                items(habitsList) { habit ->
                    val isLoggedToday = habit.loggedDaysCommaSeparated.split(",").contains(todayStr)
                    HabitCard(
                        habit = habit,
                        isLoggedToday = isLoggedToday,
                        onLogToday = { viewModel.logHabitToday(habit) },
                        onDelete = { viewModel.deleteHabit(habit) }
                    )
                }
            }
        }

        // Add Habit Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = CardBackgroundGlass,
                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                title = { Text("ESTABLISH NEW HABIT", color = NeonGreen, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = habitName,
                            onValueChange = { habitName = it },
                            label = { Text("Habit tracker name (e.g. Code daily)", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Habit Category dropdown selector
                        Text("Categorization", color = SoftTextGray, fontSize = 12.sp)
                        val types = listOf("Prayer", "Exercise", "Study", "Meditation", "Reading", "Custom")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            types.take(3).forEach { t ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (habitType == t) NeonGreen else Color(0xFF141624))
                                        .clickable { habitType = t }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(t, color = if (habitType == t) Color.Black else SoftTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            types.takeLast(3).forEach { t ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (habitType == t) NeonGreen else Color(0xFF141624))
                                        .clickable { habitType = t }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(t, color = if (habitType == t) Color.Black else SoftTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (habitName.isNotBlank()) {
                                viewModel.addHabit(habitName, habitType)
                                habitName = ""
                                showDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text("ESTABLISH", color = Color.Black, fontWeight = FontWeight.Bold)
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

@Composable
fun HabitCard(
    habit: Habit,
    isLoggedToday: Boolean,
    onLogToday: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = habit.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    
                    val totalLogged = habit.loggedDaysCommaSeparated.split(",").count { it.isNotBlank() }
                    val xp = (totalLogged * 10) + (habit.streakCount * 50)
                    val level = (xp / 500) + 1
                    val nextLevelXp = level * 500
                    val progress = (xp % 500) / 500f

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = NeonGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonGreen)
                        ) {
                            Text(
                                text = "LVL $level",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Streak: 🔥 ${habit.streakCount} • XP: $xp",
                            fontSize = 12.sp,
                            color = SoftTextGray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Level progress bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = NeonGreen,
                        trackColor = Color.Black.copy(alpha = 0.3f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isLoggedToday) NeonGreen else Color(0xFF1B1D2F))
                            .clickable(enabled = !isLoggedToday) { onLogToday() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Audit log complete icon",
                            tint = if (isLoggedToday) Color.Black else SoftTextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = SoftTextGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Last 7 Days Heatmap Simulation dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val cal = java.util.Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val daySdf = SimpleDateFormat("EE", Locale.US)

                for (i in 6 downTo 0) {
                    val loopCal = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.DAY_OF_YEAR, -i)
                    }
                    val dateKey = sdf.format(loopCal.time)
                    val label = daySdf.format(loopCal.time)
                    val isChecked = habit.loggedDaysCommaSeparated.split(",").contains(dateKey)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = label.take(1), fontSize = 10.sp, color = SoftTextGray, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isChecked) NeonGreen else Color(0xFF1B1D2F))
                                .border(1.dp, if (isChecked) Color.White else BorderColor, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
