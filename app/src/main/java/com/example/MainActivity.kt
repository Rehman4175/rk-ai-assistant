package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SlateDarkBackground
import com.example.ui.theme.SoftTextGray
import com.example.viewmodel.AppScreen
import com.example.viewmodel.AssistantViewModel
import com.example.data.scheduleAllWorkers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize WorkManager alert and report background systems
        scheduleAllWorkers(this)

        setContent {
            MyApplicationTheme {
                val isLocked by viewModel.isLocked.collectAsState()

                if (isLocked) {
                    SecurityScreen(viewModel)
                } else {
                    MainAppContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: AssistantViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val context = LocalContext.current
    var backPressCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Custom Back Navigation Logic
    BackHandler {
        if (currentScreen != AppScreen.Dashboard) {
            viewModel.currentScreen.value = AppScreen.Dashboard
        } else {
            if (backPressCount == 0) {
                backPressCount++
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                scope.launch {
                    delay(2000)
                    backPressCount = 0
                }
            } else {
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                containerColor = Color(0xFF10121F),
                tonalElevation = 8.dp
            ) {
                // Bottom Item: Dashboard
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Dashboard,
                    onClick = { viewModel.currentScreen.value = AppScreen.Dashboard },
                    label = { Text(stringResource(R.string.nav_core), fontSize = 10.sp, color = if (currentScreen == AppScreen.Dashboard) NeonCyan else SoftTextGray) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = stringResource(R.string.nav_core_desc),
                            tint = if (currentScreen == AppScreen.Dashboard) NeonCyan else SoftTextGray
                        )
                    }
                )

                // Bottom Item: Chat console
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Chat,
                    onClick = { viewModel.currentScreen.value = AppScreen.Chat },
                    label = { Text(stringResource(R.string.nav_console), fontSize = 10.sp, color = if (currentScreen == AppScreen.Chat) NeonCyan else SoftTextGray) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = stringResource(R.string.nav_console_desc),
                            tint = if (currentScreen == AppScreen.Chat) NeonCyan else SoftTextGray
                        )
                    }
                )

                // Bottom Item: Tasks log
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Tasks,
                    onClick = { viewModel.currentScreen.value = AppScreen.Tasks },
                    label = { Text(stringResource(R.string.nav_tasks), fontSize = 10.sp, color = if (currentScreen == AppScreen.Tasks) NeonCyan else SoftTextGray) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.nav_tasks_desc),
                            tint = if (currentScreen == AppScreen.Tasks) NeonCyan else SoftTextGray
                        )
                    }
                )

                // Bottom Item: Private Space (Replaced Vault)
                NavigationBarItem(
                    selected = currentScreen == AppScreen.PrivateSpace,
                    onClick = { viewModel.currentScreen.value = AppScreen.PrivateSpace },
                    label = { Text("Private", fontSize = 10.sp, color = if (currentScreen == AppScreen.PrivateSpace) NeonCyan else SoftTextGray) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Private Space",
                            tint = if (currentScreen == AppScreen.PrivateSpace) NeonCyan else SoftTextGray
                        )
                    }
                )

                // Bottom Item: Universal Engine search & utilities
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Settings || currentScreen == AppScreen.Search,
                    onClick = { viewModel.currentScreen.value = AppScreen.Settings },
                    label = { Text(stringResource(R.string.nav_system), fontSize = 10.sp, color = if (currentScreen == AppScreen.Settings) NeonCyan else SoftTextGray) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_system_desc),
                            tint = if (currentScreen == AppScreen.Settings || currentScreen == AppScreen.Search) NeonCyan else SoftTextGray
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router
            when (currentScreen) {
                AppScreen.Dashboard -> DashboardScreen(viewModel) { screenName ->
                    try {
                        val parsed = AppScreen.valueOf(screenName.replaceFirstChar { it.uppercaseChar() })
                        viewModel.currentScreen.value = parsed
                    } catch (e: Exception) {
                        if (screenName.equals("water", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.Water
                        if (screenName.equals("notes", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.Notes
                        if (screenName.equals("expenses", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.Expenses
                        if (screenName.equals("reminders", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.Reminders
                        if (screenName.equals("habits", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.Habits
                        if (screenName.equals("bills", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.Bills
                        if (screenName.equals("diary", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.Diary
                        if (screenName.equals("calendar", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.Calendar
                        if (screenName.equals("search", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.Search
                        if (screenName.equals("smartreminders", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.SmartReminders
                        if (screenName.equals("voicenotes", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.VoiceNotes
                        if (screenName.equals("weeklyreview", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.WeeklyReview
                        if (screenName.equals("goals", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.Goals
                        if (screenName.equals("recurring", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.RecurringReminders
                        if (screenName.equals("links", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.RemindLinks
                        if (screenName.equals("private", ignoreCase = true)) viewModel.currentScreen.value = AppScreen.PrivateSpace
                    }
                }
                AppScreen.Chat -> ChatScreen(viewModel)
                AppScreen.Tasks -> TaskManagerScreen(viewModel)
                AppScreen.Reminders -> ReminderScreen(viewModel)
                AppScreen.Habits -> HabitScreen(viewModel)
                AppScreen.Water -> WaterScreen(viewModel)
                AppScreen.Expenses -> ExpenseScreen(viewModel)
                AppScreen.Bills -> BillScreen(viewModel)
                AppScreen.Calendar -> CalendarScreen(viewModel)
                AppScreen.Diary -> DiaryScreen(viewModel)
                AppScreen.Notes -> NoteScreen(viewModel)
                AppScreen.Search -> SearchScreen(viewModel)
                AppScreen.Settings -> SettingsScreen(viewModel)
                AppScreen.SmartReminders -> SmartReminderScreen(viewModel)
                AppScreen.VoiceNotes -> VoiceNoteScreen(viewModel)
                AppScreen.WeeklyReview -> WeeklyReviewScreen(viewModel)
                AppScreen.Goals -> GoalsScreen(viewModel)
                AppScreen.RecurringReminders -> RecurringRemindersScreen(viewModel)
                AppScreen.RemindLinks -> RemindLinksScreen(viewModel)
                AppScreen.PrivateSpace -> PrivateSpaceScreen(viewModel)
            }
        }
    }
}
