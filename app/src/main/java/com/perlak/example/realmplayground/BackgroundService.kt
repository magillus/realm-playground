package com.perlak.example.realmplayground

import android.app.IntentService
import android.content.Intent
import com.perlak.example.realmplayground.model.RepoGenerator
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
                Realm.getDefaultInstance().use { realm ->
                    realm.executeTransaction { r ->
                        r.deleteAll()
                    }
                }
                var repoGenerator = RepoGenerator()
                var startGeneration = System.nanoTime()
                var repo = repoGenerator.generateData("test")
                var endGeneration = System.nanoTime()
                Realm.getDefaultInstance().use { realm ->
                    realm.executeTransaction { r ->
                        r.insertOrUpdate(repo)
                    }
                }
                var endInsert = System.nanoTime()
                Timber.i("generation: ${(endGeneration - startGeneration) / 10000000f} ms\n db insert: ${(endInsert - endGeneration) / 1000000f} ms ")
            }
        }
    }
}