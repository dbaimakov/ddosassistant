package com.example.ddosassistant

import android.content.Context
import com.example.ddosassistant.data.db.AppDatabase
import com.example.ddosassistant.data.network.ElkConnector
import com.example.ddosassistant.data.network.GraphConnector
import com.example.ddosassistant.data.network.NetworkModule
import com.example.ddosassistant.data.repo.IncidentRepository
import com.example.ddosassistant.data.settings.SettingsRepository
import com.google.gson.Gson

object AppServices {
    private var initialized = false

    lateinit var database: AppDatabase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var incidentRepository: IncidentRepository
        private set

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        database = AppDatabase.get(context)
        settingsRepository = SettingsRepository(context)

        val (graphApi, okHttp) = NetworkModule.graphService()
        val gson = Gson()

        val graphConnector = GraphConnector(graphApi, okHttp, gson)
        val elkConnector = ElkConnector(okHttp, gson)

        incidentRepository = IncidentRepository(
            context = context.applicationContext,
            db = database,
            settingsRepository = settingsRepository,
            graph = graphConnector,
            elk = elkConnector,
            gson = gson
        )
    }
}
