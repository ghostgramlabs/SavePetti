package com.ghostgramlabs.pettibox.di

import android.content.Context
import androidx.room.Room
import com.ghostgramlabs.pettibox.data.local.AppDatabase
import com.ghostgramlabs.pettibox.data.local.AttachmentDao
import com.ghostgramlabs.pettibox.data.local.CategoryDao
import com.ghostgramlabs.pettibox.data.local.SaveDao
import com.ghostgramlabs.pettibox.data.local.TagDao
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
            // Foreign-key cascades (CategoryEntity self-FK, attachments, item_tags)
            // require pragma to be on. Room enables it by default since 2.6,
            // but we make the dependency explicit.
            .build()

    @Provides fun provideSaveDao(db: AppDatabase): SaveDao = db.saveDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()
    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
}
