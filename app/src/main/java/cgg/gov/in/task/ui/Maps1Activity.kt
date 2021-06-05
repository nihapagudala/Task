package cgg.gov.`in`.task.ui

import android.Manifest
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cgg.gov.`in`.task.utils.AppConstants
import cgg.gov.`in`.task.BuildConfig
import cgg.gov.`in`.task.R
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.util.*

class Maps1Activity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mSettingsClient: SettingsClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    var mLocationCallback: LocationCallback? = null
    private val REQUEST_CHECK_SETTINGS = 109
    private var mCurrentLocation: Location? = null
    private var mRequestingLocationUpdates = false

    // location updates interval - 10sec
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000
    private val REQUEST_LOCATION_TURN_ON = 2000


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        turnOnLocation()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun turnOnLocation() {
        val googleApiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API).build()
        googleApiClient.connect()
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result =
            LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
        result.setResultCallback { result ->
            val status = result.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> {
                    init()
                    startLocationUpdates()
                }
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    status.startResolutionForResult(
                        this,
                        REQUEST_LOCATION_TURN_ON
                    )
                } catch (e: SendIntentException) {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                }
                else -> {
                    val i = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(i)
                }
            }
        }
    }


    private fun callLocationPermissions() {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    mRequestingLocationUpdates = true
                    init()
                    startLocationUpdates()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied()) {
                        // open device settings when the permission is
                        // denied permanently
                        openSettings()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (checkPermissions()) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
            } else {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                mCurrentLocation = null
                callLocationPermissions()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message!!)
        }
    }

    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    fun init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                mCurrentLocation = locationResult.lastLocation
                //  mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                val latlng = LatLng(
                    locationResult.lastLocation.latitude,
                    locationResult.lastLocation.longitude
                )
                val mp = MarkerOptions()
                mp.title("Current Location")
                mp.position(latlng)
                mMap.clear()
                mMap.addMarker(mp)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 14f))

                updateLocationUI()
            }
        }
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        mLocationSettingsRequest = builder.build()
    }

    private val mGpsSwitchStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (!AppConstants.locNotExist) {
                    AppConstants.locNotExist = true
                    if (intent != null && intent.action != null &&
                        intent.action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)
                    ) {
                        val locationManager =
                            context.getSystemService(LOCATION_SERVICE) as LocationManager
                        var isGpsEnabled = false
                        if (locationManager != null) {
                            isGpsEnabled =
                                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        }
                        if (locationManager != null) {
                            val isNetworkEnabled =
                                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                        }
                        if (!isGpsEnabled) {
                            customPerAlert()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    fun customPerAlert() {
        try {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if (dialog.window != null && dialog.window!!.attributes != null) {
                dialog.window!!.attributes.windowAnimations = R.style.exitdialog_animation1
                dialog.setContentView(R.layout.custom_alert_information)
                dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.setCancelable(false)
                val versionTitle = dialog.findViewById<TextView>(R.id.version_tv)
                val dialogMessage = dialog.findViewById<TextView>(R.id.dialog_message)
                dialogMessage.text = getString(R.string.plz_grant_loc)
                val yes = dialog.findViewById<Button>(R.id.btDialogYes)
                val no = dialog.findViewById<Button>(R.id.btDialogNo)
                yes.setOnClickListener {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    val intent = intent
                    finish()
                    startActivity(intent)
                }
                no.setOnClickListener {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    finish()
                }
                if (!dialog.isShowing) {
                    dialog.show()
                }
            }
        } catch (e: java.lang.Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    override fun onStart() {
        super.onStart()
        registerReceiver(
            mGpsSwitchStateReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        )
    }
    override fun onStop() {
        super.onStop()
        unregisterReceiver(mGpsSwitchStateReceiver)
        stopLocationUpdates()
    }

    private fun checkPermissions(): Boolean {
        val permissionLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return listPermissionsNeeded.isEmpty()
    }

    fun startLocationUpdates() {
        mSettingsClient
            ?.checkLocationSettings(mLocationSettingsRequest)
            ?.addOnSuccessListener(this) {
                if (checkPermissions()) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return@addOnSuccessListener
                    }
                    mFusedLocationClient!!.requestLocationUpdates(
                        mLocationRequest,
                        mLocationCallback,
                        Looper.myLooper()
                    )
                }
            }
            ?.addOnFailureListener(this) { e ->
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolvableApiException = e as ResolvableApiException
                        resolvableApiException.startResolutionForResult(
                            this@Maps1Activity,
                            REQUEST_CHECK_SETTINGS
                        )
                    } catch (e1: SendIntentException) {
                        Toast.makeText(this, e1.message, Toast.LENGTH_SHORT).show()
                        e1.printStackTrace()
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage = getString(R.string.loc_sett)
                        Toast.makeText(this@Maps1Activity, errorMessage, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
    }


    fun stopLocationUpdates() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient!!.removeLocationUpdates(mLocationCallback)
        }
    }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         super.onActivityResult(requestCode, resultCode, data)
         try {
             if (requestCode == REQUEST_LOCATION_TURN_ON) {
                 if (resultCode == RESULT_OK) {
                     val intent = intent
                     finish()
                     startActivity(intent)
                 } else {
                     customPerAlert()
                 }
             }
         } catch (e: Exception) {
             Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
             e.printStackTrace()
         }
     }

    /* fun customPerAlert() {
         try {
             val dialog = Dialog(this)
             dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
             if (dialog.window != null && dialog.window!!.attributes != null) {
                 dialog.window!!.attributes.windowAnimations = R.style.exitdialog_animation1
                 dialog.setContentView(R.layout.custom_alert_information)
                 dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                 dialog.setCancelable(false)
                 val dialogMessage = dialog.findViewById<TextView>(R.id.dialog_message)
                 dialogMessage.text = getString(R.string.plz_grant_loc)
                 val yes = dialog.findViewById<Button>(R.id.btDialogYes)
                 val no = dialog.findViewById<Button>(R.id.btDialogNo)
                 yes.setOnClickListener {
                     if (dialog.isShowing) {
                         dialog.dismiss()
                     }
                     val intent = intent
                     finish()
                     startActivity(intent)
                 }
                 no.setOnClickListener {
                     if (dialog.isShowing) {
                         dialog.dismiss()
                     }
                     finish()
                 }
                 if (!dialog.isShowing) {
                     dialog.show()
                 }
             }
         } catch (e: Exception) {
             Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
             e.printStackTrace()
         }
     }*/
}