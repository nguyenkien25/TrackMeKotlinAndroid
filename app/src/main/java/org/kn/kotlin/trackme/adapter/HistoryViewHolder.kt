package org.kn.kotlin.trackme.adapter

import android.content.Context
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

import org.kn.kotlin.trackme.R
import org.kn.kotlin.trackme.utils.Utils

import java.util.ArrayList

class HistoryViewHolder(private val mContext: Context, view: View) : RecyclerView.ViewHolder(view), OnMapReadyCallback {

    internal var tvDistance: TextView? = null
    internal var tvAvgSpeed: TextView? = null
    internal var tvDuration: TextView? = null
    internal var mapView: MapView? = null

    private var mGoogleMap: GoogleMap? = null
    private lateinit var listRouteMaps: List<String>
    private var distance: Double = 0.toDouble()

    init {
        mapView = view.findViewById(R.id.map) as MapView
        tvDistance = view.findViewById(R.id.distance) as TextView
        tvAvgSpeed = view.findViewById(R.id.avg_speed) as TextView
        tvDuration = view.findViewById(R.id.duration) as TextView
        mapView!!.onCreate(null)
        mapView!!.getMapAsync(this)
    }

    fun setRouteMaps(listRouteMaps: List<String>) {
        this.listRouteMaps = listRouteMaps

        // If the map is ready, update its content.
        if (mGoogleMap != null) {
            updateMapContents()
        }
    }

    fun setDistance(distance: Double) {
        this.distance = distance
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        MapsInitializer.initialize(mContext)
        googleMap.uiSettings.isMapToolbarEnabled = false

        // If we have map data, update the map content.
        if (listRouteMaps.size > 0) {
            updateMapContents()
        }
    }

    protected fun updateMapContents() {
        // Since the mapView is re-used, need to remove pre-existing mapView features.
        mGoogleMap!!.clear()
        val polylineOptions = PolylineOptions()
        val lstLatLngRoute = ArrayList<LatLng>()

        for (i in listRouteMaps.indices) {
            val location = listRouteMaps[i].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                val latLng = LatLng(java.lang.Double.parseDouble(location[0]), java.lang.Double.parseDouble(location[1]))
                polylineOptions.add(latLng)
                lstLatLngRoute.add(latLng)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
        polylineOptions.width(5f).color(Color.BLUE).geodesic(true)
        mGoogleMap!!.addPolyline(polylineOptions)
        if (distance < 3) {
            mGoogleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(lstLatLngRoute[lstLatLngRoute.size - 1], 17f))
        } else {
            Utils.zoomRoute(mGoogleMap, lstLatLngRoute)
        }
        val locationBegin = listRouteMaps[0].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val locationEnd = listRouteMaps[listRouteMaps.size - 1].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        mGoogleMap!!.addMarker(MarkerOptions()
                .position(LatLng(java.lang.Double.parseDouble(locationBegin[0]), java.lang.Double.parseDouble(locationBegin[1])))
        )
        mGoogleMap!!.addMarker(MarkerOptions()
                .position(LatLng(java.lang.Double.parseDouble(locationEnd[0]), java.lang.Double.parseDouble(locationEnd[1])))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location))
        )
    }
}
