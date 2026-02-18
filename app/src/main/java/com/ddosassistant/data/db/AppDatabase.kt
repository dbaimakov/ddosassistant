package com.ddosassistant.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        IncidentEntity::class,
        AttackWaveEntity::class,
        MitigationEntity::class,
        EvidenceEntity::class,
        ChangeLogEntity::class,
        CommunicationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao
    abstract fun attackWaveDao(): AttackWaveDao
    abstract fun mitigationDao(): MitigationDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun changeLogDao(): ChangeLogDao
    abstract fun communicationDao(): CommunicationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ddos_incident_assistant.db"
                ).build().also { INSTANCE = it }
            }
    }
}
