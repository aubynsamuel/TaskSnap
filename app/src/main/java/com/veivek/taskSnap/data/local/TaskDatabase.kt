package com.veivek.taskSnap.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.veivek.taskSnap.data.local.dao.TaskDao
import com.veivek.taskSnap.data.local.entity.TaskEntity

/**
 * Room database for TaskSnap.
 * Version 1: Initial schema with TaskEntity.
 */
@Database(
    entities = [TaskEntity::class],
    version = 2,
    exportSchema = true
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        private const val DATABASE_NAME = "tasksnap_database"

        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(false)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
