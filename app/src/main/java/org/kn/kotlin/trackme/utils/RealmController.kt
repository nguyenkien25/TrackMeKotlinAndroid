package org.kn.kotlin.trackme.utils

import android.app.Activity
import android.app.Application
import android.support.v4.app.Fragment

import org.kn.kotlin.trackme.model.TrackMeInfo

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort


class RealmController(application: Application) {
    val realm: Realm

    //find all objects in the TrackMeInfo.class
    val trackMes: RealmResults<TrackMeInfo>
        get() = realm.where(TrackMeInfo::class.java).findAll().sort("id", Sort.DESCENDING)

    init {
        realm = Realm.getDefaultInstance()
    }

    //Refresh the realm istance
    fun refresh() {
        realm.refresh()
    }

    //clear all objects from Book.class
    fun clearAll() {

        realm.beginTransaction()
        realm.delete(TrackMeInfo::class.java)
        realm.commitTransaction()
    }

    //query a single item with the given id
    fun getTrackMe(id: String): TrackMeInfo? {

        return realm.where(TrackMeInfo::class.java).equalTo("id", id).findFirst()
    }

    //check if TrackMeInfo.class is empty
    fun hasTrackMes(): Boolean {
        return !realm.where(TrackMeInfo::class.java).findAll().isEmpty()
    }

    companion object {

        var instance: RealmController? = null
            private set

        fun with(fragment: Fragment): RealmController {

            if (instance == null) {
                instance = RealmController(fragment.activity!!.application)
            }
            return instance as RealmController
        }

        fun with(activity: Activity): RealmController {

            if (instance == null) {
                instance = RealmController(activity.application)
            }
            return instance as RealmController
        }

        fun with(application: Application): RealmController {

            if (instance == null) {
                instance = RealmController(application)
            }
            return instance as RealmController
        }
    }
}