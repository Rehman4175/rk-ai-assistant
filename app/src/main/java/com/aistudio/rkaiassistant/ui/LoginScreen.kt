package com.aistudio.rkaiassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.rkaiassistant.ui.theme.*

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "RK ASSISTANT",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = NeonCyan,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Secure Cloud Backup & Sync",
                fontSize = 14.sp,
                color = SoftTextGray,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign in with Google", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onSkip) {
                Text("Skip for now (Offline mode)", color = SoftTextGray)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Backup your tasks, habits, and expenses automatically. Never lose your data again.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 12.sp,
                color = SoftTextGray.copy(alpha = 0.7f)
            )
        }
    }
}
