package com.example.ddosassistant.data.network

import com.google.gson.annotations.SerializedName

data class GraphDriveItem(
    val id: String? = null,
    val name: String? = null,
    @SerializedName("webUrl") val webUrl: String? = null
)

data class CreateFolderRequest(
    val name: String,
    val folder: Map<String, Any> = emptyMap(),
    @SerializedName("@microsoft.graph.conflictBehavior") val conflictBehavior: String = "fail"
)

data class UploadSessionRequest(
    val item: UploadSessionItem = UploadSessionItem()
)

data class UploadSessionItem(
    @SerializedName("@microsoft.graph.conflictBehavior") val conflictBehavior: String = "fail"
)

data class UploadSessionResponse(
    @SerializedName("uploadUrl") val uploadUrl: String,
    @SerializedName("expirationDateTime") val expirationDateTime: String? = null
)

data class TeamsPostMessageRequest(
    val body: TeamsMessageBody
)

data class TeamsMessageBody(
    val content: String
)

data class TeamsMessageResponse(
    val id: String? = null
)
