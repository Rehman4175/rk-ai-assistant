package com.aistudio.rkaiassistant

import android.Manifest
import android.os.Build
import android.os.Bundle
import com.aistudio.rkaiassistant.R
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.DisposableEffect
import com.aistudio.rkaiassistant.ui.*
import com.aistudio.rkaiassistant.ui.theme.MyApplicationTheme
import com.aistudio.rkaiassistant.ui.theme.NeonCyan
import com.aistudio.rkaiassistant.ui.theme.SlateDarkBackground
import com.aistudio.rkaiassistant.ui.theme.SoftTextGray
import com.aistudio.rkaiassistant.viewmodel.AppScreen
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel
import com.aistudio.rkaiassistant.data.scheduleAllWorkers
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class MainActivity : FragmentActivity() {
    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize WorkManager alert and report background systems
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                scheduleAllWorkers(this@MainActivity)
            } catch (t: Throwable) {
                android.util.Log.e("RKAI", "WorkManager Init Error", t)
            }
        }

        setContent {
            MyApplicationTheme {
                val isLocked by viewModel.isLocked.collectAsState()
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val isLoginSkipped by viewModel.isLoginSkipped.collectAsState()

                // Request Notification and Location Permissions
                val context = LocalContext.current
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions["android.permission.POST_NOTIFICATIONS"] ?: false
                    } else {
                        true
                    }
                    if (!notificationsGranted) {
                        Toast.makeText(context, "Notification permission recommended for reminders!", Toast.LENGTH_SHORT).show()
                    }
                }

                LaunchedEffect(Unit) {
                    val perms = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add("android.permission.POST_NOTIFICATIONS")
                    }
                    perms.add(Manifest.permission.RECORD_AUDIO)
                    permissionsLauncher.launch(perms.toTypedArray())
                }

                // Auto-lock on background
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) {
                            if (viewModel.prefs.isAppLockActive() && viewModel.prefs.isPinEnabled()) {
                                viewModel.isLocked.value = true
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Google Sign-In Launcher
                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                        if (account != null) {
                            viewModel.loginSuccess()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                if (isLocked) {
                    SecurityScreen(viewModel)
                } else if (!isLoggedIn && !isLoginSkipped) {
                    LoginScreen(
                        onLoginClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestIdToken("879298355170-rp7s7vngjg60vhpn6p55cfoi86c1ort2.apps.googleusercontent.com")
                                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                                .build()
                            val client = GoogleSignIn.getClient(this@MainActivity, gso)
                            googleSignInLauncher.launch(client.signInIntent)
                        },
                        onSkip = { viewModel.skipLogin() }
                    )
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
    var backPressCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Custom Back Navigation Logic
    BackHandler {
        if (!viewModel.navigateBack()) {
            if (backPressCount == 0) {
                backPressCount++
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                scope.launch {
                    delay(2.seconds)
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
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Dashboard,
                    onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                    label = { Text(stringResource(R.string.nav_core), fontSize = 10.sp, color = if (currentScreen == AppScreen.Dashboard) NeonCyan else SoftTextGray) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = stringResource(R.string.nav_core_desc),
                            tint = if (currentScreen == AppScreen.Dashboard) NeonCyan else SoftTextGray
                        )
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.Chat,
                    onClick = { viewModel.navigateTo(AppScreen.Chat) },
                    label = { Text(stringResource(R.string.nav_console), fontSize = 10.sp, color = if (currentScreen == AppScreen.Chat) NeonCyan else SoftTextGray) },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = stringResource(R.string.nav_console_desc),
                            tint = if (currentScreen == AppScreen.Chat) NeonCyan else SoftTextGray
                        )
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.Tasks,
                    onClick = { viewModel.navigateTo(AppScreen.Tasks) },
                    label = { Text(stringResource(R.string.nav_tasks), fontSize = 10.sp, color = if (currentScreen == AppScreen.Tasks) NeonCyan else SoftTextGray) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.nav_tasks_desc),
                            tint = if (currentScreen == AppScreen.Tasks) NeonCyan else SoftTextGray
                        )
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.History,
                    onClick = { viewModel.navigateTo(AppScreen.History) },
                    label = { Text("History", fontSize = 10.sp, color = if (currentScreen == AppScreen.History) NeonCyan else SoftTextGray) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "App History",
                            tint = if (currentScreen == AppScreen.History) NeonCyan else SoftTextGray
                        )
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.GoldCalculator,
                    onClick = { viewModel.navigateTo(AppScreen.GoldCalculator) },
                    label = { Text("Gold", fontSize = 10.sp, color = if (currentScreen == AppScreen.GoldCalculator) NeonCyan else SoftTextGray) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Gold Calculator",
                            tint = if (currentScreen == AppScreen.GoldCalculator) NeonCyan else SoftTextGray
                        )
                    }
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.Settings || currentScreen == AppScreen.Search,
                    onClick = { viewModel.navigateTo(AppScreen.Settings) },
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
            when (currentScreen) {
                AppScreen.Dashboard -> DashboardScreen(viewModel) { screenName ->
                    try {
                        val parsed = AppScreen.valueOf(screenName.replaceFirstChar { it.uppercaseChar() })
                        viewModel.navigateTo(parsed)
                    } catch (_: Exception) {
                        when (screenName.lowercase()) {
                            "water" -> viewModel.navigateTo(AppScreen.Water)
                            "notes" -> viewModel.navigateTo(AppScreen.Notes)
                            "expenses" -> viewModel.navigateTo(AppScreen.Expenses)
                            "reminders" -> viewModel.navigateTo(AppScreen.Reminders)
                            "habits" -> viewModel.navigateTo(AppScreen.Habits)
                            "bills" -> viewModel.navigateTo(AppScreen.Bills)
                            "diary" -> viewModel.navigateTo(AppScreen.Diary)
                            "calendar" -> viewModel.navigateTo(AppScreen.Calendar)
                            "search" -> viewModel.navigateTo(AppScreen.Search)
                            "smartreminders" -> viewModel.navigateTo(AppScreen.SmartReminders)
                            "voicenotes" -> viewModel.navigateTo(AppScreen.VoiceNotes)
                            "weeklyreview" -> viewModel.navigateTo(AppScreen.WeeklyReview)
                            "goals" -> viewModel.navigateTo(AppScreen.Goals)
                            "recurring" -> viewModel.navigateTo(AppScreen.RecurringReminders)
                            "links" -> viewModel.navigateTo(AppScreen.RemindLinks)
                            "private" -> viewModel.navigateTo(AppScreen.PrivateSpace)
                        }
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
                AppScreen.History -> HistoryScreen(viewModel)
                AppScreen.Search -> SearchScreen(viewModel)
                AppScreen.Settings -> SettingsScreen(viewModel)
                AppScreen.SmartReminders -> SmartReminderScreen(viewModel)
                AppScreen.VoiceNotes -> VoiceNoteScreen(viewModel)
                AppScreen.WeeklyReview -> WeeklyReviewScreen(viewModel)
                AppScreen.Goals -> GoalsScreen(viewModel)
                AppScreen.RecurringReminders -> RecurringRemindersScreen(viewModel)
                AppScreen.RemindLinks -> RemindLinksScreen(viewModel)
                AppScreen.PrivateSpace -> PrivateSpaceScreen(viewModel)
                AppScreen.PrivateNoteEdit -> PrivateNoteEditScreen(viewModel)
                AppScreen.GoldCalculator -> GoldCalculatorScreen(viewModel)
            }
        }
    }
}
