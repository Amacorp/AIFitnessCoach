package com.aicoach.fitness

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FitnessCoachApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application initialization
    }
}
