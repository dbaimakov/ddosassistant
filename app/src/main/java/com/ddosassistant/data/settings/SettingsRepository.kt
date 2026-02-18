package com.ddosassistant.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ddos_settings")

data class AppSettings(
    val graphToken: String,
    val sharePointDriveId: String,
    val sharePointBaseFolderPath: String,
    val teamsTeamId: String,
    val teamsChannelId: String,
    val kibanaBaseUrl: String,
    val kibanaApiKey: String,
    val defaultEmailTo: String,
    val defaultActorName: String
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val GRAPH_TOKEN = stringPreferencesKey("graph_token")
        val DRIVE_ID = stringPreferencesKey("sp_drive_id")
        val BASE_PATH = stringPreferencesKey("sp_base_path")
        val TEAM_ID = stringPreferencesKey("teams_team_id")
        val CHANNEL_ID = stringPreferencesKey("teams_channel_id")
        val KIBANA_URL = stringPreferencesKey("kibana_url")
        val KIBANA_API_KEY = stringPreferencesKey("kibana_api_key")
        val EMAIL_TO = stringPreferencesKey("email_to")
        val ACTOR_NAME = stringPreferencesKey("actor_name")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            graphToken = prefs[Keys.GRAPH_TOKEN] ?: "",
            sharePointDriveId = prefs[Keys.DRIVE_ID] ?: "",
            sharePointBaseFolderPath = prefs[Keys.BASE_PATH] ?: "Evidence",
            teamsTeamId = prefs[Keys.TEAM_ID] ?: "",
            teamsChannelId = prefs[Keys.CHANNEL_ID] ?: "",
            kibanaBaseUrl = prefs[Keys.KIBANA_URL] ?: "",
            kibanaApiKey = prefs[Keys.KIBANA_API_KEY] ?: "",
            defaultEmailTo = prefs[Keys.EMAIL_TO] ?: "",
            defaultActorName = prefs[Keys.ACTOR_NAME] ?: "Analyst"
        )
    }

    suspend fun update(transform: suspend (MutableSettings) -> Unit) {
        context.dataStore.edit { prefs ->
            val ms = MutableSettings(prefs)
            transform(ms)
            ms.applyTo(prefs)
        }
    }

    class MutableSettings internal constructor(prefs: Preferences) {
        private var graphToken: String? = null
        private var driveId: String? = null
        private var basePath: String? = null
        private var teamId: String? = null
        private var channelId: String? = null
        private var kibanaUrl: String? = null
        private var kibanaApiKey: String? = null
        private var emailTo: String? = null
        private var actorName: String? = null

        fun graphToken(value: String) { graphToken = value }
        fun driveId(value: String) { driveId = value }
        fun basePath(value: String) { basePath = value }
        fun teamId(value: String) { teamId = value }
        fun channelId(value: String) { channelId = value }
        fun kibanaUrl(value: String) { kibanaUrl = value }
        fun kibanaApiKey(value: String) { kibanaApiKey = value }
        fun emailTo(value: String) { emailTo = value }
        fun actorName(value: String) { actorName = value }

        internal fun applyTo(out: androidx.datastore.preferences.core.MutablePreferences) {
            graphToken?.let { out[Keys.GRAPH_TOKEN] = it }
            driveId?.let { out[Keys.DRIVE_ID] = it }
            basePath?.let { out[Keys.BASE_PATH] = it.trim().trimStart('/').trimEnd('/') }
            teamId?.let { out[Keys.TEAM_ID] = it.trim() }
            channelId?.let { out[Keys.CHANNEL_ID] = it.trim() }
            kibanaUrl?.let { out[Keys.KIBANA_URL] = it.trim().trimEnd('/') }
            kibanaApiKey?.let { out[Keys.KIBANA_API_KEY] = it.trim() }
            emailTo?.let { out[Keys.EMAIL_TO] = it.trim() }
            actorName?.let { out[Keys.ACTOR_NAME] = it.trim().ifBlank { "Analyst" } }
        }
    }
}
