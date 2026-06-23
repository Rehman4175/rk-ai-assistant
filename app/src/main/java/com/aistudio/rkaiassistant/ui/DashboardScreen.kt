package com.aistudio.rkaiassistant.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aistudio.rkaiassistant.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel
import com.aistudio.rkaiassistant.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: AssistantViewModel,
    onNavigate: (String) -> Unit
) {
    val isJarvisActive by viewModel.isJarvisActive.collectAsStateWithLifecycle()
    val jarvisStatus by viewModel.jarvisStatus.collectAsStateWithLifecycle()

    val tasksList by viewModel.tasks.collectAsStateWithLifecycle()
    val habitsList by viewModel.habits.collectAsStateWithLifecycle()
    val remindersList by viewModel.reminders.collectAsStateWithLifecycle()
    val notesList by viewModel.notes.collectAsStateWithLifecycle()
    val memoriesList by viewModel.memories.collectAsStateWithLifecycle()
    val expensesList by viewModel.expenses.collectAsStateWithLifecycle()
    val billsList by viewModel.bills.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.events.collectAsStateWithLifecycle()
    val diaryList by viewModel.diaryEntries.collectAsStateWithLifecycle()
    val weatherTerminal by viewModel.weatherTerminal.collectAsStateWithLifecycle()

    val todayStr = viewModel.getTodayDateString()
    val waterSum by viewModel.todayWaterSum.collectAsStateWithLifecycle()

    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

    val todayFormatted = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(Date())

    var currentTime by remember { mutableStateOf(SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    val pendingTasksCount = tasksList.count { !it.isCompleted }
    val activeRemindersCount = remindersList.count { !it.isAcknowledged }
    val totalSpends = expensesList.filter { !it.isIncome && it.dateString.startsWith(todayStr.take(7)) }.sumOf { it.amount }
    val maxBudget = viewModel.prefs.getExpenseBudget()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(SlateDarkBackground)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome and Time terminal
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1B1D30), Color(0xFF0F111E))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WELCOME BACK RK",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Briefing ready. Type /briefing in console.",
                                    color = SoftTextGray,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.clickable { 
                                        viewModel.sendDaySummary()
                                        onNavigate("chat")
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Image(
                                painter = painterResource(id = R.drawable.img_app_logo_1781776003478),
                                contentDescription = stringResource(R.string.app_logo_desc),
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, NeonCyan, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Divider(color = BorderColor, thickness = 1.dp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "CURRENT TIME",
                                    fontSize = 10.sp,
                                    color = SoftTextGray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentTime,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.calendar_period_label),
                                    fontSize = 10.sp,
                                    color = SoftTextGray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = todayFormatted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "WEATHER TERMINAL",
                                    fontSize = 10.sp,
                                    color = SoftTextGray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = weatherTerminal,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Quick Command Console Row
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.core_telemetry),
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = stringResource(R.string.system_ok),
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Tasks Metric Card
            item {
                MetricCard(
                    title = stringResource(R.string.pending_tasks),
                    value = pendingTasksCount.toString(),
                    subText = stringResource(R.string.tasks_awaiting_execution),
                    color = NeonCyan,
                    icon = Icons.Default.CheckCircle
                ) { onNavigate("tasks") }
            }

            // Reminders Metric Card
            item {
                MetricCard(
                    title = stringResource(R.string.smart_reminders),
                    value = activeRemindersCount.toString(),
                    subText = stringResource(R.string.upcoming_schedules),
                    color = NeonPink,
                    icon = Icons.Default.NotificationsActive
                ) { onNavigate("reminders") }
            }

            // Habits Metric Card
            item {
                MetricCard(
                    title = stringResource(R.string.today_habits),
                    value = "${habitsList.count { it.loggedDaysCommaSeparated.contains(todayStr) }}/${habitsList.size}",
                    subText = stringResource(R.string.keep_up_streaks),
                    color = NeonGreen,
                    icon = Icons.Default.DirectionsRun
                ) { onNavigate("habits") }
            }

            // Expense Metric Card
            item {
                val ratio = if (maxBudget > 0) totalSpends / maxBudget else 0.0
                val ratioPercent = (ratio * 100).toInt()
                MetricCard(
                    title = stringResource(R.string.monthly_spend),
                    value = "₹${totalSpends.toInt()}",
                    subText = stringResource(R.string.monthly_limit_percent, ratioPercent),
                    color = NeonPurple,
                    icon = Icons.Default.Payments
                ) { onNavigate("expenses") }
            }

            // Water Metric Card
            item {
                val goal = viewModel.prefs.getWaterGoal()
                val ratio = if (goal > 0) (waterSum.toDouble() / goal.toDouble()) else 0.0
                val percent = (ratio * 100).toInt()
                MetricCard(
                    title = stringResource(R.string.water_intake),
                    value = "${waterSum}ml",
                    subText = stringResource(R.string.water_goal_format, goal, percent),
                    color = Color(0xFF29B6F6),
                    icon = Icons.Default.LocalActivity
                ) { onNavigate("water") }
            }

            // Notes and Memories Mini-Cards
            item {
                MetricCard(
                    title = stringResource(R.string.knowledge_base),
                    value = stringResource(R.string.core_memory_entries_count, memoriesList.size),
                    subText = "${notesList.size} notes",
                    color = SoftTextGray,
                    icon = Icons.Default.Bookmark
                ) { onNavigate("notes") }
            }

            // Bills Info Card
            item(span = { GridItemSpan(2) }) {
                val unpaidBills = billsList.count { !it.paidMonthsCommaSeparated.contains(todayStr.take(7)) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundGlass)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .clickable { onNavigate("bills") }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = stringResource(R.string.bill_icon_desc),
                            tint = NeonPink,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.due_bills_subscriptions),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (unpaidBills > 0) stringResource(R.string.bills_pending_month, unpaidBills) else stringResource(R.string.all_bills_paid),
                                color = SoftTextGray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.view_detail),
                            tint = SoftTextGray
                        )
                    }
                }
            }

            // Diary Tracker
            item(span = { GridItemSpan(2) }) {
                val diaryLogged = diaryList.any { it.dateString == todayStr }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundGlass)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .clickable { onNavigate("diary") }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = stringResource(R.string.diary_logs_desc),
                            tint = NeonGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.journal_logs),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (diaryLogged) stringResource(R.string.logged_for_today) else stringResource(R.string.write_today_diary),
                                color = SoftTextGray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.view_details),
                            tint = SoftTextGray
                        )
                    }
                }
            }

            // Calendar Agenda Card
            item(span = { GridItemSpan(2) }) {
                val countTodayEvents = calendarEvents.count { it.dateString == todayStr }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundGlass)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .clickable { onNavigate("calendar") }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = stringResource(R.string.calendar_desc),
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.daily_agenda_calendar),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (countTodayEvents > 0) stringResource(R.string.events_on_schedule_today, countTodayEvents) else stringResource(R.string.agenda_clear_today),
                                color = SoftTextGray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.view_detail),
                            tint = SoftTextGray
                        )
                    }
                }
            }

            // ADVANCED RK METRIC UTILITY BANNER
            item(span = { GridItemSpan(2) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isJarvisActive) NeonCyan.copy(alpha = 0.1f) else CardBackgroundGlass)
                        .border(1.dp, if (isJarvisActive) NeonCyan else BorderColor, RoundedCornerShape(20.dp))
                        .clickable { viewModel.toggleJarvisMode() }
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isJarvisActive) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "Jarvis Mode",
                        tint = if (isJarvisActive) NeonCyan else SoftTextGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isJarvisActive) "RK ASSISTANT ACTIVE" else "ACTIVATE RK VOICE",
                        color = if (isJarvisActive) NeonCyan else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = jarvisStatus,
                        color = SoftTextGray,
                        fontSize = 12.sp
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                MetricCard(
                    title = stringResource(R.string.smart_alarms),
                    value = stringResource(R.string.repeat_ops),
                    subText = stringResource(R.string.alarms_repeat_finished),
                    color = Color(0xFFE040FB),
                    icon = Icons.Default.NotificationsActive
                ) { onNavigate("smartreminders") }
            }

            // Voice notes Menu Box
            item {
                MetricCard(
                    title = stringResource(R.string.voice_memos),
                    value = stringResource(R.string.audio_core),
                    subText = stringResource(R.string.transcribe_speech_gemini),
                    color = Color(0xFF00E5FF),
                    icon = Icons.Default.Mic
                ) { onNavigate("voicenotes") }
            }

            // Weekly review Menu Box
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundGlass)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .clickable { onNavigate("weeklyreview") }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.weekly_retrospective_desc),
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.weekly_retrospective_digest),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = stringResource(R.string.analyze_weekly_achievements),
                                color = SoftTextGray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.view_details),
                            tint = SoftTextGray
                        )
                    }
                }
            }

            // Private Vault Card
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackgroundGlass)
                        .border(1.dp, NeonPurple, RoundedCornerShape(16.dp))
                        .clickable { onNavigate("private") }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Private Vault",
                            tint = NeonPurple,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Private Space Vault",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Secure your most sensitive notes and photos.",
                                color = SoftTextGray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = SoftTextGray
                        )
                    }
                }
            }
        }
        
        // Jarvis Voice Overlay
        AnimatedVisibility(
            visible = isJarvisActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { viewModel.toggleJarvisMode() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Pulsing AI Ring
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(120.dp * scale),
                            shape = CircleShape,
                            color = NeonCyan.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {}
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = NeonCyan,
                            shadowElevation = 20.dp
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                null,
                                tint = Color.Black,
                                modifier = Modifier.padding(25.dp).size(50.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Text(
                        text = "RK VOICE ENGINE",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = jarvisStatus,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Tap anywhere to stop",
                        color = SoftTextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subText: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title.uppercase(),
                    color = SoftTextGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    color = SoftTextGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
