package com.perlak.example.repoviewers

import android.app.Application
import com.perlak.example.common.model.TestConfiguration
import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber

/**
 * Created by mateusz.perlak on 10/11/17.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Realm.init(this)

        initConfig()
    }

    companion object {
        lateinit var realmConfiguration: RealmConfiguration

        fun initConfig() {

            try {
                realmConfiguration = TestConfiguration.getRealmConfig()
            } catch (e: Exception) {
                Timber.w("Error on opening db directory")
                realmConfiguration = Realm.getDefaultConfiguration()!!
            }

        }

    }
}