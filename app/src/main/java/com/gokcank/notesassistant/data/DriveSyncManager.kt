package com.gokcank.notesassistant.data

import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume

/** Drive'a yazılan eşitleme dosyasının içeriği (çöptekiler dahil tüm kayıtlar). */
@Serializable
data class SyncData(
    val version: Int = 1,
    val syncedAt: Long,
    val notes: List<Note>,
    val items: List<ChecklistItem>,
)

/**
 * Google Drive eşitlemesi: notlar Drive'ın uygulamaya özel gizli alanında
 * (appDataFolder) tek bir dosyada tutulur. Eşitlemede uzak dosya indirilir,
 * her not için "en son düzenlenen kazanır" kuralıyla birleştirilir ve
 * birleşik durum geri yüklenir. Notlar cihazlar arasında [Note.syncId] ile eşleşir.
 *
 * Koda hiçbir gizli değer gömülü değildir; yetki Google Play hizmetleri
 * üzerinden kullanıcının kendi hesabıyla alınır.
 */
class DriveSyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsStore: SettingsStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * İlk bağlanma: kullanıcıdan izin ister. İzin zaten verilmişse [onGranted],
     * onay ekranı gerekiyorsa [onResolution] (ekranı başlatmak çağıranın işi),
     * hata durumunda [onError] çağrılır.
     */
    fun requestAuthorization(
        onGranted: () -> Unit,
        onResolution: (PendingIntent) -> Unit,
        onError: () -> Unit,
    ) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()
        Identity.getAuthorizationClient(context).authorize(request)
            .addOnSuccessListener { result ->
                when {
                    result.hasResolution() ->
                        result.pendingIntent?.let(onResolution) ?: onError()
                    else -> onGranted()
                }
            }
            .addOnFailureListener { onError() }
    }

    /** Daha önce izin verildiyse sessizce erişim anahtarı alır; yoksa null. */
    private suspend fun getAccessToken(): String? = suspendCancellableCoroutine { cont ->
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()
        Identity.getAuthorizationClient(context).authorize(request)
            .addOnSuccessListener { result ->
                cont.resume(if (result.hasResolution()) null else result.accessToken)
            }
            .addOnFailureListener { cont.resume(null) }
    }

    /**
     * Tam eşitleme turu. Eşitleme kapalıysa, izin yoksa veya ağ hatası olursa
     * sessizce false döner — çağıran taraf istediğinde kullanıcıya bildirir.
     */
    suspend fun syncNow(): Boolean {
        if (!settingsStore.driveSyncEnabled.first()) return false
        val token = getAccessToken() ?: return false
        return withContext(Dispatchers.IO) {
            runCatching {
                val dao = database.noteDao()
                val fileId = findSyncFile(token)
                val remote = fileId?.let { id ->
                    runCatching {
                        json.decodeFromString(SyncData.serializer(), download(token, id))
                    }.getOrNull()
                }
                merge(remote, settingsStore.lastSyncAt.first())

                val now = System.currentTimeMillis()
                val payload = json.encodeToString(
                    SyncData.serializer(),
                    SyncData(
                        syncedAt = now,
                        notes = dao.getAllNotesForSync(),
                        items = dao.getAllItemsForSync(),
                    ),
                )
                if (fileId == null) create(token, payload) else update(token, fileId, payload)
                settingsStore.setLastSyncAt(now)
                true
            }.getOrDefault(false)
        }
    }

    /**
     * Uzak durumu yerelle birleştirir. Not bazında en son düzenlenen kazanır
     * (silinme anı da düzenleme sayılır). Uzakta hiç olmayan eski yerel not,
     * son eşitlemeden beri değişmediyse başka cihazda kalıcı silinmiş demektir.
     */
    private suspend fun merge(remote: SyncData?, lastSync: Long) {
        if (remote == null) return
        val dao = database.noteDao()
        val localBySyncId = dao.getAllNotesForSync().associateBy { it.syncId }
        val remoteItems = remote.items.groupBy { it.noteId }

        for (remoteNote in remote.notes) {
            val local = localBySyncId[remoteNote.syncId]
            val remoteModified = maxOf(remoteNote.updatedAt, remoteNote.deletedAt ?: 0)
            when {
                local == null -> {
                    if (remoteModified > lastSync) {
                        val newId = dao.insertNote(remoteNote.copy(id = 0))
                        val items = remoteItems[remoteNote.id].orEmpty()
                            .map { it.copy(id = 0, noteId = newId) }
                        if (items.isNotEmpty()) dao.insertItems(items)
                    }
                    // aksi halde bu cihazda kalıcı silinmiş bir nottur; geri alınmaz
                }
                remoteModified > maxOf(local.updatedAt, local.deletedAt ?: 0) -> {
                    dao.updateNote(remoteNote.copy(id = local.id))
                    dao.deleteItemsForNote(local.id)
                    val items = remoteItems[remoteNote.id].orEmpty()
                        .map { it.copy(id = 0, noteId = local.id) }
                    if (items.isNotEmpty()) dao.insertItems(items)
                }
            }
        }

        if (lastSync > 0) {
            val remoteIds = remote.notes.mapTo(mutableSetOf()) { it.syncId }
            for (local in localBySyncId.values) {
                val localModified = maxOf(local.updatedAt, local.deletedAt ?: 0)
                if (local.syncId !in remoteIds && localModified <= lastSync) {
                    dao.deleteNote(local.id)
                }
            }
        }
    }

    // --- Drive REST çağrıları (uygulamaya özel gizli alan) ---

    private fun findSyncFile(token: String): String? {
        val q = URLEncoder.encode("name='$SYNC_FILE_NAME'", "UTF-8")
        val body = request(
            token,
            "GET",
            "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=files(id)",
        )
        return Regex(""""id"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
    }

    private fun download(token: String, fileId: String): String =
        request(token, "GET", "https://www.googleapis.com/drive/v3/files/$fileId?alt=media")

    private fun create(token: String, content: String) {
        val boundary = "notesassistant-sync"
        val metadata = """{"name":"$SYNC_FILE_NAME","parents":["appDataFolder"]}"""
        val body = buildString {
            append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\nContent-Type: application/json\r\n\r\n")
            append(content)
            append("\r\n--$boundary--")
        }
        request(
            token,
            "POST",
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart",
            body,
            "multipart/related; boundary=$boundary",
        )
    }

    private fun update(token: String, fileId: String, content: String) {
        request(
            token,
            "PATCH",
            "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media",
            content,
            "application/json",
        )
    }

    private fun request(
        token: String,
        method: String,
        url: String,
        body: String? = null,
        contentType: String? = null,
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            // HttpURLConnection PATCH bilmez; Google API'lerinin desteklediği
            // yöntem-geçersiz-kılma başlığıyla POST üzerinden gönderilir
            if (method == "PATCH") {
                connection.requestMethod = "POST"
                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else {
                connection.requestMethod = method
            }
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            if (body != null) {
                connection.doOutput = true
                contentType?.let { connection.setRequestProperty("Content-Type", it) }
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            if (code !in 200..299) error("Drive isteği başarısız: HTTP $code")
            return connection.inputStream.use { it.readBytes().decodeToString() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val SYNC_FILE_NAME = "notes-sync.json"
    }
}
