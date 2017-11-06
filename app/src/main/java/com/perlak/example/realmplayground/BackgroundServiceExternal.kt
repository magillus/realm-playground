package com.perlak.example.realmplayground

import android.app.IntentService
import android.content.Intent
import com.perlak.example.realmplayground.model.RepoGenerator
import com.perlak.example.realmplayground.model.VcCommit
import io.realm.Realm
import timber.log.Timber

/**
 * Created on 11/6/17.
 */
class BackgroundServiceExternal : IntentService("Test background external") {
    companion object {
        var ACTION_GENERATE = "action.generate"
        var ACTION_DELETE_COMMIT = "action.delete.commit"
        var EXTRA_ID = "id"
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
            ACTION_DELETE_COMMIT -> {
                var id = intent.getStringExtra(EXTRA_ID)
                Timber.i("Deleting commit on separate process: $id")
                Realm.getDefaultInstance().use {
                    it.executeTransaction { realm ->
                        realm.where(VcCommit::class.java).equalTo("id", id).findAll().deleteAllFromRealm()
                    }
                }
            }
        }

    }

}
