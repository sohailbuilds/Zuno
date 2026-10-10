package com.zuno.assistant.data

import androidx.room.Database
import androidx.room.RoomDatabase

/** Stores ONLY the user's speech rules. Message contents are never written to disk. */
@Database(entities = [RuleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
}
