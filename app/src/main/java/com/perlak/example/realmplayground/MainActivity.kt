package com.perlak.example.realmplayground

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.perlak.example.realmplayground.MainActivity.Companion.dateFormatter
import com.perlak.example.realmplayground.model.RepoGenerator
import com.perlak.example.realmplayground.model.VcCommit
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    companion object {
        var dateFormatter = SimpleDateFormat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        realm = Realm.getDefaultInstance()

        btnGenerate.setOnClickListener { v ->
            Single.fromCallable {
                var repoGenerator = RepoGenerator()
                var repo = repoGenerator.generateData("test")
                Realm.getDefaultInstance().use { realm ->
                    realm.executeTransactionAsync { r ->
                        r.deleteAll()
                        r.insertOrUpdate(repo)
                    }
                }
            }.subscribeOn(Schedulers.io()).subscribe()
        }
        btnToggleSort.setOnClickListener {
            sort = if (sort == Sort.ASCENDING) Sort.DESCENDING else Sort.ASCENDING
            realm?.let {
                listenForResults(it.where(VcCommit::class.java).findAllSorted("dateTimeMillis", sort))
            }
        }
        commitList.layoutManager = LinearLayoutManager(this)

        commitList.adapter = adapter

    }

    override fun onDestroy() {
        super.onDestroy()
        realm?.close()
    }
    var adapter = CommitAdapter()
    var sort = Sort.ASCENDING
    var realm: Realm? = null
    var resultChangeListener: RealmChangeListener<RealmResults<VcCommit>> = RealmChangeListener { changeSet ->
        adapter.setData(changeSet)
    }
    var realmResults: RealmResults<VcCommit>? = null

    override fun onResume() {
        super.onResume()
        realm?.let { realm ->
            var result = realm.where(VcCommit::class.java).findAllSorted("dateTimeMillis")
            listenForResults(result)
        }
    }

    fun listenForResults(result: RealmResults<VcCommit>?) {
        realmResults?.removeAllChangeListeners()
        realmResults = result
        realmResults?.addChangeListener(resultChangeListener)
        realmResults?.let { adapter.setData(it) }
    }

    override fun onPause() {
        super.onPause()
        realmResults?.removeAllChangeListeners()
    }

}

class CommitAdapter(var commitList: List<VcCommit> = emptyList()) : RecyclerView.Adapter<CommitViewHolder>() {
    fun setData(newCommits: List<VcCommit>) {
        commitList = newCommits
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: CommitViewHolder?, position: Int) {
        holder?.bind(commitList[position])
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
    var datetime = itemView.findViewById<TextView>(R.id.datetime)
    var btnDelete = itemView.findViewById<Button>(R.id.btnDelete)

    fun bind(commit: VcCommit) {
        label.text = commit.message
        datetime.text = dateFormatter.format(commit.dateTime)
        btnDelete.tag = commit.id
        btnDelete.setOnClickListener {
            var commitId = btnDelete.tag as String
            if (commitId != null) {
                Single.fromCallable {
                    // no need it if we use async transaction
                    Realm.getDefaultInstance().use { realm ->
                        realm.executeTransaction { r ->
                            r.where(VcCommit::class.java).equalTo("id", commitId).findAll().deleteAllFromRealm()
                        }
                    }
                }.subscribeOn(Schedulers.io()).subscribe()
            }
        }
    }
}
