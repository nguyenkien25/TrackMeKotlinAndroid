package org.kn.kotlin.trackme.activity

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

import org.kn.kotlin.trackme.BuildConfig
import org.kn.kotlin.trackme.R
import org.kn.kotlin.trackme.model.TrackMeInfo
import org.kn.kotlin.trackme.utils.RealmController
import org.kn.kotlin.trackme.utils.Utils

import io.realm.Realm
import io.realm.RealmList

class RecordActivity : AppCompatActivity(), OnMapReadyCallback {

    private var imgPause: ImageButton? = null
    private var imgResume: ImageButton? = null
    private var imgStop: ImageButton? = null

    private var tvDistance: TextView? = null
    private var tvSpeed: TextView? = null
    private var tvDuration: TextView? = null

    private var isAlreadyStartedService = false
    private var isPause = false
    private var mGoogleMap: GoogleMap? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locations: RealmList<String>? = null
    private var markerCurrent: Marker? = null
    private var customHandler: Handler? = null
    private var timeInMilliseconds: Long = 0
    private var timeSwapBuff: Long = 0
    private var updatedTime: Long = 0
    private var startTime: Long = 0
    private var distance: Double = 0.toDouble()
    private var speed: Double = 0.toDouble()
    private var numberCheck: Int = 0
    private var avgSpeed: Double = 0.toDouble()

    private var realm: Realm? = null

    /**
     * Return the availability of GooglePlayServices
     */
    private val isGooglePlayServicesAvailable: Boolean
        get() {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val status = googleApiAvailability.isGooglePlayServicesAvailable(this)
            if (status != ConnectionResult.SUCCESS) {
                if (googleApiAvailability.isUserResolvableError(status)) {
                    googleApiAvailability.getErrorDialog(this, status, 2404).show()
                }
                return false
            }
            return true
        }

    private var mLocationRequest: LocationRequest? = null
    private var mLocationCallback: LocationCallback? = null

    private val updateTimerThread = object : Runnable {
        override fun run() {
            timeInMilliseconds = System.currentTimeMillis() - startTime
            updatedTime = timeSwapBuff + timeInMilliseconds
            tvDuration!!.text = Utils.formatLongTime(updatedTime)
            customHandler!!.postDelayed(this, 0)
        }
    }

    fun pause(view: View) {
        isPause = true
        imgPause!!.visibility = View.GONE
        imgResume!!.visibility = View.VISIBLE
        imgStop!!.visibility = View.VISIBLE
        timeSwapBuff += timeInMilliseconds
        customHandler!!.removeCallbacks(updateTimerThread)
    }

    fun resume(view: View) {
        isPause = false
        imgPause!!.visibility = View.VISIBLE
        imgResume!!.visibility = View.GONE
        imgStop!!.visibility = View.GONE
        startTime = System.currentTimeMillis()
        customHandler!!.postDelayed(updateTimerThread, 0)
    }

    fun stop(view: View) {
        //Stop location sharing service to app server.........
        fusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback!!)
        isAlreadyStartedService = false
        addTrackMe()
        //Ends................................................
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        imgPause = findViewById(R.id.img_pause)
        imgResume = findViewById(R.id.img_pause)
        imgStop = findViewById(R.id.img_pause)
        tvDistance = findViewById(R.id.distance)
        tvSpeed = findViewById(R.id.speed)
        tvDuration = findViewById(R.id.duration)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        this.realm = RealmController.with(this).realm
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        checkGooglePlayServices()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(map: GoogleMap) {
        mGoogleMap = map
    }

    /**
     * Check Google Play services
     */
    private fun checkGooglePlayServices() {

        //Check whether this user has installed Google play service which is being used by Location updates.
        if (isGooglePlayServicesAvailable) {
            //Passing null to indicate that it is executing for the first time.
            checkAndPromptInternet(null)
        } else {
            Toast.makeText(applicationContext, R.string.no_google_play_service_available, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Check & Prompt Internet connection
     */
    private fun checkAndPromptInternet(dialog: DialogInterface?): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected) {
            promptInternetConnect()
            return false
        }

        dialog?.dismiss()
        //Yes there is active internet connection. Next check Location is granted by user or not.
        if (checkPermissions()) { //Yes permissions are granted by the user. Go to the next step.
            startLocationUpdates()
            //startLocationMonitorService();
        } else {  //No user has not granted the permissions yet. Request now.
            requestPermissions()
        }
        return true
    }

    /**
     * Show A Dialog with button to refresh the internet state.
     */
    private fun promptInternetConnect() {
        val builder = AlertDialog.Builder(this@RecordActivity)
        builder.setTitle(R.string.title_alert_no_internet)
        builder.setMessage(R.string.msg_alert_no_internet)

        val positiveText = getString(R.string.btn_label_refresh)
        builder.setPositiveButton(positiveText) { dialog, which ->
            //Block the Application Execution until user grants the permissions
            if (checkAndPromptInternet(dialog)) {
                //Now make sure about location permission.
                if (checkPermissions()) {
                    //Step 2: Start the Location Monitor Service
                    //Everything is there to start the service.
                    startLocationUpdates()
                    // startLocationMonitorService();
                } else if (!checkPermissions()) {
                    requestPermissions()
                }
            }
        }

        val dialog = builder.create()
        dialog.show()
    }

    /**
     * Return the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        val permissionState1 = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        val permissionState2 = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)

        return permissionState1 == PackageManager.PERMISSION_GRANTED && permissionState2 == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start permissions requests.
     */
    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)

