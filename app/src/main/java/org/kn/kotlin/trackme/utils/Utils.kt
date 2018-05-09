package org.kn.kotlin.trackme.utils

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.concurrent.TimeUnit

object Utils {

    val UPDATE_INTERVAL = 10 * 1000  /* 10 secs */
    val FASTEST_INTERVAL = 2000 /* 2 sec */

    fun zoomRoute(map: GoogleMap?, lstLatLngRoute: List<LatLng>?) {
        if (map == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) {
            return
        }
        val boundsBuilder = LatLngBounds.Builder()
        for (latLngPoint in lstLatLngRoute) {
            boundsBuilder.include(latLngPoint)
        }

        val routePadding = 100
        val latLngBounds = boundsBuilder.build()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding))
    }

    fun formatLongTime(millis: Long): String {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1))
    }

    fun formatDistence(distance: Double): String {
        return String.format("%.2f", distance)
    }

    fun formatSpeed(speed: Double): String {
        return String.format("%.2f", speed)
    }
}
