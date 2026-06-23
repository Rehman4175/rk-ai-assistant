package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AssistantViewModel) {
    val context = LocalContext.current

    var waterGoalInput by remember { mutableStateOf(viewModel.prefs.getWaterGoal().toString()) }
    var budgetInput by remember { mutableStateOf(viewModel.prefs.getExpenseBudget().toString()) }

    var geminiKeyInput by remember { mutableStateOf(viewModel.prefs.getGeminiApiKey()) }
    var weatherKeyInput by remember { mutableStateOf(viewModel.prefs.getWeatherApiKey()) }

    var scriptUrlInput by remember { mutableStateOf(viewModel.prefs.getGoogleScriptUrl()) }
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.lastSyncStatus.collectAsState()

    val isUrlValid = scriptUrlInput.isBlank() || 
                    (scriptUrlInput.startsWith("https://script.google.com") && 
                     scriptUrlInput.contains("/macros/s/"))
    val isSpreadsheetUrl = scriptUrlInput.contains("docs.google.com/spreadsheets")

    var notificationTuneInput by remember { mutableStateOf(viewModel.prefs.getNotificationTune()) }
    var showSoundPicker by remember { mutableStateOf(false) }

    // Sound Picker Launcher
    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Take persistable permission to access the file in background/after reboot
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}

            val uriString = uri.toString()
            notificationTuneInput = uriString
            viewModel.prefs.saveNotificationTune(uriString)
            Toast.makeText(context, "Notification tune updated!", Toast.LENGTH_SHORT).show()
        }
    }

    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    val isAppLockActive by viewModel.isAppLockActive.collectAsState()
    val isWelcomeSoundEnabled by viewModel.isWelcomeSoundEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    // Backup Export File Picker
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    viewModel.exportBackupJson(outputStream)
                    Toast.makeText(context, "Export finished successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Backup Import File Picker
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val success = viewModel.importBackupJson(inputStream)
                    if (success) {
                        Toast.makeText(context, "Data restoration finished successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Data restoration failed. Verify file format.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Model File Picker
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Model file selected! Processing...", Toast.LENGTH_SHORT).show()
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importModelFromFile(inputStream)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set Security PIN", color = Color.White) },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to secure your data.", color = SoftTextGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInput = it },
                        label = { Text("Enter 4-digit PIN", color = SoftTextGray) },
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
                        if (pinInput.length == 4) {
                            viewModel.setOrUpdatePin(pinInput)
                            showPinDialog = false
                            pinInput = ""
                            Toast.makeText(context, "Security PIN set successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter exactly 4 digits.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Set PIN", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel", color = NeonCyan)
                }
            },
            containerColor = Color(0xFF1C1E32)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "SETTINGS PANEL",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subgoals preferences edit block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackgroundGlass)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "GOAL LIMITS CONFIGURATION",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    OutlinedTextField(
                        value = waterGoalInput,
                        onValueChange = {
                            waterGoalInput = it
                            val value = it.toIntOrNull()
                            if (value != null && value > 0) {
                                viewModel.prefs.setWaterGoal(value)
                            }
                        },
                        label = { Text("Water daily Goal ml", color = SoftTextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = {
                            budgetInput = it
                            val value = it.toDoubleOrNull()
                            if (value != null && value > 0) {
                                viewModel.prefs.setExpenseBudget(value)
                            }
                        },
                        label = { Text("Expense Budget limit (₹)", color = SoftTextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(color = BorderColor, thickness = 0.5.dp)

                    Text(
                        text = "API KEYS CONFIGURATION (ENCRYPTED)",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        label = { Text("Gemini API Key", color = SoftTextGray) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            if (geminiKeyInput != viewModel.prefs.getGeminiApiKey()) {
                                IconButton(onClick = {
                                    viewModel.prefs.saveGeminiApiKey(geminiKeyInput)
                                    GeminiService.initialize(geminiKeyInput)
                                    Toast.makeText(context, "Gemini Key Updated!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Upload, null, tint = NeonCyan)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = weatherKeyInput,
                        onValueChange = { weatherKeyInput = it },
                        label = { Text("OpenWeatherMap API Key", color = SoftTextGray) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            if (weatherKeyInput != viewModel.prefs.getWeatherApiKey()) {
                                IconButton(onClick = {
                                    viewModel.prefs.saveWeatherApiKey(weatherKeyInput)
                                    Toast.makeText(context, "Weather Key Updated!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Upload, null, tint = NeonCyan)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider(color = BorderColor, thickness = 0.5.dp)

                    // Custom Notification Tune
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSoundPicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Notification Tune Selection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = if (notificationTuneInput.isBlank()) "Default system sound" else "Selected: ${notificationTuneInput.takeLast(20)}...",
                                    color = SoftTextGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = SoftTextGray)
                    }
                }
            }

            if (showSoundPicker) {
                SoundSelectionDialog(
                    onDismiss = { showSoundPicker = false },
                    onSelect = { uri ->
                        notificationTuneInput = uri.toString()
                        viewModel.prefs.saveNotificationTune(uri.toString())
                        showSoundPicker = false
                        Toast.makeText(context, "Notification tune updated!", Toast.LENGTH_SHORT).show()
                    },
                    currentUri = notificationTuneInput,
                    onCustomClick = { soundPickerLauncher.launch(arrayOf("audio/*")) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Encryption and Passcode Reset Action List block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackgroundGlass)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "APP SECURITY & LOCK",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Fingerprint/Biometric Status
                    val biometricHelper = remember { BiometricHelper(context) }
                    val isBioAvailable = remember { biometricHelper.isBiometricAvailable() }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.Fingerprint, null, tint = if (isBioAvailable) NeonCyan else SoftTextGray)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Biometric Support", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(if (isBioAvailable) "Ready to use" else "Not supported/setup", color = SoftTextGray, fontSize = 11.sp)
                            }
                        }
                        if (isBioAvailable) {
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { viewModel.toggleBiometricSetting(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonCyan,
                                    checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Divider(color = BorderColor, thickness = 0.5.dp)

                    // Welcome Sound Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, null, tint = if (isWelcomeSoundEnabled) NeonCyan else SoftTextGray)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Welcome Greeting", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Speak 'Welcome back RK' on unlock", color = SoftTextGray, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = isWelcomeSoundEnabled,
                            onCheckedChange = { viewModel.toggleWelcomeSound(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Divider(color = BorderColor, thickness = 0.5.dp)

                    // Enable/Disable Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isAppLockActive) {
                                    viewModel.toggleAppLock(false)
                                    Toast.makeText(context, "App lock disabled.", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (viewModel.prefs.isPinEnabled()) {
                                        viewModel.toggleAppLock(true)
                                        Toast.makeText(context, "App lock enabled.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showPinDialog = true
                                    }
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAppLockActive) Icons.Default.Lock else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isAppLockActive) NeonCyan else SoftTextGray
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (isAppLockActive) "Disable App Lock" else "Enable App Lock",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isAppLockActive) "Passcode protection is active" else "Secure your data with a 4-digit PIN",
                                    color = SoftTextGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = isAppLockActive,
                            onCheckedChange = {
                                if (isAppLockActive) {
                                    viewModel.toggleAppLock(false)
                                    Toast.makeText(context, "App lock disabled.", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (viewModel.prefs.isPinEnabled()) {
                                        viewModel.toggleAppLock(true)
                                        Toast.makeText(context, "App lock enabled.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showPinDialog = true
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                            )
                        )
                    }

                    if (viewModel.prefs.isPinEnabled()) {
                        Divider(color = BorderColor, thickness = 0.5.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPinDialog = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BugReport, null, tint = SoftTextGray) // Using an icon for 'Change'
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Change Security PIN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Update your security code", color = SoftTextGray, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = SoftTextGray)
                        }

                        // Option to clear PIN entirely
                        TextButton(
                            onClick = { 
                                viewModel.removePin()
                                Toast.makeText(context, "PIN removed and lock disabled.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Remove PIN Entirely", color = Color.Red.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECURITY & ACCESS
            Text(text = "SYSTEM & PERMISSIONS", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackgroundGlass)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val packageName = context.packageName
                            val intent = Intent()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                intent.action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                intent.data = android.net.Uri.parse("package:$packageName")
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error opening settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141624)),
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.BatteryFull, null, tint = NeonGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ALLOW BACKGROUND RUN (FIX POPUP)", color = Color.White)
                    }
                    
                    Text(
                        text = "Agar app background me band ho jati hai to ise 'Allow' karein. Isse baar-baar wala popup bhi nahi aayega.",
                        color = SoftTextGray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CLOUD SYNC TERMINAL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackgroundGlass)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "CLOUD SYNC TERMINAL (GOOGLE SHEETS)",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Offline LLM Status
                    val isLocalAiAvailable by viewModel.isLocalAiAvailable.collectAsState()
                    val isImportingModel by viewModel.isImportingModel.collectAsState()
                    val downloadProgress by viewModel.modelDownloadProgress.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = if (isLocalAiAvailable) NeonGreen else if (downloadProgress != null || isImportingModel) NeonCyan else SoftTextGray
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Offline LLM Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                if (isLocalAiAvailable) "Model Loaded (Qwen 2.5)" 
                                else if (isImportingModel) "Importing Model File..."
                                else if (downloadProgress != null) "Downloading Model... ${(downloadProgress!! * 100).toInt()}%" 
                                else "Model Missing: Required for Offline AI",
                                color = if (isLocalAiAvailable) NeonGreen else if (downloadProgress != null || isImportingModel) NeonCyan else SoftTextGray,
                                fontSize = 11.sp
                            )
                        }
                        
                        if (!isLocalAiAvailable && downloadProgress == null && !isImportingModel) {
                            Row {
                                Button(
                                    onClick = { viewModel.downloadOfflineModel() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("DOWNLOAD", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { modelPickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141624)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                                ) {
                                    Text("SELECT FILE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (!isLocalAiAvailable && !isImportingModel) {
                             // If model exists but is corrupted (hence isLocalAiAvailable is false)
                             Button(
                                 onClick = { viewModel.deleteModel() },
                                 colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                 modifier = Modifier.height(32.dp)
                             ) {
                                 Text("DELETE & RETRY", color = Color.White, fontSize = 10.sp)
                             }
                        }
                    }
                    
                    if (downloadProgress != null) {
                        LinearProgressIndicator(
                            progress = { downloadProgress!! },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = NeonCyan,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }

                    Divider(color = BorderColor, thickness = 0.5.dp)

                    OutlinedTextField(
                        value = scriptUrlInput,
                        onValueChange = {
                            scriptUrlInput = it
                            viewModel.prefs.setGoogleScriptUrl(it)
                        },
                        label = { Text("Apps Script Web App URL", color = SoftTextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isUrlValid) NeonCyan else Color.Red,
                            unfocusedBorderColor = if (isUrlValid) BorderColor else Color.Red.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://script.google.com/macros/s/.../exec", fontSize = 11.sp) },
                        isError = !isUrlValid || isSpreadsheetUrl
                    )

                    if (isSpreadsheetUrl) {
                        Text(
                            text = "⚠️ Ye Spreadsheet URL hai! Aapko 'Apps Script' se 'Deploy as Web App' karke URL nikalna hoga.",
                            color = Color.Yellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (!isUrlValid && scriptUrlInput.isNotBlank()) {
                        Text(
                            text = "❌ Invalid URL. Google Apps Script Web App URL (script.google.com) chahiye.",
                            color = Color.Red,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "LAST SYNC STATUS:",
                        color = SoftTextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = viewModel.lastSyncStatus.collectAsState().value ?: "Never synced",
                        color = if (viewModel.lastSyncStatus.collectAsState().value?.contains("✅") == true) NeonGreen else if (viewModel.lastSyncStatus.collectAsState().value?.contains("❌") == true) NeonPink else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = { viewModel.syncToGoogleSheets(scriptUrlInput) },
                        enabled = !isSyncing && isUrlValid && scriptUrlInput.isNotBlank() && !isSpreadsheetUrl,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Upload, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SYNC TO GOOGLE SHEETS", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    var showSyncHelp by remember { mutableStateOf(false) }
                    TextButton(onClick = { showSyncHelp = true }) {
                        Text("❓ How to setup Google Sheets Sync?", color = NeonCyan, fontSize = 12.sp)
                    }

                    if (showSyncHelp) {
                        AlertDialog(
                            onDismissRequest = { showSyncHelp = false },
                            title = { Text("How to Setup Sync", color = NeonCyan) },
                            text = {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    Text(
                                        text = "1. Google Sheet kholen.\n" +
                                                "2. 'Extensions' -> 'Apps Script' pe jayen.\n" +
                                                "3. Neeche wala code copy karke paste karein:\n\n" +
                                                "function doPost(e) {\n" +
                                                "  var data = JSON.parse(e.postData.contents);\n" +
                                                "  var ss = SpreadsheetApp.getActiveSpreadsheet();\n\n" +
                                                "  for (var key in data) {\n" +
                                                "    var sheet = ss.getSheetByName(key) || ss.insertSheet(key);\n" +
                                                "    sheet.clear(); // Fresh sync\n" +
                                                "    var rows = data[key];\n" +
                                                "    if (rows.length > 0) {\n" +
                                                "      var headers = Object.keys(rows[0]);\n" +
                                                "      sheet.appendRow(headers);\n" +
                                                "      \n" +
                                                "      // Formatting headers\n" +
                                                "      sheet.getRange(1, 1, 1, headers.length).setBackground('#00E5FF').setFontWeight('bold');\n\n" +
                                                "      var dataValues = rows.map(function(r) {\n" +
                                                "        return headers.map(function(h) { return r[h]; });\n" +
                                                "      });\n" +
                                                "      sheet.getRange(2, 1, dataValues.length, headers.length).setValues(dataValues);\n" +
                                                "    }\n" +
                                                "  }\n" +
                                                "  return ContentService.createTextOutput('Success');\n" +
                                                "}\n\n" +
                                                "4. 'Deploy' -> 'New Deployment' -> 'Web App'.\n" +
                                                "5. 'Who has access' ko 'Anyone' karein.\n" +
                                                "6. Deployment URL yahan paste karein.",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showSyncHelp = false }) { Text("OK") }
                            },
                            containerColor = Color(0xFF1C1E32)
                        )
                    }

                    if (syncStatus != null) {
                        Text(
                            text = syncStatus ?: "",
                            color = if (syncStatus?.contains("✅") == true) NeonGreen else Color.Yellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // HELP & COMMANDS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackgroundGlass)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "HELP & COMMANDS",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    var showCommandList by remember { mutableStateOf(false) }
                    
                    Button(
                        onClick = { showCommandList = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141624)),
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Settings, null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VIEW COMMAND LIST", color = Color.White)
                    }

                    if (showCommandList) {
                        CommandListDialog(onDismiss = { showCommandList = false })
                    }

                    Divider(color = BorderColor, thickness = 0.5.dp)

                    Text(
                        text = "DATABASE JSON BACKUP TERMINAL",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Button(
                        onClick = { exportLauncher.launch("RK_AI_Assistant_backup_${System.currentTimeMillis()}.json") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXPORT ALL DATA TO JSON", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { importLauncher.launch("application/json") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1E32)),
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Download, null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("IMPORT RESTORE DATA", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CommandListDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("RK COMMAND CONSOLE", color = NeonCyan, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommandListItem("task [text]", "Naya task add karne ke liye. (Ex: task gym jana hai)")
                CommandListItem("done [id]", "Task complete karne ke liye ID ke saath.")
                CommandListItem("reminder [time] [text]", "Timer based reminder. (Ex: remind 10 min chai)")
                CommandListItem("expense [amt] [item]", "Kharcha add karein. (Ex: expense 50 juice)")
                CommandListItem("water [ml]", "Paani peena log karein. (Ex: water 250)")
                CommandListItem("diary [text]", "Aaj ki diary entry likhein.")
                CommandListItem("habit [name]", "Nayi habit track karna shuru karein.")
                CommandListItem("hdone [name]", "Habit complete mark karein.")
                CommandListItem("sync now", "Data Google Sheets par sync karein.")
                CommandListItem("help", "Saari commands dekhne ke liye.")
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tip: Aap seedha Gemini se bhi pooch sakte hain: 'Aaj kitna kharcha hua?'",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                Text("SAMAJH GAYA", color = Color.Black)
            }
        },
        containerColor = Color(0xFF1C1E32)
    )
}

@Composable
fun CommandListItem(cmd: String, desc: String) {
    Column {
        Text(cmd, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text(desc, color = SoftTextGray, fontSize = 12.sp)
        Divider(color = BorderColor.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun SoundSelectionDialog(
    onDismiss: () -> Unit,
    onSelect: (Uri) -> Unit,
    currentUri: String,
    onCustomClick: () -> Unit
) {
    val context = LocalContext.current
    val ringtoneManager = RingtoneManager(context).apply {
        setType(RingtoneManager.TYPE_NOTIFICATION)
    }
    
    val cursor = ringtoneManager.cursor
    val ringtones = remember {
        val list = mutableListOf<Pair<String, Uri>>()
        if (cursor != null && cursor.moveToFirst()) {
            var count = 0
            do {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = ringtoneManager.getRingtoneUri(cursor.position)
                list.add(title to uri)
                count++
            } while (cursor.moveToNext() && count < 25)
        }
        list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SELECT NOTIFICATION TUNE", color = NeonCyan, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                Button(
                    onClick = onCustomClick,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141624))
                ) {
                    Icon(Icons.Default.Upload, null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("UPLOAD CUSTOM MP3/FILE", color = Color.White)
                }
                
                Divider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(ringtones.size) { index ->
                        val (title, uri) = ringtones[index]
                        val isSelected = currentUri == uri.toString()
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { 
                                    onSelect(uri)
                                    // Play sound preview
                                    try {
                                        val r = RingtoneManager.getRingtone(context, uri)
                                        r.play()
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            if (r.isPlaying) r.stop()
                                        }, 2000)
                                    } catch (e: Exception) {}
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.MusicNote else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) NeonCyan else SoftTextGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = title, color = if (isSelected) NeonCyan else Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = SoftTextGray)
            }
        },
        containerColor = Color(0xFF1C1E32)
    )
}
