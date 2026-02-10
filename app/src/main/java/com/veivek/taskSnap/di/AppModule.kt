package com.veivek.taskSnap.di

import android.content.Context
import com.veivek.taskSnap.data.local.TaskDatabase
import com.veivek.taskSnap.data.local.dao.TaskDao
import com.veivek.taskSnap.data.repository.TaskRepositoryImpl
import com.veivek.taskSnap.domain.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideTaskDatabase(
        @ApplicationContext context: Context,
    ): TaskDatabase {
        return TaskDatabase.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideTaskDao(taskDatabase: TaskDatabase): TaskDao {
        return taskDatabase.taskDao()
    }

    @Singleton
    @Provides
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepositoryImpl(taskDao)
    }
}
