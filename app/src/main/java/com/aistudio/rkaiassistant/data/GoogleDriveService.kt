package com.aistudio.rkaiassistant.data

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object GoogleDriveService {
    private const val TAG = "GoogleDriveService"
    private const val BACKUP_FILE_NAME = "rk_assistant_backup.json"

    private fun getDriveService(context: Context): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).setSelectedAccount(account.account)

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("RK AI Assistant").build()
    }

    suspend fun uploadBackup(context: Context, backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService(context) ?: return@withContext false
            
            val tempFile = File(context.cacheDir, BACKUP_FILE_NAME)
            tempFile.writeText(backupJson)

            val result = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .setFields("files(id, name)")
                .execute()
            
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = BACKUP_FILE_NAME
            fileMetadata.parents = listOf("appDataFolder")

            val mediaContent = FileContent("application/json", tempFile)

            if (result.files.isNullOrEmpty()) {
                service.files().create(fileMetadata, mediaContent).execute()
            } else {
                val existingFileId = result.files[0].id
                service.files().update(existingFileId, com.google.api.services.drive.model.File(), mediaContent).execute()
            }
            
            tempFile.delete()
            Log.d(TAG, "Backup uploaded successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed: ${e.message}")
            false
        }
    }

    suspend fun downloadBackup(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val service = getDriveService(context) ?: return@withContext null

            val result = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .execute()

            if (result.files.isNullOrEmpty()) return@withContext null

            val fileId = result.files[0].id
            val outputStream = java.io.ByteArrayOutputStream()
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            
            val content = outputStream.toString()
            Log.d(TAG, "Backup downloaded successfully.")
            content
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            null
        }
    }
}
