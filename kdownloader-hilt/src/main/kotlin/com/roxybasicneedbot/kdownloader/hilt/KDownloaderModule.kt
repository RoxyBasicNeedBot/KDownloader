package com.roxybasicneedbot.kdownloader.hilt

import android.content.Context
import com.roxybasicneedbot.kdownloader.android.KDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KDownloaderModule {

    @Provides
    @Singleton
    fun provideKDownloader(
        @ApplicationContext context: Context
    ): KDownloader {
        return KDownloader.getInstance(context)
    }
}
