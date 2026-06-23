package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(viewModel: AssistantViewModel) {
    val notesList by viewModel.notes.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var noteCategory by remember { mutableStateOf("Work") }

    var searchFilter by remember { mutableStateOf("") }

    val filteredNotes = notesList.filter {
        it.title.contains(searchFilter, ignoreCase = true) ||
                it.content.contains(searchFilter, ignoreCase = true)
    }.sortedByDescending { it.isPinned }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NeonCyan.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = NeonCyan)
                }
            }

            // Quick search note bar inside layout
            OutlinedTextField(
                value = searchFilter,
                onValueChange = { searchFilter = it },
                placeholder = { Text("Search notes...", color = SoftTextGray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SoftTextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Grid note layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredNotes.isEmpty()) {
                    item {
                        Text(
                            text = "No notes stored inside memory.",
                            color = SoftTextGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                items(filteredNotes) { note ->
                    NoteGridCard(
                        note = note,
                        onPinToggle = { viewModel.toggleNotePin(note) },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }

        // Add note Dialog box
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = CardBackgroundGlass,
                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                title = { Text("SAVE STRATEGIC NOTE", color = NeonCyan, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            label = { Text("Title", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = noteContent,
                            onValueChange = { noteContent = it },
                            label = { Text("Content notes", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )

                        // Category custom Selector
                        Text("Tag category", color = SoftTextGray, fontSize = 12.sp)
                        val tags = listOf("Work", "Ideas", "Personal", "Study", "Codes")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tags.forEach { t ->
                                val isSel = noteCategory == t
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) NeonCyan else Color(0xFF141624))
                                        .clickable { noteCategory = t }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(t, color = if (isSel) Color.Black else SoftTextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (noteTitle.isNotBlank()) {
                                viewModel.addNote(noteTitle, noteContent, noteCategory)
                                noteTitle = ""
                                noteContent = ""
                                showDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("SAVE NOTE", color = Color.Black, fontWeight = FontWeight.Bold)
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
fun NoteGridCard(
    note: QuickNote,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundGlass)
            .border(
                width = 1.dp,
                color = if (note.isPinned) NeonCyan.copy(alpha = 0.5f) else BorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF141624))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = note.tag, color = SoftTextGray, fontSize = 9.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onPinToggle, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Pin Note",
                            tint = if (note.isPinned) NeonCyan else SoftTextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = SoftTextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = note.title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = note.content,
                color = SoftTextGray,
                fontSize = 13.sp,
                maxLines = 4,
                lineHeight = 18.sp
            )
        }
    }
}
