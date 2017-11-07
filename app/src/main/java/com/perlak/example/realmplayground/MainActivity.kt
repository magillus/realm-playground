package com.perlak.example.realmplayground

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.perlak.example.realmplayground.model.RepoGenerator
import com.perlak.example.realmplayground.model.VcCommit
import com.perlak.example.realmplayground.model.VcRepository
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storagePermission()

        btnGenerate.setOnClickListener { v ->
            Single.fromCallable({
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
                    realm.executeTransactionAsync { r ->
                        r.insertOrUpdate(repo)
                    }
                }
                var endInsert = System.currentTimeMillis()
                return@fromCallable "generation: ${(endGeneration - startGeneration)} ms\n db async insert: ${(endInsert - endGeneration)} ms "
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ msg ->
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        Timber.i(msg)
                    })
        }
        btnGenerateServiceExternal.setOnClickListener({
            var intent = Intent(this, BackgroundServiceExternal::class.java)
            intent.action = BackgroundService.ACTION_GENERATE
            startService(intent)
        })

        btnGenerateService.setOnClickListener({
            var intent = Intent(this, BackgroundService::class.java)
            intent.action = BackgroundService.ACTION_GENERATE
            startService(intent)
        })

        btnCopyFromDb.setOnClickListener({
            Single.fromCallable({
                var startCopy = 0L
                var endCopy = 0L

                var repoInMemory = Realm.getInstance(App.realmConfiguration).use { realm ->
                    startCopy = System.currentTimeMillis()
                    var repoInDb = realm.where(VcRepository::class.java).findFirst()
                    var copyRepo = realm.copyFromRealm(repoInDb)
                    endCopy = System.currentTimeMillis()
                    return@use copyRepo
                }

                return@fromCallable "copy from DB to Memory: ${(endCopy - startCopy)} ms"
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ msg ->
                        Timber.i(msg)
                        Snackbar.make(btnCopyFromDb, msg, Snackbar.LENGTH_LONG).show()
                    })
        })

        commitList.layoutManager = LinearLayoutManager(this)

        commitList.adapter = adapter

    }

    fun storagePermission() {
        val permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Timber.i("Permission to record denied")

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Permission to access the SD-CARD is required for this app to Download PDF.")
                        .setTitle("Permission required")

                builder.setPositiveButton("OK", { dialog, id ->
                    Timber.i("Clicked")
                    makeRequest()
                })

                val dialog = builder.create()
                dialog.show()

            } else {
                makeRequest()
            }
        }
    }

    val REQUEST_WRITE_STORAGE = 222
    fun makeRequest() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE)
    }

    var adapter = CommitAdapter()
    var realm: Realm? = null

    override fun onResume() {
        super.onResume()
        realm = Realm.getInstance(App.realmConfiguration)
        realm?.let { r ->
            var result = r.where(VcCommit::class.java).findAllSorted("dateTimeMillis")
            result.addChangeListener { changeSet ->
                adapter.setData(changeSet)
                label.text = "Total commits: ${changeSet.size}"
            }
            adapter.setData(result)
            label.text = "Total commits: ${result.size}"
        }
    }

    override fun onPause() {
        super.onPause()
        realm?.close()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        App.initConfig()
    }
}

class CommitAdapter(var commitList: List<VcCommit> = emptyList()) : RecyclerView.Adapter<CommitViewHolder>() {
    fun setData(newCommits: List<VcCommit>) {
        commitList = newCommits
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: CommitViewHolder?, position: Int) {
        holder?.bind(commitList[position], position)
    }

    override fun getItemCount(): Int {
        return commitList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): CommitViewHolder {
        return CommitViewHolder(parent!!)
    }

}

class CommitViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.commit_view, parent, false)
) {
    var label = itemView.findViewById<TextView>(R.id.label)
    var btnDelete = itemView.findViewById<Button>(R.id.btnDelete)
    var btnDeleteEx = itemView.findViewById<Button>(R.id.btnDeleteEx)

    fun bind(commit: VcCommit, position: Int) {
        label.text = "#$position ${commit.message}"
        btnDelete.tag = commit.id
        btnDelete.setOnClickListener({
            var commitId = btnDelete.tag as String
            if (commitId != null) {
                Single.fromCallable {
                    // no need it if we use async transaction

                    Timber.i("Directly delete commit $commitId")
                    Realm.getInstance(App.realmConfiguration).use { realm ->
                        realm.executeTransaction { r ->
                            r.where(VcCommit::class.java).equalTo("id", commitId).findAll().deleteAllFromRealm()
                        }
                    }
                }.subscribeOn(Schedulers.io()).subscribe()
            }
        })
        btnDeleteEx.tag = commit.id
        btnDeleteEx.setOnClickListener({
            var commitId = btnDeleteEx.tag as String
            if (commitId != null) {
                var ctx = btnDeleteEx.context
                var deleteIntent = Intent(ctx, BackgroundServiceExternal::class.java)
                deleteIntent.action = BackgroundServiceExternal.ACTION_DELETE_COMMIT
                deleteIntent.putExtra(BackgroundServiceExternal.EXTRA_ID, commitId)
                ctx.startService(deleteIntent)
                Timber.i("Posting to delete commit $commitId")
            }
        })
    }
}
