package com.blanksstudio.gridpix.di

import android.content.Context
import androidx.room.Room
import com.blanksstudio.gridpix.data.local.GridPixDatabase
import com.blanksstudio.gridpix.data.local.PuzzleProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GridPixDatabase =
        Room.databaseBuilder(context, GridPixDatabase::class.java, GridPixDatabase.NAME)
            // Migrations are added here with .addMigrations(...) when the version changes.
            .build()

    @Provides
    fun providePuzzleProgressDao(db: GridPixDatabase): PuzzleProgressDao = db.puzzleProgressDao()
}
