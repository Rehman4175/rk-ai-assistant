package com.example.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import com.example.viewmodel.AssistantViewModel
import com.example.ui.theme.*
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AssistantViewModel) {
    val context = LocalContext.current

    var waterGoalInput by remember { mutableStateOf(viewModel.prefs.getWaterGoal().toString()) }
    var budgetInput by remember { mutableStateOf(viewModel.prefs.getExpenseBudget().toString()) }

    var scriptUrlInput by remember { mutableStateOf(viewModel.prefs.getGoogleScriptUrl()) }
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.lastSyncStatus.collectAsState()

    var notificationTuneInput by remember { mutableStateOf(viewModel.prefs.getNotificationTune()) }

    // Sound Picker Launcher
    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val uriString = uri.toString()
            notificationTuneInput = uriString
            viewModel.prefs.saveNotificationTune(uriString)
            Toast.makeText(context, "Notification tune updated!", Toast.LENGTH_SHORT).show()
        }
    }

    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    val isPinEnabled by viewModel.isLocked.collectAsState()
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
                            Toast.makeText(context, "Vault PIN set successfully!", Toast.LENGTH_SHORT).show()
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

                    // Custom Notification Tune
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { soundPickerLauncher.launch("audio/*") },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Custom Notification Tune", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = if (notificationTuneInput.isBlank()) "Default system sound" else "Custom sound selected",
                                    color = SoftTextGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = SoftTextGray)
                    }
                }
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
                        text = "APP SECURITY VAULT",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Enable/Disable Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isPinEnabled) {
                                    viewModel.removePin()
                                    Toast.makeText(context, "Vault lock disabled.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showPinDialog = true
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPinEnabled) Icons.Default.Lock else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isPinEnabled) NeonCyan else SoftTextGray
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (isPinEnabled) "Disable Vault Lock" else "Enable Vault Lock",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isPinEnabled) "Turn off passcode protection" else "Secure your data with a 4-digit PIN",
                                    color = SoftTextGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = isPinEnabled,
                            onCheckedChange = {
                                if (isPinEnabled) {
                                    viewModel.removePin()
                                    Toast.makeText(context, "Vault lock disabled.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showPinDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                            )
                        )
                    }

                    if (isPinEnabled) {
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
                                    Text("Change Vault PIN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Update your security code", color = SoftTextGray, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = SoftTextGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BACKUP JSON RECOVERY BLOCK
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

                    OutlinedTextField(
                        value = scriptUrlInput,
                        onValueChange = {
                            scriptUrlInput = it
                            viewModel.prefs.setGoogleScriptUrl(it)
                        },
                        label = { Text("Apps Script Web App URL", color = SoftTextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://script.google.com/macros/s/.../exec", fontSize = 11.sp) }
                    )

                    Button(
                        onClick = { viewModel.syncToGoogleSheets(scriptUrlInput) },
                        enabled = !isSyncing && scriptUrlInput.isNotBlank(),
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

                    Divider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

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
