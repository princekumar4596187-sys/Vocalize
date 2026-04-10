package com.vocalize.app.di

import android.content.Context
import androidx.room.Room
import com.vocalize.app.data.local.AppDatabase
import com.vocalize.app.data.local.dao.CategoryDao
import com.vocalize.app.data.local.dao.MemoDao
import com.vocalize.app.data.local.dao.PlaylistDao
import com.vocalize.app.data.local.dao.TagDao
import com.vocalize.app.data.local.dao.ReminderDao
import com.vocalize.app.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMemoDao(db: AppDatabase): MemoDao = db.memoDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
}
