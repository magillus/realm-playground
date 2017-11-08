package com.perlak.example.repoviewer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.perlak.example.common.model.VcCommit
import com.perlak.example.repoviewers.App
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storagePermission()

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

    override fun onStart() {
        super.onStart()

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

    override fun onStop() {
        super.onStop()
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
    }
}