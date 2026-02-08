package com.aicoach.fitness.di

import android.content.Context
import com.aicoach.fitness.utils.PoseAnalyzer
import com.aicoach.fitness.utils.VoiceAssistant
import com.google.gson.Gson
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
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideVoiceAssistant(
        @ApplicationContext context: Context
    ): VoiceAssistant {
        return VoiceAssistant(context)
    }

    @Provides
    @Singleton
    fun providePoseAnalyzer(
        @ApplicationContext context: Context,
        gson: Gson
    ): PoseAnalyzer {
        return PoseAnalyzer(context, gson)
    }
}
