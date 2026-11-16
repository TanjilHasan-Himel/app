package com.eplaytime.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.eplaytime.app.data.dao.PlaylistDao
import com.eplaytime.app.data.dao.SavedQueueDao
import com.eplaytime.app.data.dao.ScheduledTaskDao
import com.eplaytime.app.data.dao.SongDao
import com.eplaytime.app.data.model.Playlist
import com.eplaytime.app.data.model.PlaylistItem
import com.eplaytime.app.data.model.SavedQueue
import com.eplaytime.app.data.model.SavedQueueItem
import com.eplaytime.app.data.model.ScheduledTask
import com.eplaytime.app.data.model.Song

/**
 * PlayTime Room Database
 */
@Database(
    entities = [
        ScheduledTask::class,
        FavoriteSong::class,
        Song::class,
        LyricsEntity::class,
        SongWikiEntity::class,
        Playlist::class,
        PlaylistItem::class,
        SavedQueue::class,
        SavedQueueItem::class
    ],
    version = 11,
    exportSchema = false
)
abstract class PlayTimeDatabase : RoomDatabase() {

    abstract fun scheduledTaskDao(): ScheduledTaskDao
    abstract fun favoritesDAO(): FavoritesDAO
    abstract fun songDao(): SongDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun songWikiDao(): SongWikiDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun savedQueueDao(): SavedQueueDao

    companion object {
        @Volatile
        private var INSTANCE: PlayTimeDatabase? = null

        fun getDatabase(context: Context): PlayTimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlayTimeDatabase::class.java,
                    "playtime_database"
                )
                    .addMigrations(MIGRATION_6_8, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_6_8 = object : Migration(6, 8) {
            override fun migrate(db: SupportSQLiteDatabase) = createOnlineCacheTables(db)
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) = createOnlineCacheTables(db)
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `contentType` TEXT NOT NULL DEFAULT 'TRACK'")
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `contentUris` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `stopAfterMinutes` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `fadeInSeconds` INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `lastRunAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `lastResult` TEXT NOT NULL DEFAULT 'NEVER'")
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `activeCycleId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `activeTrackIndex` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `recoveryAttempts` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `originalSystemVolume` INTEGER NOT NULL DEFAULT -1")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `scheduled_tasks` ADD COLUMN `nextRunAt` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `playlists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `customArtUri` TEXT
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `playlist_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `playlistId` INTEGER NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `songUri` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `duration` INTEGER NOT NULL,
                        `albumArtUri` TEXT,
                        `position` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_items_playlistId` ON `playlist_items` (`playlistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_items_playlistId_position` ON `playlist_items` (`playlistId`, `position`)")

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `saved_queues` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `activeTrackIndex` INTEGER NOT NULL,
                        `activePositionMs` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `saved_queue_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `queueId` INTEGER NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `songUri` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `duration` INTEGER NOT NULL,
                        `albumArtUri` TEXT,
                        `position` INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_queue_items_queueId` ON `saved_queue_items` (`queueId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_queue_items_queueId_position` ON `saved_queue_items` (`queueId`, `position`)")
            }
        }

        private fun createOnlineCacheTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `lyrics_cache` (`songUri` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `syncedLyrics` TEXT, `plainLyrics` TEXT, `source` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`songUri`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `song_wiki` (`songKey` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `summary` TEXT NOT NULL, `description` TEXT, `thumbnailUrl` TEXT, `composer` TEXT, `lyricist` TEXT, `producer` TEXT, `releaseDate` TEXT, `sourceUrl` TEXT, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`songKey`))"""
            )
        }
    }
}
