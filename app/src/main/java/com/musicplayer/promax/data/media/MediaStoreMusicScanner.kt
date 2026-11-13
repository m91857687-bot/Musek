package com.musicplayer.promax.data.media

import android.content.ContentUris
import java.io.IOException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.musicplayer.promax.data.local.MusicLibraryDatabase
import com.musicplayer.promax.data.local.MusicTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads only metadata exposed by Android's media index; audio bytes are never copied or modified. */
class MediaStoreMusicScanner(
    private val context: Context,
    private val database: MusicLibraryDatabase
) {
    suspend fun scanVolume(
        volumeName: String,
        fullMetadataScan: Boolean,
        legacyPathPrefix: String? = null
    ): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(volumeName)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val checkpoint = database.scanCheckpointDao().get(volumeName)
        val hasCachedTracks = database.musicTrackDao().trackKeysForVolume(volumeName).isNotEmpty()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISPLAY_NAME
        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.Audio.Media.RELATIVE_PATH)
        } else emptyArray()
        val selectionParts = mutableListOf<String>()
        val selectionValues = mutableListOf<String>()
        if (!fullMetadataScan && hasCachedTracks && checkpoint != null && checkpoint.lastSuccessfulScanSeconds > 0L) {
            // A small overlap protects against coarse filesystem timestamps.
            selectionParts += "${MediaStore.Audio.Media.DATE_MODIFIED} >= ?"
            selectionValues += (checkpoint.lastSuccessfulScanSeconds - 2L).coerceAtLeast(0L).toString()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !legacyPathPrefix.isNullOrBlank()) {
            selectionParts += "${MediaStore.Audio.Media.DATA} LIKE ?"
            selectionValues += legacyPathPrefix
        }
        val selection = selectionParts.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
        val selectionArgs = selectionValues.takeIf { it.isNotEmpty() }?.toTypedArray()

        val changedTracks = mutableListOf<MusicTrackEntity>()
        val metadataCursor = resolver.query(collection, projection, selection, selectionArgs, null)
            ?: throw IOException("MediaStore returned no metadata cursor for $volumeName")
        metadataCursor.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val mimeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val displayNameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val relativePathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val title = cursor.getString(titleIndex)?.takeIf(String::isNotBlank)
                    ?: cursor.getString(displayNameIndex)?.substringBeforeLast('.')
                    ?: "Unknown title"
                val uri = ContentUris.withAppendedId(collection, id)
                changedTracks += MusicTrackEntity(
                    trackKey = "$volumeName:$id",
                    mediaStoreId = id,
                    volumeName = volumeName,
                    contentUri = uri.toString(),
                    title = title,
                    artist = cursor.getString(artistIndex)?.takeUnless { it == "<unknown>" }.orEmpty(),
                    album = cursor.getString(albumIndex)?.takeUnless { it == "<unknown>" }.orEmpty(),
                    durationMs = cursor.getLong(durationIndex),
                    sizeBytes = cursor.getLong(sizeIndex),
                    dateModifiedSeconds = cursor.getLong(modifiedIndex),
                    mimeType = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null,
                    relativePath = if (relativePathIndex >= 0) cursor.getString(relativePathIndex) else null
                )
                if (changedTracks.size >= 400) {
                    database.musicTrackDao().upsertTracks(changedTracks.toList())
                    changedTracks.clear()
                }
            }
        }
        if (changedTracks.isNotEmpty()) database.musicTrackDao().upsertTracks(changedTracks)

        // Reconcile IDs only. This is substantially cheaper than rereading all metadata on each run.
        val presentKeys = HashSet<String>()
        val idSelection = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !legacyPathPrefix.isNullOrBlank()) {
            "${MediaStore.Audio.Media.DATA} LIKE ?"
        } else null
        val idSelectionArgs = if (idSelection != null) arrayOf(legacyPathPrefix!!) else null
        val idCursor = resolver.query(
            collection,
            arrayOf(MediaStore.Audio.Media._ID),
            idSelection,
            idSelectionArgs,
            null
        ) ?: throw IOException("MediaStore returned no ID cursor for $volumeName")
        idCursor.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) presentKeys += "$volumeName:${cursor.getLong(idIndex)}"
        }
        val staleKeys = database.musicTrackDao().trackKeysForVolume(volumeName)
            .filterNot(presentKeys::contains)
        if (staleKeys.isNotEmpty()) database.musicTrackDao().deleteTracks(staleKeys)

        val now = System.currentTimeMillis() / 1000L
        database.scanCheckpointDao().save(
            com.musicplayer.promax.data.local.ScanCheckpointEntity(
                volumeName = volumeName,
                lastSuccessfulScanSeconds = now,
                lastFullReconciliationSeconds = now
            )
        )
        presentKeys.size
    }

    suspend fun scanSafTree(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val hasPersistedReadGrant = context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }
        if (!hasPersistedReadGrant) throw SecurityException("The selected SD-card folder permission is no longer available")
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("Unable to open the selected SD-card folder")
        val volumeKey = "saf:${treeUri}"
        // Keep the previously indexed tree until this replacement tree has been
        // traversed successfully. A provider error must not erase a valid library.
        val found = mutableListOf<MusicTrackEntity>()
        var totalFound = 0
        val existingByUri = database.musicTrackDao().fingerprintsForVolume(volumeKey).associateBy { it.contentUri }
        val presentKeys = HashSet<String>()
        val pending = ArrayDeque<DocumentFile>()
        pending.add(tree)
        while (pending.isNotEmpty()) {
            val directory = pending.removeLast()
            directory.listFiles().forEach { child ->
                if (child.isDirectory) {
                    pending.add(child)
                } else if (child.isFile && isAudioFile(child)) {
                    val uri = child.uri
                    val name = child.name ?: "Unknown title"
                    val trackKey = "saf:$uri"
                    presentKeys += trackKey
                    val modifiedSeconds = child.lastModified() / 1000L
                    val size = child.length()
                    val old = existingByUri[uri.toString()]
                    if (old == null || old.dateModifiedSeconds != modifiedSeconds || old.sizeBytes != size) {
                        found += MusicTrackEntity(
                            trackKey = trackKey,
                            mediaStoreId = uri.toString().hashCode().toLong(),
                            volumeName = volumeKey,
                            contentUri = uri.toString(),
                            title = name.substringBeforeLast('.'),
                            artist = "",
                            album = "",
                            durationMs = 0L,
                            sizeBytes = size,
                            dateModifiedSeconds = modifiedSeconds,
                            mimeType = child.type,
                            relativePath = null
                        )
                    }
                    totalFound++
                    if (found.size >= 300) {
                        database.musicTrackDao().upsertTracks(found.toList())
                        found.clear()
                    }
                }
            }
        }
        if (found.isNotEmpty()) database.musicTrackDao().upsertTracks(found)
        val stale = database.musicTrackDao().trackKeysForVolume(volumeKey)
            .filterNot(presentKeys::contains)
        if (stale.isNotEmpty()) database.musicTrackDao().deleteTracks(stale)
        // Switch the active SAF source only after a complete successful traversal.
        database.musicTrackDao().deleteOtherSafTrees(volumeKey)
        totalFound
    }

    private fun isAudioFile(file: DocumentFile): Boolean {
        if (file.type?.startsWith("audio/", ignoreCase = true) == true) return true
        val extension = file.name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return extension in setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "wma", "aiff")
    }

    companion object {
        fun availableVolumeNames(context: Context): Set<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.getExternalVolumeNames(context)
            } else {
                setOf(MediaStore.VOLUME_EXTERNAL)
            }

        fun primaryVolumeName(): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL_PRIMARY
            else MediaStore.VOLUME_EXTERNAL

        fun legacyPrimaryPathPrefix(): String =
            Environment.getExternalStorageDirectory().absolutePath.trimEnd('/') + "/%"
    }
}
