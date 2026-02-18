package com.ddosassistant.util

import android.content.Context
import android.net.Uri
import java.security.MessageDigest
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

fun queryDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex) ?: "artifact.bin"
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "artifact.bin"
}

fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}
