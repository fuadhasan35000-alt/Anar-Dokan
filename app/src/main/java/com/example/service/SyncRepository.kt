package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.SyncQueueEntity
import com.example.data.model.SyncStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SyncRepository(
    private val db: AppDatabase,
    private val context: Context
) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun enqueueChange(
        businessId: String,
        branchId: String,
        entityType: String,
        entityId: String,
        operation: String,
        payloadMap: Map<String, Any>
    ) = withContext(Dispatchers.IO) {
        val json = JSONObject(payloadMap).toString()
        val queueItem = SyncQueueEntity(
            businessId = businessId,
            branchId = branchId,
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payloadJson = json,
            status = SyncStatus.PENDING.name
        )
        db.syncQueueDao().enqueue(queueItem)
        if (isOnline()) {
            syncPendingOperations()
        }
    }

    suspend fun syncPendingOperations(): Int = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext 0
        val pending = db.syncQueueDao().getPendingOperationsDirect()
        var syncedCount = 0

        for (item in pending) {
            try {
                item.let { op ->
                    db.syncQueueDao().updateOperation(op.copy(status = SyncStatus.SYNCING.name))
                    val docRef = firestore.collection("businesses")
                        .document(op.businessId)
                        .collection(op.entityType)
                        .document(op.entityId)

                    if (op.operation == "DELETE") {
                        docRef.delete().await()
                    } else {
                        val json = JSONObject(op.payloadJson)
                        val map = mutableMapOf<String, Any>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            map[key] = json.get(key)
                        }
                        map["syncedAt"] = System.currentTimeMillis()
                        docRef.set(map, SetOptions.merge()).await()
                    }

                    db.syncQueueDao().updateOperation(op.copy(status = SyncStatus.SYNCED.name))
                    syncedCount++
                }
            } catch (e: Exception) {
                Log.w("SyncRepository", "Sync failed for item ${item.id}: ${e.message}")
                db.syncQueueDao().updateOperation(item.copy(
                    status = SyncStatus.FAILED.name,
                    retryCount = item.retryCount + 1
                ))
            }
        }
        db.syncQueueDao().clearSyncedOperations()
        syncedCount
    }
}
