package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VoiceNote
import com.example.ui.theme.*
import com.example.viewmodel.AssistantViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNoteScreen(viewModel: AssistantViewModel) {
    val context = LocalContext.current
    val voiceNotesList by viewModel.voiceNotes.collectAsStateWithLifecycle()

    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }
    var currentRecordFile by remember { mutableStateOf<File?>(null) }
    var recordStartTime by remember { mutableStateOf(0L) }

    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentlyPlayingId by remember { mutableStateOf<Int?>(null) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }

    val scope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    // Request permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required to record audio notes.", Toast.LENGTH_LONG).show()
        }
    }

    // Recording duration thread/effect
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0L
            while (isRecording) {
                delay(1000)
                recordingDuration += 1
            }
        }
    }

    // Cleanup player on dispose
    DisposableEffect(Unit) {
        onDispose {
            activePlayer?.release()
            mediaRecorder?.release()
        }
    }

    fun startRecording() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        try {
            val audioDirectory = File(context.cacheDir, "audio_notes")
            if (!audioDirectory.exists()) {
                audioDirectory.mkdirs()
            }
            val file = File(audioDirectory, "recording_${System.currentTimeMillis()}.mp4")
            currentRecordFile = file
            recordStartTime = System.currentTimeMillis()

            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = r
            isRecording = true
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start recording: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        isRecording = false

        val file = currentRecordFile
        if (file != null && file.exists()) {
            val durationMs = System.currentTimeMillis() - recordStartTime
            val durationSecs = durationMs / 1000
            if (durationSecs > 0) {
                viewModel.addVoiceNote(file.absolutePath, durationSecs)
                Toast.makeText(context, "Voice Note saved!", Toast.LENGTH_SHORT).show()
            } else {
                file.delete()
                Toast.makeText(context, "Recording too short", Toast.LENGTH_SHORT).show()
            }
        }
        currentRecordFile = null
    }

    fun togglePlayPause(voiceNote: VoiceNote) {
        if (currentlyPlayingId == voiceNote.id) {
            // Pause/Stop
            activePlayer?.stop()
            activePlayer?.release()
            activePlayer = null
            currentlyPlayingId = null
        } else {
            // Stop old one
            activePlayer?.stop()
            activePlayer?.release()
            activePlayer = null
            currentlyPlayingId = null

            val file = File(voiceNote.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "Error: Audio file not found", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val player = MediaPlayer().apply {
                    setDataSource(voiceNote.filePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        currentlyPlayingId = null
                        activePlayer = null
                    }
                }
                activePlayer = player
                currentlyPlayingId = voiceNote.id
            } catch (e: Exception) {
                Toast.makeText(context, "Playback error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VOICE ARCHIVES",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan
                    )
                    Text(
                        text = "Record speech memos and transcribe with AI",
                        fontSize = 11.sp,
                        color = SoftTextGray
                    )
                }
            }

            // Central Recording Dashboard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF10121F))
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isRecording) {
                        Text(
                            text = "RECORDING NOW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPink,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Pulsing Record Wave
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NeonPink)
                                .clickable { stopRecording() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop micro",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "AUDIO CONSOLE IDLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Tap button to begin memo",
                            color = SoftTextGray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                                .clickable { startRecording() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Active micro",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Records List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (voiceNotesList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No audio logs compiled securely.",
                                color = SoftTextGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                items(voiceNotesList) { note ->
                    VoiceNoteCard(
                        note = note,
                        isPlaying = currentlyPlayingId == note.id,
                        formatter = sdf,
                        onPlayPause = { togglePlayPause(note) },
                        onTranscribe = { viewModel.transcribeVoiceNoteGemini(note) },
                        onDelete = { viewModel.deleteVoiceNote(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceNoteCard(
    note: VoiceNote,
    isPlaying: Boolean,
    formatter: SimpleDateFormat,
    onPlayPause: () -> Unit,
    onTranscribe: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackgroundGlass)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Audio header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isPlaying) NeonPink.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Playback button",
                            tint = if (isPlaying) NeonPink else NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Voice Memo (${note.duration}s)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = formatter.format(Date(note.timestamp)),
                            color = SoftTextGray,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete voice file",
                        tint = SoftTextGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Audio transcription section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141624))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "TRANSCRIPTION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = note.transcription,
                        color = if (note.isTranscribed) Color.White else SoftTextGray,
                        fontSize = 11.sp
                    )
                }
            }

            // Action row
            if (!note.isTranscribed && note.transcription != "Transcribing with AI...") {
                Button(
                    onClick = onTranscribe,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI symbol",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Transcribe with Gemini AI",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
