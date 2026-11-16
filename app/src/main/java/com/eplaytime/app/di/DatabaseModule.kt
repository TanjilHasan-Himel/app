package com.eplaytime.app.di

import android.content.Context
import androidx.room.Room
import com.eplaytime.app.data.database.FavoritesDAO
import com.eplaytime.app.data.database.PlayTimeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DatabaseModule - Hilt module for providing database instances
 * Centralizes database configuration and dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provide PlayTimeDatabase singleton
     */
    @Singleton
    @Provides
    fun providePlayTimeDatabase(
        @ApplicationContext context: Context
    ): PlayTimeDatabase {
        return Room.databaseBuilder(
            context,
            PlayTimeDatabase::class.java,
            "playtime_database"
        )
            .addMigrations(
                PlayTimeDatabase.MIGRATION_6_8,
                PlayTimeDatabase.MIGRATION_7_8,
                PlayTimeDatabase.MIGRATION_8_9,
                PlayTimeDatabase.MIGRATION_9_10,
                PlayTimeDatabase.MIGRATION_10_11
            )
            .build()
    }

    /**
     * Provide FavoritesDAO from database
     */
    @Singleton
    @Provides
    fun provideFavoritesDAO(database: PlayTimeDatabase): FavoritesDAO {
        return database.favoritesDAO()
    }

    /**
     * Provide SongDao from database
     */
    @Singleton
    @Provides
    fun provideSongDao(database: PlayTimeDatabase): com.eplaytime.app.data.dao.SongDao {
        return database.songDao()
    }

    /**
     * Provide ScheduledTaskDao from database
     */
    @Singleton
    @Provides
    fun provideScheduledTaskDao(database: PlayTimeDatabase): com.eplaytime.app.data.dao.ScheduledTaskDao {
        return database.scheduledTaskDao()
    }

    /**
     * Provide PlaylistDao from database
     */
    @Singleton
    @Provides
    fun providePlaylistDao(database: PlayTimeDatabase): com.eplaytime.app.data.dao.PlaylistDao {
        return database.playlistDao()
    }

    /**
     * Provide SavedQueueDao from database
     */
    @Singleton
    @Provides
    fun provideSavedQueueDao(database: PlayTimeDatabase): com.eplaytime.app.data.dao.SavedQueueDao {
        return database.savedQueueDao()
    }
}
