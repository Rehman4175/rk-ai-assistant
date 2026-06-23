package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel
import com.aistudio.rkaiassistant.viewmodel.AppScreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateSpaceScreen(viewModel: AssistantViewModel) {
    val itemsList by viewModel.privateSpaceItems.collectAsStateWithLifecycle()
    val isLocked by viewModel.isPrivateSpaceLocked.collectAsStateWithLifecycle()
    val hasPassword by viewModel.hasPrivateSpacePassword.collectAsStateWithLifecycle()
    val error by viewModel.privateSpaceError.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PrivateSpaceItem?>(null) }
    
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Personal") }
    var photoPath by remember { mutableStateOf("") }

    var passwordInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { photoPath = it.toString() }
    }

    if (isLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateDarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = NeonPurple.copy(alpha = 0.1f)
                ) {
                    Icon(
                        Icons.Default.Lock, 
                        null, 
                        tint = NeonPurple, 
                        modifier = Modifier.padding(20.dp).size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (hasPassword) "PRIVATE SPACE LOCKED" else "SET PRIVATE PASSWORD",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hasPassword) "Enter password to access your encrypted vault." else "Create a password to secure your sensitive data.",
                    color = SoftTextGray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password", color = SoftTextGray) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonPurple
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(text = error!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (hasPassword) {
                            viewModel.unlockPrivateSpace(passwordInput)
                        } else {
                            viewModel.setPrivateSpacePassword(passwordInput)
                        }
                        passwordInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(if (hasPassword) "UNLOCK VAULT" else "INITIALIZE VAULT", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = SlateDarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    viewModel.editingPrivateItem.value = null
                    viewModel.navigateTo(AppScreen.PrivateNoteEdit)
                },
                containerColor = NeonPurple,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, "Add Note")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.lockPrivateSpace() }) {
                        Icon(Icons.Default.LockOpen, "Lock", tint = NeonPurple)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Private Vault",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // Staggered Grid for Note-like layout
            val filteredList = itemsList.filter { !it.isDeleted }
            val pinnedItems = filteredList.filter { it.isPinned }
            val otherItems = filteredList.filter { !it.isPinned }

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                if (pinnedItems.isNotEmpty()) {
                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                        Text("PINNED", color = SoftTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    }
                    items(pinnedItems) { item ->
                        PrivateItemCard(item, viewModel) {
                            viewModel.editingPrivateItem.value = item
                            viewModel.navigateTo(AppScreen.PrivateNoteEdit)
                        }
                    }
                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                        Text("OTHERS", color = SoftTextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    }
                }

                items(otherItems) { item ->
                    PrivateItemCard(item, viewModel) {
                        viewModel.editingPrivateItem.value = item
                        viewModel.navigateTo(AppScreen.PrivateNoteEdit)
                    }
                }

                if (itemsList.isEmpty()) {
                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Notes, null, tint = SoftTextGray.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                                Text("Your secret notes appear here", color = SoftTextGray, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = Color(0xFF1A1C2E),
                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                title = { 
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (editingItem == null) "NEW SECRET" else "EDIT SECRET", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (editingItem != null) {
                            IconButton(onClick = { 
                                viewModel.deletePrivateSpaceItem(editingItem!!)
                                showDialog = false 
                            }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Title", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = { Text("Write something private...", color = SoftTextGray) },
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                                Icon(Icons.Default.Image, "Add Photo", tint = NeonCyan)
                            }
                            if (photoPath.isNotBlank()) {
                                Text("Photo attached", color = NeonCyan, fontSize = 12.sp)
                                IconButton(onClick = { photoPath = "" }) {
                                    Icon(Icons.Default.Close, "Remove Photo", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (photoPath.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(photoPath),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category", color = SoftTextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
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
                            if (title.isNotBlank() || content.isNotBlank()) {
                                if (editingItem == null) {
                                    viewModel.addPrivateSpaceItem(title, content, category, photoPath)
                                } else {
                                    // Normally we'd update, but let's just delete and re-add for simplicity or add an update function
                                    viewModel.deletePrivateSpaceItem(editingItem!!)
                                    viewModel.addPrivateSpaceItem(title, content, category, photoPath)
                                }
                                showDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Text("SAVE ENCRYPTED", color = Color.Black, fontWeight = FontWeight.Bold)
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
fun PrivateItemCard(item: PrivateSpaceItem, viewModel: AssistantViewModel, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, if (item.isPinned) NeonPurple else BorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            if (item.photoPath.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(item.photoPath),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .padding(bottom = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }
            
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (item.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                    contentDescription = "Pin",
                    tint = if (item.isPinned) NeonPurple else SoftTextGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp).clickable { viewModel.togglePrivateSpaceItemPin(item) }
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = item.content,
                fontSize = 13.sp,
                color = SoftTextGray,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonPurple.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(text = item.category, fontSize = 9.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
