package com.perlak.example.realmplayground

import android.app.IntentService
import android.content.Intent
import android.widget.Toast
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
                var startGeneration = System.currentTimeMillis()
                var repo = repoGenerator.generateData("test")
                var endGeneration = System.currentTimeMillis()
                Realm.getDefaultInstance().use { realm ->
                    realm.executeTransaction { r ->
                        r.insertOrUpdate(repo)
                    }
                }
                var endInsert = System.currentTimeMillis()
                var msg = "generation: ${(endGeneration - startGeneration)} ms\n db insert: ${(endInsert - endGeneration)} ms "
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                Timber.i(msg)
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
