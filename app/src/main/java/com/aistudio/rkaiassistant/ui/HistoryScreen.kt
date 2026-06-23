package com.aistudio.rkaiassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel
import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: AssistantViewModel) {
    val historyItems by viewModel.allActivities.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("ACTIVITY HISTORY", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text("Full audit log (including deleted items):", color = SoftTextGray, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(10.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(historyItems) { item ->
                    HistoryItemCard(item)
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: AssistantViewModel.ActivityItem) {
    val dateTimeStr = if (item.timestamp > 0) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
    } else {
        item.date
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, if (item.isDeleted) Color.Red.copy(alpha = 0.3f) else BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "[${item.id}] ${item.type}: ${item.title}",
                    color = if (item.isDeleted) SoftTextGray else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        textDecoration = if (item.isDeleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                )
                if (dateTimeStr.isNotBlank()) {
                    Text(text = "Activity: $dateTimeStr", color = SoftTextGray, fontSize = 11.sp)
                }
            }
            
            if (item.isDeleted) {
                Text("DELETED", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
            } else if (item.isDone) {
                Text("DONE", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
            } else {
                Text("ACTIVE", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
