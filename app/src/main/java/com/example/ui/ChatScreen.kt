package com.aistudio.rkaiassistant.ui

import com.aistudio.rkaiassistant.R
import com.aistudio.rkaiassistant.ui.theme.*
import com.aistudio.rkaiassistant.data.*
import com.aistudio.rkaiassistant.viewmodel.AssistantViewModel
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: AssistantViewModel) {
    var promptInput by remember { mutableStateOf("") }
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.aiIsGenerating.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.textToSpeechEnabled.collectAsStateWithLifecycle()

    val ocrText by viewModel.ocrText.collectAsStateWithLifecycle()
    val ocrLoading by viewModel.ocrLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                promptInput = spokenText
            }
        }
    }

    // Image Picker Launcher for OCR Extraction
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val base64 = bitmapToBase64(bitmap)
                viewModel.extractTextFromImage(base64)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.ocr_image_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        // Top Toolbar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.chat_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    ),
                    fontFamily = FontFamily.Monospace
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SlateDarkBackground,
                titleContentColor = Color.White
            ),
            actions = {
                IconButton(onClick = { viewModel.clearChatHistory() }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = SoftTextGray
                    )
                }
                IconButton(onClick = { viewModel.toggleTextToSpeech() }) {
                    Icon(
                        imageVector = if (ttsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = stringResource(R.string.chat_voice_toggle_desc),
                        tint = if (ttsEnabled) NeonCyan else SoftTextGray
                    )
                }
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = stringResource(R.string.chat_ocr_trigger_desc),
                        tint = NeonCyan
                    )
                }
            }
        )

        // OCR Result Banner
        if (ocrLoading || ocrText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF152A38))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ocr_extraction_panel),
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(
                            onClick = { viewModel.ocrText.value = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (ocrLoading) {
                        CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
                    } else ocrText?.let { text ->
                        Text(text = text, color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(text))
                                    Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Text(stringResource(R.string.btn_copy_text), color = Color.Black, fontSize = 10.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.addNote(context.getString(R.string.ocr_scanned_text_label), text, context.getString(R.string.ocr_label))
                                    viewModel.ocrText.value = null
                                    Toast.makeText(context, context.getString(R.string.saved_as_note_success), Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                            ) {
                                Text(stringResource(R.string.btn_save_as_note), color = Color.Black, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Conversation List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = false
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = stringResource(R.string.ai_symbol_desc),
                                tint = NeonCyan,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.system_initialized),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = stringResource(R.string.chat_initial_hint),
                                color = SoftTextGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }

            items(chatMessages) { message ->
                ChatBubble(message, onCopy = {
                    clipboardManager.setText(AnnotatedString(message.text))
                })
            }

            if (isGenerating) {
                item {
                    Text(
                        text = stringResource(R.string.chat_generating_response),
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // Bottom Input Toolbar
        Surface(
            color = CardBackgroundGlass,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic button
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.chat_speak_command))
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.voice_recognizer_error), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NeonCyan.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = stringResource(R.string.chat_trigger_speech_desc),
                        tint = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input
                TextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text(stringResource(R.string.chat_input_placeholder), color = SoftTextGray, fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1C1E32))
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send Button
                IconButton(
                    onClick = {
                        if (promptInput.isNotBlank()) {
                            viewModel.sendAssistancePrompt(promptInput)
                            promptInput = ""
                        }
                    },
                    enabled = promptInput.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (promptInput.isNotBlank()) NeonCyan else Color(0xFF23253B),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = stringResource(R.string.chat_send_button_desc),
                        tint = if (promptInput.isNotBlank()) Color.Black else SoftTextGray
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, onCopy: () -> Unit) {
    val isUser = message.sender == "User"
    val bubbleColor = if (isUser) Color(0xFF1E3A5F) else Color(0xFF1B1D2F)
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .border(
                    width = 1.dp,
                    color = if (isUser) NeonCyan.copy(alpha = 0.3f) else BorderColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                Text(
                    text = if (isUser) stringResource(R.string.sender_user) else stringResource(R.string.sender_ai),
                    fontSize = 10.sp,
                    color = if (isUser) NeonCyan else NeonPink,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Formatted markdown styling view
                val textValue = message.text
                if (textValue.contains("```")) {
                    val parts = textValue.split("```")
                    parts.forEachIndexed { index, part ->
                        if (index % 2 == 1) { // Code Snippet block
                            CodeBlock(part.trim(), onCopy = onCopy)
                        } else {
                            Text(text = part, color = Color.White, fontSize = 14.sp)
                        }
                    }
                } else {
                    Text(text = textValue, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun CodeBlock(code: String, onCopy: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.code_snippet_label),
                    color = NeonPink,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_code_desc),
                    tint = SoftTextGray,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onCopy() }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = code,
                color = NeonCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// Inline Base64 image compression helper
fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}
