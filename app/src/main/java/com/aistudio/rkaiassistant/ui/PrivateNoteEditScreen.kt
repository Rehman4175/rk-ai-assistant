package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@Composable
fun PrivateNoteEditScreen(viewModel: AssistantViewModel) {
    val editingItem by viewModel.editingPrivateItem.collectAsState()
    
    var title by remember { mutableStateOf(editingItem?.title ?: "") }
    var content by remember { mutableStateOf(editingItem?.content ?: "") }
    var category by remember { mutableStateOf(editingItem?.category ?: "Personal") }
    var photoPath by remember { mutableStateOf(editingItem?.photoPath ?: "") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { photoPath = it.toString() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                viewModel.navigateBack()
                viewModel.editingPrivateItem.value = null
            }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            
            Row {
                IconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Image, "Add Image", tint = NeonCyan)
                }
                IconButton(onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        if (editingItem == null) {
                            viewModel.addPrivateSpaceItem(title, content, category, photoPath)
                        } else {
                            viewModel.updatePrivateSpaceItem(editingItem!!.copy(
                                title = title, 
                                content = content, 
                                category = category, 
                                photoPath = photoPath
                            ))
                        }
                    }
                    viewModel.navigateBack()
                    viewModel.editingPrivateItem.value = null
                }) {
                    Icon(Icons.Default.Check, "Save", tint = NeonPurple)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Category Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonPurple.copy(alpha = 0.1f))
                    .clickable { /* Maybe a category picker dialog? */ }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = category.uppercase(), color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Title Field
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(NeonPurple),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text("Title", color = SoftTextGray, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content Field
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(
                    color = SoftTextGray,
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                ),
                cursorBrush = SolidColor(NeonPurple),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                decorationBox = { innerTextField ->
                    if (content.isEmpty()) {
                        Text("Note", color = SoftTextGray.copy(alpha = 0.5f), fontSize = 18.sp)
                    }
                    innerTextField()
                }
            )

            if (photoPath.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box {
                    Image(
                        painter = rememberAsyncImagePainter(photoPath),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                    IconButton(
                        onClick = { photoPath = "" },
                        modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Remove", tint = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
