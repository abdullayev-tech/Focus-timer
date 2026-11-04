package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// The log's own file, so res/xml/backup_rules.xml and data_extraction_rules.xml can keep it out of
// cloud backup while the timers still get backed up. Rename it there too
private const val EVENT_LOG_FILE = "focus_timer_log"

@Database(entities = [TimerEvent::class], version = 1, exportSchema = false)
abstract class EventLogDatabase : RoomDatabase() {
    abstract fun timerEventDao(): TimerEventDao

    companion object {
        @Volatile
        private var INSTANCE: EventLogDatabase? = null

        fun getDatabase(context: Context): EventLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EventLogDatabase::class.java,
                    EVENT_LOG_FILE
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
