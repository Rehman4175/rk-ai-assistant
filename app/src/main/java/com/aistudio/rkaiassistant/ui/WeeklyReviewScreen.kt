package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewScreen(viewModel: AssistantViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val diaryEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()

    // Computations
    val completedTasks = tasks.count { it.isCompleted }
    val pendingTasks = tasks.count { !it.isCompleted }
    val totalTasks = tasks.size
    val taskCompletionRate = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 0

    val habitCompTotal = habits.size
    val habitLoggedCount = habits.count { it.loggedDaysCommaSeparated.isNotBlank() }
    val habitCompletionRate = if (habitCompTotal > 0) (habitLoggedCount * 100) / habitCompTotal else 0

    val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 3600 * 1000L)
    val weeklyUnpaidExpenses = expenses.filter { it.timestamp >= oneWeekAgo && !it.isIncome }
    val totalWeeklyExpense = weeklyUnpaidExpenses.sumOf { it.amount }

    val categorySumMap = weeklyUnpaidExpenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val recentDiaryCount = diaryEntries.count { it.timestamp >= oneWeekAgo }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEEKLY DIGEST",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan
                    )
                    Text(
                        text = "A retrospective overview of your productivity & logs",
                        fontSize = 11.sp,
                        color = SoftTextGray
                    )
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
                // Section 1: Productivity (Tasks & Habits)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBackgroundGlass)
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PRODUCTIVITY ENGAGEMENT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Tasks Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Completed: $completedTasks", color = NeonCyan, fontSize = 12.sp)
                                Text("Pending: $pendingTasks", color = SoftTextGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { taskCompletionRate / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = NeonCyan,
                                    trackColor = Color(0xFF1C1E32)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$taskCompletionRate% Done", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Habits Logged", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Active: ${habits.size}", color = NeonPink, fontSize = 12.sp)
                                Text("Logged this week: $habitLoggedCount", color = SoftTextGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { habitCompletionRate / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = NeonPink,
                                    trackColor = Color(0xFF1C1E32)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$habitCompletionRate% Logged", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 2: Expenses Category Breakdown & Diary Entries
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBackgroundGlass)
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "FINANCES & HEALTH JOURNAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPink,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Weekly Expense", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Rs $totalWeeklyExpense spent", color = Color(0xFFFFB300), fontSize = 15.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Category Breakdown:", color = SoftTextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (categorySumMap.isEmpty()) {
                                    Text("No expenses logged.", color = SoftTextGray, fontSize = 11.sp)
                                } else {
                                    categorySumMap.forEach { (cat, sum) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(cat, color = SoftTextGray, fontSize = 11.sp)
                                            Text("Rs $sum", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(0.8f)) {
                                Text("Diary Logged", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF141624))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$recentDiaryCount",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeonCyan
                                        )
                                        Text(
                                            text = "entries this week",
                                            color = SoftTextGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
