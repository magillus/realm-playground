package com.perlak.example.realmplayground

import android.app.Application
import io.realm.Realm
import timber.log.Timber

/**
 * Created by mateusz.perlak on 10/11/17.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Realm.init(this)
    }
}