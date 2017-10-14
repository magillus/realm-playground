package com.perlak.example.realmplayground.model

import io.realm.RealmList
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*


open class VcRepository(@PrimaryKey var id: String? = null, var url: String? = null, var name: String? = null) : RealmObject() {
    var commits: RealmList<VcCommit> = RealmList()
}

@RealmClass
open class VcCommit(@PrimaryKey var id: String? = null, var message: String? = null, var dateTimeMillis: Long = 0, var user: VcUser? = null) : RealmModel {

    var dateTime: Date
        get() = Date(dateTimeMillis)
        set(value) {
            dateTimeMillis = value.time
        }

}

open class VcUser(@PrimaryKey var id: Long = 0, var username: String = "") : RealmObject()