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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.AppScreen
import com.example.viewmodel.AssistantViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: AssistantViewModel) {
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        delay(300)
        debouncedQuery = query
    }

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val diaryEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val smartReminders by viewModel.smartReminders.collectAsStateWithLifecycle()
    val standardReminders by viewModel.reminders.collectAsStateWithLifecycle()

    val matchedTasks = if (debouncedQuery.isBlank()) emptyList() else tasks.filter { it.title.contains(debouncedQuery, ignoreCase = true) || it.notes.contains(debouncedQuery, ignoreCase = true) }
    val matchedNotes = if (debouncedQuery.isBlank()) emptyList() else notes.filter { it.title.contains(debouncedQuery, ignoreCase = true) || it.content.contains(debouncedQuery, ignoreCase = true) }
    val matchedExpenses = if (debouncedQuery.isBlank()) emptyList() else expenses.filter { it.title.contains(debouncedQuery, ignoreCase = true) || it.category.contains(debouncedQuery, ignoreCase = true) }
    val matchedEvents = if (debouncedQuery.isBlank()) emptyList() else events.filter { it.title.contains(debouncedQuery, ignoreCase = true) || it.notes.contains(debouncedQuery, ignoreCase = true) }
    val matchedDiary = if (debouncedQuery.isBlank()) emptyList() else diaryEntries.filter { it.text.contains(debouncedQuery, ignoreCase = true) }
    val matchedBills = if (debouncedQuery.isBlank()) emptyList() else bills.filter { it.name.contains(debouncedQuery, ignoreCase = true) || it.category.contains(debouncedQuery, ignoreCase = true) }
    val matchedHabits = if (debouncedQuery.isBlank()) emptyList() else habits.filter { it.name.contains(debouncedQuery, ignoreCase = true) }
    val matchedMemories = if (debouncedQuery.isBlank()) emptyList() else memories.filter { it.content.contains(debouncedQuery, ignoreCase = true) || it.category.contains(debouncedQuery, ignoreCase = true) }
    val matchedSmartReminders = if (debouncedQuery.isBlank()) emptyList() else smartReminders.filter { it.title.contains(debouncedQuery, ignoreCase = true) }
    val matchedStandardReminders = if (debouncedQuery.isBlank()) emptyList() else standardReminders.filter { it.title.contains(debouncedQuery, ignoreCase = true) }

    val totalMatches = matchedTasks.size + matchedNotes.size + matchedExpenses.size + matchedEvents.size + matchedDiary.size + matchedBills.size + matchedHabits.size + matchedMemories.size + matchedSmartReminders.size + matchedStandardReminders.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UNIVERSAL ENGINE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Central query input text field
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search logs, archives, notes...", color = SoftTextGray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, "Scan icon", tint = NeonCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Query lists results
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (debouncedQuery.isBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Enter a keyword above to scan database archives.",
                                color = SoftTextGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (totalMatches == 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No records matched your search query.",
                                color = SoftTextGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Matched tasks section
                if (matchedTasks.isNotEmpty()) {
                    item {
                        ResultHeader("TASKS (${matchedTasks.size})", NeonCyan)
                    }
                    items(matchedTasks) { task ->
                        ResultCard(
                            title = task.title,
                            desc = "Label: ${task.label} • Priority: ${task.priority}",
                            categoryColor = NeonCyan,
                            icon = Icons.Default.FormatListNumbered,
                            onClick = { viewModel.currentScreen.value = AppScreen.Tasks }
                        )
                    }
                }

                // Matched Smart Reminders section
                if (matchedSmartReminders.isNotEmpty()) {
                    item {
                        ResultHeader("SMART REMINDERS (${matchedSmartReminders.size})", Color(0xFFE040FB))
                    }
                    items(matchedSmartReminders) { rem ->
                        ResultCard(
                            title = rem.title,
                            desc = "Priority: ${rem.priority} • Repeat Interval: ${rem.repeatIntervalMinutes}m",
                            categoryColor = Color(0xFFE040FB),
                            icon = Icons.Default.NotificationsActive,
                            onClick = { viewModel.currentScreen.value = AppScreen.SmartReminders }
                        )
                    }
                }

                // Matched Standard Reminders section
                if (matchedStandardReminders.isNotEmpty()) {
                    item {
                        ResultHeader("STANDARD REMINDERS (${matchedStandardReminders.size})", Color(0xFF9575CD))
                    }
                    items(matchedStandardReminders) { rem ->
                        ResultCard(
                            title = rem.title,
                            desc = "One-time alert • Status: Active",
                            categoryColor = Color(0xFF9575CD),
                            icon = Icons.Default.Notifications,
                            onClick = { viewModel.currentScreen.value = AppScreen.Reminders }
                        )
                    }
                }

                // Matched notes section
                if (matchedNotes.isNotEmpty()) {
                    item {
                        ResultHeader("NOTES (${matchedNotes.size})", NeonPink)
                    }
                    items(matchedNotes) { note ->
                        ResultCard(
                            title = note.title,
                            desc = if (note.content.length > 80) note.content.take(80) + "..." else note.content,
                            categoryColor = NeonPink,
                            icon = Icons.Default.Bookmark,
                            onClick = { viewModel.currentScreen.value = AppScreen.Notes }
                        )
                    }
                }

                // Matched expenses section
                if (matchedExpenses.isNotEmpty()) {
                    item {
                        ResultHeader("EXPENSES & INCOME (${matchedExpenses.size})", Color(0xFFFF9100))
                    }
                    items(matchedExpenses) { ex ->
                        ResultCard(
                            title = ex.title,
                            desc = "Amount: Rs ${ex.amount} • Category: ${ex.category} • ${if (ex.isIncome) "Income" else "Expense"}",
                            categoryColor = Color(0xFFFF9100),
                            icon = Icons.Default.TrendingDown,
                            onClick = { viewModel.currentScreen.value = AppScreen.Expenses }
                        )
                    }
                }

                // Matched calendar events section
                if (matchedEvents.isNotEmpty()) {
                    item {
                        ResultHeader("CALENDAR EVENTS (${matchedEvents.size})", NeonGreen)
                    }
                    items(matchedEvents) { event ->
                        ResultCard(
                            title = event.title,
                            desc = "Due: ${event.dateString} at ${event.timeString} • Location: ${event.location}",
                            categoryColor = NeonGreen,
                            icon = Icons.Default.EventNote,
                            onClick = { viewModel.currentScreen.value = AppScreen.Calendar }
                        )
                    }
                }

                // Matched diary section
                if (matchedDiary.isNotEmpty()) {
                    item {
                        ResultHeader("DIARY ENTRIES (${matchedDiary.size})", Color(0xFF00E5FF))
                    }
                    items(matchedDiary) { entry ->
                        ResultCard(
                            title = "Diary Entry: ${entry.dateString}",
                            desc = if (entry.text.length > 80) entry.text.take(80) + "..." else entry.text,
                            categoryColor = Color(0xFF00E5FF),
                            icon = Icons.Default.MenuBook,
                            onClick = { viewModel.currentScreen.value = AppScreen.Diary }
                        )
                    }
                }

                // Matched bills section
                if (matchedBills.isNotEmpty()) {
                    item {
                        ResultHeader("BILL TRACKER (${matchedBills.size})", Color(0xFFFFD600))
                    }
                    items(matchedBills) { bill ->
                        ResultCard(
                            title = bill.name,
                            desc = "Amount: Rs ${bill.amount} • Due Day of Month: ${bill.dueDayOfMonth}",
                            categoryColor = Color(0xFFFFD600),
                            icon = Icons.Default.ReceiptLong,
                            onClick = { viewModel.currentScreen.value = AppScreen.Bills }
                        )
                    }
                }

                // Matched habits section
                if (matchedHabits.isNotEmpty()) {
                    item {
                        ResultHeader("HABIT TRACKER (${matchedHabits.size})", Color(0xFF1DE9B6))
                    }
                    items(matchedHabits) { habit ->
                        ResultCard(
                            title = habit.name,
                            desc = "Type: ${habit.type} • Streak: ${habit.streakCount} days",
                            categoryColor = Color(0xFF1DE9B6),
                            icon = Icons.Default.DirectionsRun,
                            onClick = { viewModel.currentScreen.value = AppScreen.Habits }
                        )
                    }
                }

                // Matched memories section
                if (matchedMemories.isNotEmpty()) {
                    item {
                        ResultHeader("AI PERSONAL MEMORIES (${matchedMemories.size})", Color(0xFFFF5252))
                    }
                    items(matchedMemories) { memory ->
                        ResultCard(
                            title = "Memory Fact: ${memory.category}",
                            desc = memory.content,
                            categoryColor = Color(0xFFFF5252),
                            icon = Icons.Default.Psychology,
                            onClick = { viewModel.currentScreen.value = AppScreen.Dashboard }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultHeader(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun ResultCard(
    title: String,
    desc: String,
    categoryColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(categoryColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = categoryColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = desc, color = SoftTextGray, fontSize = 11.sp)
            }
        }
    }
}
