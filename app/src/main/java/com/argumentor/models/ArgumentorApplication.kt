package com.argumentor.models

import android.app.Application
import timber.log.Timber

class ArgumentorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
