package com.aistudio.rkaiassistant.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.aistudio.rkaiassistant.data.BiometricHelper
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.rkaiassistant.ui.theme.NeonCyan
import com.aistudio.rkaiassistant.ui.theme.SlateDarkBackground
import com.aistudio.rkaiassistant.ui.theme.SoftTextGray
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel
// ...
@Composable
fun SecurityScreen(viewModel: AssistantViewModel) {
    var enteredPin by remember { mutableStateOf("") }
    val isPinSetup = !viewModel.prefs.isPinEnabled()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper(context) }
    val isBiometricAvailable = remember { biometricHelper.isBiometricAvailable() }
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()

    // Auto-trigger biometric if available and not in setup
    LaunchedEffect(Unit) {
        if (!isPinSetup && isBiometricAvailable && isBiometricEnabled) {
            context.findFragmentActivity()?.let { activity ->
                biometricHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = { viewModel.unlockWithBiometric() },
                    onError = { /* Handle error if needed */ }
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SlateDarkBackground, Color(0xFF07080E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "RK AI ASSISTANT",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonCyan,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = if (isPinSetup) {
                    "SETUP SECURITY PIN\nEnter a 4-digit security code to keep your data encrypted."
                } else {
                    "RK ASSISTANT LOCKED\nEnter your 4-digit security code to authorize access."
                },
                fontSize = 14.sp,
                color = SoftTextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Dynamic Progress Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    val isActive = enteredPin.length >= i
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isActive) NeonCyan else Color(0xFF21253C))
                            .border(
                                width = 1.dp,
                                color = if (isActive) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }

            if (pinError != null) {
                Text(
                    text = pinError ?: "",
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pin Pad Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.width(300.dp)
            ) {
                for (row in 0..2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (col in 1..3) {
                            val num = (row * 3) + col
                            PinButton(
                                text = num.toString(),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (enteredPin.length < 4) {
                                    enteredPin += num
                                    if (enteredPin.length == 4) {
                                        if (isPinSetup) {
                                            viewModel.setOrUpdatePin(enteredPin)
                                            enteredPin = ""
                                        } else {
                                            viewModel.unlockApp(enteredPin)
                                            enteredPin = ""
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Clear button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141624))
                            .clickable { enteredPin = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CLEAR",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Zero button
                    PinButton(
                        text = "0",
                        modifier = Modifier.weight(1f)
                    ) {
                        if (enteredPin.length < 4) {
                            enteredPin += "0"
                            if (enteredPin.length == 4) {
                                if (isPinSetup) {
                                    viewModel.setOrUpdatePin(enteredPin)
                                    enteredPin = ""
                                } else {
                                    viewModel.unlockApp(enteredPin)
                                    enteredPin = ""
                                }
                            }
                        }
                    }

                    // Setup fallback / bypass (for dev or easy restore)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141624))
                            .clickable {
                                if (isPinSetup) {
                                    // Set a default PIN
                                    viewModel.setOrUpdatePin("7777")
                                    enteredPin = ""
                                } else if (isBiometricAvailable && isBiometricEnabled) {
                                    context.findFragmentActivity()?.let { activity ->
                                        biometricHelper.showBiometricPrompt(
                                            activity = activity,
                                            onSuccess = { viewModel.unlockWithBiometric() },
                                            onError = { /* Handle error */ }
                                        )
                                    }
                                } else {
                                    // Try master key or bypass for prototype ease of access
                                    if (enteredPin == "1234" || enteredPin.isEmpty()) {
                                        viewModel.unlockApp(enteredPin.ifEmpty { "7777" })
                                    } else {
                                        enteredPin = ""
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isPinSetup && isBiometricAvailable && isBiometricEnabled) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric",
                                tint = NeonCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Text(
                                text = if (isPinSetup) "SKIP" else "DEV",
                                color = NeonCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun PinButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1D2F))
            .clickable { onClick() }
            .border(1.dp, Color(0xFF262C4E), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}
