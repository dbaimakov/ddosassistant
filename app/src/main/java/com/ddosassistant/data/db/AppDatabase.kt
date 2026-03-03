package com.ddosassistant.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        IncidentEntity::class,
        AttackWaveEntity::class,
        MitigationEntity::class,
        EvidenceEntity::class,
        ChangeLogEntity::class,
        CommunicationEntity::class
    ],
    version = 2,
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE incidents ADD COLUMN category TEXT NOT NULL DEFAULT 'DDOS'")
                db.execSQL("ALTER TABLE incidents ADD COLUMN additionalInfo TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ddos_incident_assistant.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
