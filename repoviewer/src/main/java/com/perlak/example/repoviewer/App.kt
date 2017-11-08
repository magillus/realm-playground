package com.perlak.example.repoviewers

import android.app.Application
import android.os.Environment
import android.os.Environment.DIRECTORY_DOCUMENTS
import com.perlak.example.common.Constants
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
                Timber.i("Db file: ${Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS)}")
                realmConfiguration = RealmConfiguration.Builder()
                        .directory(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS))
                        .name(Constants.DB_NAME)
                        .schemaVersion(1)
                        .build()
            } catch (e: Exception) {
                Timber.w("Error on opening db directory")
                realmConfiguration = Realm.getDefaultConfiguration()!!
            }

        }

    }
}