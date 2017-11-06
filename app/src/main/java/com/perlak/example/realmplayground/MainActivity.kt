package com.perlak.example.realmplayground

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.perlak.example.realmplayground.model.RepoGenerator
import com.perlak.example.realmplayground.model.VcCommit
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGenerate.setOnClickListener { v ->
            Single.fromCallable {
                Realm.getDefaultInstance().use { realm->
                    realm.executeTransaction{ r->
                        r.deleteAll()
                    }
                }
                var repoGenerator = RepoGenerator()
                var repo = repoGenerator.generateData("test")
                Realm.getDefaultInstance().use { realm ->
                    realm.executeTransactionAsync { r ->
                        r.insertOrUpdate(repo)
                    }
                }
            }.subscribeOn(Schedulers.io()).subscribe()
        }
        commitList.layoutManager = LinearLayoutManager(this)

        commitList.adapter = adapter

    }

    var adapter = CommitAdapter()
    var realm: Realm? = null

    override fun onResume() {
        super.onResume()
        realm = Realm.getDefaultInstance()
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

    fun bind(commit: VcCommit, position:Int) {
        label.text = "#$position ${commit.message}"
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
