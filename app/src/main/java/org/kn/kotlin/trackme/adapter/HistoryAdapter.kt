package org.kn.kotlin.trackme.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

import com.google.android.gms.maps.MapView

import org.kn.kotlin.trackme.R
import org.kn.kotlin.trackme.model.TrackMeInfo
import org.kn.kotlin.trackme.utils.Utils

import java.util.ArrayList
import java.util.HashSet

class HistoryAdapter : RecyclerView.Adapter<HistoryViewHolder>() {
    var mapViews = HashSet<MapView>()
        private set
    private var trackMeInfos: ArrayList<TrackMeInfo>? = null
    private var context: Context? = null

    fun setTrackMes(trackMeInfos: ArrayList<TrackMeInfo>) {
        this.trackMeInfos = trackMeInfos
    }

    fun setContext(context: Context) {
        this.context = context
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): HistoryViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.activity_list_item, viewGroup, false)
        val viewHolder = HistoryViewHolder(viewGroup.context, view)

        mapViews.add(viewHolder.mapView!!)

        return viewHolder
    }

    override fun onBindViewHolder(viewHolder: HistoryViewHolder, position: Int) {
        val trackMeInfo = trackMeInfos!![position]

        viewHolder.itemView.tag = trackMeInfo

        viewHolder.tvDistance!!.text = Utils.formatDistence(trackMeInfo.distance) + " " + context!!.getString(R.string.extension_distance)
        viewHolder.tvAvgSpeed!!.text = Utils.formatSpeed(trackMeInfo.avgSpeed) + " " + context!!.getString(R.string.extension_avg_speed)
        viewHolder.tvDuration!!.text = Utils.formatLongTime(trackMeInfo.duration)

        viewHolder.setRouteMaps(trackMeInfo.location!!)
        viewHolder.setDistance(trackMeInfo.distance)
    }

    override fun getItemCount(): Int {
        return if (trackMeInfos == null) 0 else trackMeInfos!!.size
    }
}
