package com.example.ui

import com.example.ui.theme.*
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
import com.example.data.GeminiService
import com.example.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewScreen(viewModel: AssistantViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val diaryEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var aiAdvice by remember { mutableStateOf("Ready to analyze your progress. Click below to receive customized AI review logs & motivation.") }
    var adviceLoading by remember { mutableStateOf(false) }

    // Computations
    val completedTasks = tasks.count { it.isCompleted }
    val pendingTasks = tasks.count { !it.isCompleted }
    val totalTasks = tasks.size
    val taskCompletionRate = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 0

    // Habit completion rate
    // Count days logged in general, or average streak
    val habitCompTotal = habits.size
    val habitLoggedCount = habits.count { it.loggedDaysCommaSeparated.isNotBlank() }
    val habitCompletionRate = if (habitCompTotal > 0) (habitLoggedCount * 100) / habitCompTotal else 0

    // Total expenses (last 7 days)
    val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 3600 * 1000L)
    val weeklyUnpaidExpenses = expenses.filter { it.timestamp >= oneWeekAgo && !it.isIncome }
    val totalWeeklyExpense = weeklyUnpaidExpenses.sumOf { it.amount }

    // Category breakdown
    val categorySumMap = weeklyUnpaidExpenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val recentDiaryCount = diaryEntries.count { it.timestamp >= oneWeekAgo }

    fun generateAiWeeklyReviewFeedback() {
        scope.launch {
            adviceLoading = true
            aiAdvice = "Formulating neural performance summaries..."
            val prompt = """
                The user has the following weekly statistics:
                - Tasks Completed vs Pending: $completedTasks completed out of $totalTasks total tasks ($taskCompletionRate% completion rate).
                - Habits Check-In Rate: $habitCompletionRate% of habits checked-in this week.
                - Total Expenses: Rs $totalWeeklyExpense
                - Diary Journal Entries Logged: $recentDiaryCount entries in the last 7 days.
                
                Provide a highly motivational, encouraging 2-3 sentence progress report as their personal assistant 'Rk'. Praise their accomplishments or give practical advice for pending items. Be warm, professional, and positive.
            """.trimIndent()
            
            val feedback = GeminiService.chat(prompt = prompt, systemInstruction = "You are RK, a motivational personal AI assistant.")
            aiAdvice = feedback ?: "I couldn't analyze your stats right now because I'm offline. But from what I see, you're making steady progress! Keep up the good work and I'll give you a detailed report once we're back online."
            adviceLoading = false
        }
    }

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

                IconButton(
                    onClick = { generateAiWeeklyReviewFeedback() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NeonCyan.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Action", tint = NeonCyan)
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
                            // Tasks Widget
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

                            // Habits Widget
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
                            // Financial widget
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

                            // Diary widget
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

                // Section 3: AI Assistant review section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF10121F))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "RK AI ASSISTANT FEEDBACK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = aiAdvice,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (!adviceLoading) {
                            Button(
                                onClick = { generateAiWeeklyReviewFeedback() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Text("Generate AI Review Summary", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
