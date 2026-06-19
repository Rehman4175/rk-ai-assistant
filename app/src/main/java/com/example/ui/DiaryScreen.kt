package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DiaryEntry
import com.example.viewmodel.AssistantViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(viewModel: AssistantViewModel) {
    val diaryEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var entryText by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("Neutral") }

    val todayStr = viewModel.getTodayDateString()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERSONAL DIARY",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonGreen
                )

                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NeonGreen.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = NeonGreen)
                }
            }

            // Diary entries listing
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (diaryEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Unfold your thoughts and document daily logs.", color = SoftTextGray, fontSize = 14.sp)
                        }
                    }
                }

                items(diaryEntries) { entry ->
                    DiaryCard(entry = entry, onDelete = { viewModel.deleteDiaryEntry(entry) })
                }
            }
        }

        // Add/Update Diary Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = CardBackgroundGlass,
                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                title = { Text("DOCUMENT JOURNAL LOG", color = NeonGreen, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Mood selector row
                        Text("Mood energy selection", color = SoftTextGray, fontSize = 12.sp)
                        val moodList = listOf("Happy", "Sad", "Energetic", "Peaceful", "Neutral")
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            moodList.take(3).forEach { m ->
                                val isSel = selectedMood == m
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) NeonGreen else Color(0xFF141624))
                                        .clickable { selectedMood = m }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(m, color = if (isSel) Color.Black else SoftTextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            moodList.takeLast(2).forEach { m ->
                                val isSel = selectedMood == m
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) NeonGreen else Color(0xFF141624))
                                        .clickable { selectedMood = m }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(m, color = if (isSel) Color.Black else SoftTextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = entryText,
                            onValueChange = { entryText = it },
                            placeholder = { Text("What did you accomplish today? Reflect on your goals, mind state...", color = SoftTextGray, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (entryText.isNotBlank()) {
                                viewModel.addOrUpdateDiaryEntry(todayStr, entryText, selectedMood, "")
                                entryText = ""
                                showDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text("DOCUMENT", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("CANCEL", color = SoftTextGray)
                    }
                }
            )
        }
    }
}

@Composable
fun DiaryCard(entry: DiaryEntry, onDelete: () -> Unit) {
    val moodEmoji = when (entry.mood) {
        "Happy" -> "😊 Happy"
        "Sad" -> "😢 Sad"
        "Energetic" -> "⚡ Energetic"
        "Peaceful" -> "🧘 Peaceful"
        else -> "😐 Neutral"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = entry.dateString, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = moodEmoji, color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = SoftTextGray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = entry.text,
                color = SoftTextGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
