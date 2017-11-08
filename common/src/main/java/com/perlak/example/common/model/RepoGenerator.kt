package com.perlak.example.common.model

import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by mateusz.perlak on 10/10/17.
 */

class RepoGenerator() {


    fun generateUsers(name: String, count: Int): List<VcUser> {
        var users = ArrayList<VcUser>()
        for (i: Int in 0..count) {
            users.add(VcUser(i.toLong(), "${name}_$i"))
        }
        return users
    }

    fun generateData(name: String): VcRepository {
        var users = generateUsers("test", 100)
        var repository = generateRepository("http://example.com/repo_A", "test_A", 100000, users)
        return repository
    }

    fun generateRepository(url: String, name: String, commitCount: Int, userList: List<VcUser>): VcRepository {
        var repo = VcRepository(name, url, name)
        repo.commits.addAll(randomCommit(commitCount, userList))
        return repo
    }

    fun randomCommit(count: Int, users: List<VcUser>): List<VcCommit> {
        val random = Random()
        val commits = ArrayList<VcCommit>()
        var now = System.currentTimeMillis()
        for (i: Int in 0..count - 1) {
            now -= (random.nextInt(5) * 60000 + random.nextInt(30000))
            commits.add(VcCommit(randomString(16), randomString(random.nextInt(20) + 20), now, users[random.nextInt(users.size)]))
        }
        return commits
    }

    fun randomString(size: Int): String {
        val txt = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val sb = StringBuilder()
        val r = Random()
        for (i in 0..size - 1) {
            sb.append(txt[r.nextInt(txt.length)])
        }
        return sb.toString()
    }

}