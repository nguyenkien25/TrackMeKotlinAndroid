package org.kn.kotlin.trackme.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class TrackMeInfo : RealmObject() {

    @PrimaryKey
    var id: Long = 0
    var distance: Double = 0.toDouble()
    var duration: Long = 0
    var avgSpeed: Double = 0.toDouble()
    var location: RealmList<String>? = null
}
