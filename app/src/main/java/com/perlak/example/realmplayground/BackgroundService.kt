package com.perlak.example.realmplayground

import android.app.IntentService
import android.content.Intent
import android.widget.Toast
import com.perlak.example.common.model.RepoGenerator
import io.realm.Realm
import timber.log.Timber

/**
 * Created on 11/6/17.
 */
class BackgroundService : IntentService("Test background") {
    companion object {
        var ACTION_GENERATE = "action.generate"
    }

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_GENERATE -> {
                Realm.getInstance(App.realmConfiguration).use { realm ->
                    realm.executeTransaction { r ->
                        r.deleteAll()
                    }
                }
                var repoGenerator = RepoGenerator()
                var startGeneration = System.currentTimeMillis()
                var repo = repoGenerator.generateData("test")
                var endGeneration = System.currentTimeMillis()
                Realm.getInstance(App.realmConfiguration).use { realm ->
                    realm.executeTransaction { r ->
                        r.insertOrUpdate(repo)
                    }
                }
                var endInsert = System.currentTimeMillis()
                var msg = "generation: ${(endGeneration - startGeneration)} ms\n db insert: ${(endInsert - endGeneration)} ms "
                Timber.i(msg)
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}