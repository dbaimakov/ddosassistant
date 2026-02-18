package com.ddosassistant.data.network

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import retrofit2.HttpException

class GraphConnector(
    private val api: GraphApiService,
    private val okHttp: OkHttpClient,
    private val gson: Gson = Gson()
) {
    companion object {
        // Graph "simple upload" has a practical limit (~4MB). Use upload session above this.
        private const val SMALL_UPLOAD_MAX_BYTES: Int = 4 * 1024 * 1024
        private const val DEFAULT_CHUNK_SIZE: Int = 320 * 1024 // 320 KiB
    }

    suspend fun createIncidentFolder(
        bearerToken: String,
        driveId: String,
        baseFolderPath: String,
        incidentId: String
    ): GraphDriveItem {
        val auth = bearer(bearerToken)
        val incidentFolderName = "Incident-$incidentId"
        return try {
            api.createFolder(
                authorization = auth,
                driveId = driveId,
                parentPath = baseFolderPath.trim().trimStart('/').trimEnd('/'),
                body = CreateFolderRequest(name = incidentFolderName, conflictBehavior = "fail")
            )
        } catch (e: HttpException) {
            if (e.code() == 409) {
                // Folder already exists; treat as success.
                GraphDriveItem(name = incidentFolderName)
            } else {
                throw e
            }
        }
    }

    suspend fun uploadBytes(
        bearerToken: String,
        driveId: String,
        itemPath: String,
        bytes: ByteArray,
        contentType: String = "application/octet-stream"
    ): GraphDriveItem {
        val auth = bearer(bearerToken)
        val cleanPath = itemPath.trim().trimStart('/')

        return if (bytes.size <= SMALL_UPLOAD_MAX_BYTES) {
            val body = bytes.toRequestBody(contentType.toMediaTypeOrNull())
            api.uploadSmallFile(auth, driveId, cleanPath, body)
        } else {
            uploadLargeFileWithSession(auth, driveId, cleanPath, bytes, contentType)
        }
    }

    suspend fun postTeamsMessage(
        bearerToken: String,
        teamId: String,
        channelId: String,
        contentHtml: String
    ): TeamsMessageResponse {
        val auth = bearer(bearerToken)
        return api.postTeamsMessage(
            authorization = auth,
            teamId = teamId,
            channelId = channelId,
            body = TeamsPostMessageRequest(body = TeamsMessageBody(content = contentHtml))
        )
    }

    private suspend fun uploadLargeFileWithSession(
        authHeader: String,
        driveId: String,
        itemPath: String,
        bytes: ByteArray,
        contentType: String
    ): GraphDriveItem {
        val session = api.createUploadSession(
            authorization = authHeader,
            driveId = driveId,
            itemPath = itemPath,
            body = UploadSessionRequest()
        )

        val uploadUrl = session.uploadUrl
        val total = bytes.size.toLong()
        var start = 0L

        while (start < total) {
            val endExclusive = minOf(start + DEFAULT_CHUNK_SIZE, total)
            val chunk = bytes.copyOfRange(start.toInt(), endExclusive.toInt())
            val endInclusive = endExclusive - 1

            val request = Request.Builder()
                .url(uploadUrl)
                .put(chunk.toRequestBody(contentType.toMediaTypeOrNull()))
                .header("Content-Length", chunk.size.toString())
                .header("Content-Range", "bytes $start-$endInclusive/$total")
                .build()

            okHttp.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    throw IOException("Upload chunk failed: HTTP ${resp.code} ${resp.message}. Body=$body")
                }

                // When upload completes, Graph returns a driveItem JSON
                val responseBody = resp.body?.string().orEmpty()
                val maybeDriveItem = runCatching { gson.fromJson(responseBody, GraphDriveItem::class.java) }.getOrNull()
                if (maybeDriveItem?.id != null) {
                    return maybeDriveItem
                }
            }

            start = endExclusive
        }

        throw IOException("Upload session finished without returning a driveItem. Check Graph response and file size.")
    }

    private fun bearer(token: String): String {
        val t = token.trim()
        require(t.isNotBlank()) { "Graph token is empty. Configure a bearer token in Settings." }
        return if (t.startsWith("Bearer ", ignoreCase = true)) t else "Bearer $t"
    }
}
