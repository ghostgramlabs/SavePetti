package com.savepetti.di

import android.content.Context
import androidx.room.Room
import com.savepetti.data.local.AppDatabase
import com.savepetti.data.local.AttachmentDao
import com.savepetti.data.local.CategoryDao
import com.savepetti.data.local.MIGRATION_1_2
import com.savepetti.data.local.SaveDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "savepetti.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun provideSaveDao(db: AppDatabase): SaveDao = db.saveDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()
}