        val shouldProvideRationale2 = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)


        // Provide an additional rationale to the img_user. This would happen if the img_user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale || shouldProvideRationale2) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackbar(R.string.permission_rationale, android.R.string.ok, { v ->
                // Request permission
                ActivityCompat.requestPermissions(this@RecordActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE)
            })
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the img_user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(this@RecordActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_PERMISSIONS_REQUEST_CODE)
        }

    }


    /**
     * Shows a [Snackbar].
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private fun showSnackbar(mainTextStringId: Int, actionStringId: Int, listener: (Any) -> Unit) {
        Snackbar.make(findViewById(android.R.id.content), getString(mainTextStringId), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show()
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size <= 0) {
                // If img_user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.i(TAG, "Permission granted, updates requested, starting location updates")
                startLocationUpdates()
                //startLocationMonitorService();

            } else {
                // Permission denied.

                // Notify the img_user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the img_user for permission (device policy or "Never ask
                // again" prompts). Therefore, a img_user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings, { v ->
                    // Build intent that displays the App settings screen.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                })
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        //Stop location sharing service to app server.........
        fusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback!!)
        isAlreadyStartedService = false
        //Ends................................................
    }

    // Trigger new location updates at interval
    protected fun startLocationUpdates() {
        if (!isAlreadyStartedService) {
            distance = 0.0
            speed = 0.0
            avgSpeed = 0.0
            numberCheck = 0
            locations = RealmList()
            //Start location sharing service to app server.........

            // Create the location request to start receiving updates
            mLocationRequest = LocationRequest()
            mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            mLocationRequest!!.interval = Utils.UPDATE_INTERVAL.toLong()
            mLocationRequest!!.fastestInterval = Utils.FASTEST_INTERVAL.toLong()
            mLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    super.onLocationResult(locationResult)
                    if (!isPause) {
                        onLocationChanged(locationResult!!.lastLocation)
                    }
                }
            }
            // Create LocationSettingsRequest object using location request
            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(mLocationRequest!!)
            val locationSettingsRequest = builder.build()

            // Check whether location settings are satisfied
            val settingsClient = LocationServices.getSettingsClient(this)
            settingsClient.checkLocationSettings(locationSettingsRequest)

            // new Google API SDK v11 uses getFusedLocationProviderClient(this)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            fusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback!!, Looper.myLooper())
            timeInMilliseconds = 0L
            timeSwapBuff = 0L
            updatedTime = 0L
            startTime = System.currentTimeMillis()
            customHandler = Handler()
            customHandler!!.postDelayed(updateTimerThread, 0)
            //Ends................................................
        }
    }

    fun onLocationChanged(location: Location) {
        numberCheck++
        // New location has now been determined
        Log.d(TAG, "Updated Location: " + java.lang.Double.toString(location.latitude) + "," + java.lang.Double.toString(location.longitude))
        // You can now create a LatLng Object for use with maps
        locations!!.add(java.lang.Double.toString(location.latitude) + "," + java.lang.Double.toString(location.longitude))
        val latLng = LatLng(location.latitude, location.longitude)
        if (!isAlreadyStartedService) {
            mGoogleMap!!.addMarker(MarkerOptions().position(latLng))
            markerCurrent = mGoogleMap!!.addMarker(MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location))
            )
            mGoogleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            isAlreadyStartedService = true
        } else {
            try {
                val polylineOptions = PolylineOptions()
                val locationBegin = locations!![locations!!.size - 2]!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val lastLatlng = LatLng(java.lang.Double.parseDouble(locationBegin[0]), java.lang.Double.parseDouble(locationBegin[1]))
                polylineOptions.add(lastLatlng)
                polylineOptions.add(latLng)
                polylineOptions.width(5f).color(Color.BLUE).geodesic(true)
                mGoogleMap!!.addPolyline(polylineOptions)
                markerCurrent!!.position = latLng

                val locationA = Location("point A")
                locationA.latitude = lastLatlng.latitude
                locationA.longitude = lastLatlng.longitude
                val locationB = Location("point B")
                locationB.latitude = latLng.latitude
                locationB.longitude = latLng.longitude
                distance += (locationA.distanceTo(locationB) / 1000).toDouble()
                speed = (locationA.distanceTo(locationB) / (Utils.UPDATE_INTERVAL * 1000)).toDouble()
                avgSpeed = (avgSpeed * (numberCheck - 1) + speed) / numberCheck
                Log.d(TAG, "distance $distance")
                tvDistance!!.text = Utils.formatDistence(distance) + " " + getString(R.string.extension_distance)
                tvSpeed!!.text = Utils.formatSpeed(speed) + " " + getString(R.string.extension_speed)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }

    fun addTrackMe() {
        realm!!.beginTransaction()
        val trackMeInfo = TrackMeInfo()
        trackMeInfo.id = System.currentTimeMillis()
        trackMeInfo.distance = distance
        trackMeInfo.avgSpeed = avgSpeed
        trackMeInfo.duration = updatedTime
        trackMeInfo.location = locations
        realm!!.copyToRealm(trackMeInfo)
        realm!!.commitTransaction()
    }

    companion object {
        private val TAG = RecordActivity::class.java.simpleName
        private val REQUEST_PERMISSIONS_REQUEST_CODE = 5
    }
}
