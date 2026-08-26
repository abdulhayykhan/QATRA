package com.qatra.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.qatra.app.di.appModule

class QatraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QatraApplication)
            modules(appModule)
        }
    }
}
