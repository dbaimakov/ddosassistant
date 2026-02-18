package com.example.ddosassistant.data.network

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface GraphApiService {

    // Create a folder under /drive/root:/parentPath:/children
    @POST("drives/{driveId}/root:/{parentPath}:/children")
    suspend fun createFolder(
        @Header("Authorization") authorization: String,
        @Path("driveId") driveId: String,
        @Path(value = "parentPath", encoded = true) parentPath: String,
        @Body body: CreateFolderRequest
    ): GraphDriveItem

    // Upload small file (<= ~4MB) to /drive/root:/itemPath:/content
    @PUT("drives/{driveId}/root:/{itemPath}:/content")
    suspend fun uploadSmallFile(
        @Header("Authorization") authorization: String,
        @Path("driveId") driveId: String,
        @Path(value = "itemPath", encoded = true) itemPath: String,
        @Body body: RequestBody
    ): GraphDriveItem

    // Create an upload session for large files
    @POST("drives/{driveId}/root:/{itemPath}:/createUploadSession")
    suspend fun createUploadSession(
        @Header("Authorization") authorization: String,
        @Path("driveId") driveId: String,
        @Path(value = "itemPath", encoded = true) itemPath: String,
        @Body body: UploadSessionRequest = UploadSessionRequest()
    ): UploadSessionResponse

    // Post a message into a Teams channel
    @POST("teams/{teamId}/channels/{channelId}/messages")
    suspend fun postTeamsMessage(
        @Header("Authorization") authorization: String,
        @Path("teamId") teamId: String,
        @Path("channelId") channelId: String,
        @Body body: TeamsPostMessageRequest
    ): TeamsMessageResponse
}
