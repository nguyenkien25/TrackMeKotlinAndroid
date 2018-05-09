package org.kn.kotlin.trackme.activity

import android.content.Intent
import android.os.Bundle
import android.view.View

import org.kn.kotlin.trackme.adapter.HistoryAdapter
import org.kn.kotlin.trackme.model.TrackMeInfo
import org.kn.kotlin.trackme.utils.RealmController

import java.util.ArrayList

class HistoryActivityImpl : HistoryActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRecyclerView!!.setHasFixedSize(true)
    }

    override fun createMapListAdapter(): HistoryAdapter {
        val adapter = HistoryAdapter()
        val list = ArrayList<TrackMeInfo>()
        list.addAll(RealmController.instance!!.trackMes)
        adapter.setTrackMes(list)
        adapter.setContext(this)

        return adapter
    }

    override fun showNewRecord(view: View) {
        val intent = Intent(this, RecordActivity::class.java)
        startActivity(intent)
    }

}
