package org.kn.kotlin.trackme.utils

import android.app.Application

import io.realm.Realm
import io.realm.RealmConfiguration

class TrackMeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(applicationContext)
        val realmConfiguration = RealmConfiguration.Builder()
                .name(Realm.DEFAULT_REALM_NAME)
                .schemaVersion(0)
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(realmConfiguration)
    }
}
