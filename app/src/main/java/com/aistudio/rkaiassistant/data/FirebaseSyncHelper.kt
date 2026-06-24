package com.aistudio.rkaiassistant.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirebaseSyncHelper {
    private const val TAG = "FirebaseSyncHelper"
    
    private val db by lazy { try { FirebaseFirestore.getInstance() } catch (e: Exception) { null } }
    private val auth by lazy { try { FirebaseAuth.getInstance() } catch (e: Exception) { null } }

    private fun getUserId(): String? = auth?.currentUser?.uid

    suspend fun uploadToCloud(collection: String, data: Any, id: String) {
        val firestore = db ?: return
        val uid = getUserId() ?: return
        try {
            firestore.collection("users").document(uid)
                .collection(collection).document(id)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Uploaded $id to $collection")
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to $collection", e)
        }
    }

    suspend fun deleteFromCloud(collection: String, id: String) {
        val firestore = db ?: return
        val uid = getUserId() ?: return
        try {
            firestore.collection("users").document(uid)
                .collection(collection).document(id)
                .delete()
                .await()
            Log.d(TAG, "Deleted $id from $collection")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting from $collection", e)
        }
    }

    suspend fun downloadFromCloud(collection: String): List<Map<String, Any>> {
        val firestore = db ?: return emptyList()
        val uid = getUserId() ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(uid)
                .collection(collection).get().await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading from $collection", e)
            emptyList()
        }
    }
}
