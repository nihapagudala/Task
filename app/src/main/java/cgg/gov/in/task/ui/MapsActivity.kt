package cgg.gov.`in`.task.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cgg.gov.`in`.task.R
import cgg.gov.`in`.task.`interface`.ServiceInterface
import cgg.gov.`in`.task.error_handler.ErrorHandler
import cgg.gov.`in`.task.error_handler.ErrorHandlerInterface
import cgg.gov.`in`.task.model.CompaniesRes
import cgg.gov.`in`.task.utils.AppConstants
import cgg.gov.`in`.task.utils.Utils
import cgg.gov.`in`.task.viewmodel.MapViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, PermissionListener,
    ErrorHandlerInterface, ServiceInterface {

    companion object {
        const val REQUEST_CHECK_SETTINGS = 43
    }

    private lateinit var mLastLocation: Location
    private lateinit var companiesList: List<CompaniesRes>
    private lateinit var companiesTempList: MutableList<CompaniesRes>
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var myAttendanceViewModel: MapViewModel? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
//        setSupportActionBar(toolbar)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        fusedLocationProviderClient = FusedLocationProviderClient(this)

    }

    private fun callService() {
        myAttendanceViewModel = MapViewModel(this, application)

//        customProgressDialog.show()
//        val attendanceResLiveData: LiveData<List<CompaniesRes>?>? =
        myAttendanceViewModel?.getServiceResponse()

        /*attendanceResLiveData?.observe(this,
            androidx.lifecycle.Observer<List<Any>?> { response ->
//                customProgressDialog.dismiss()

            })*/
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map ?: return
        callService()
    }

    private fun isPermissionGiven(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun givePermission() {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(this)
            .check()
    }

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        getCurrentLocation()
    }

    override fun onPermissionRationaleShouldBeShown(
        permission: PermissionRequest?,
        token: PermissionToken?
    ) {
        token!!.continuePermissionRequest()
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        Toast.makeText(
            this,
            "Permission required for showing location",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun getCurrentLocation() {

        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = (10 * 60 * 1000).toLong()
        locationRequest.fastestInterval = 1000 * 60 * 2

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val locationSettingsRequest = builder.build()

        val result =
            LocationServices.getSettingsClient(this)
                .checkLocationSettings(locationSettingsRequest)
        result.addOnCompleteListener { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                if (response!!.locationSettingsStates.isLocationPresent) {
                    getLastLocation()
                }
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolvable = exception as ResolvableApiException
                        resolvable.startResolutionForResult(
                            this,
                            REQUEST_CHECK_SETTINGS
                        )
                    } catch (e: IntentSender.SendIntentException) {
                    } catch (e: ClassCastException) {
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    }
                }
            }
        }
    }

    private fun getLastLocation() {
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
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    mLastLocation = task.result

                    var address = "No known address"

                    val gcd = Geocoder(this, Locale.getDefault())
                    val addresses: List<Address>
                    try {
                        addresses = gcd.getFromLocation(
                            mLastLocation.latitude,
                            mLastLocation.longitude,
                            1
                        )
                        if (addresses.isNotEmpty()) {
                            address = addresses[0].getAddressLine(0)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

//                    val icon = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.resources, R.drawable.dialog_round))
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(
                                LatLng(
                                    mLastLocation.latitude,
                                    mLastLocation.longitude
                                )
                            )
                            .title("Current Location")
                            .snippet(address)
//                            .icon(icon)
                    )

                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(mLastLocation.latitude, mLastLocation.longitude))
                        .zoom(17f)
                        .build()
                    googleMap.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            cameraPosition
                        )
                    )

                    companiesTempList = mutableListOf<CompaniesRes>()
                    for (item in companiesList) {
                        getNearCompanies(item)
                    }

                    if (companiesTempList.size > 0) {
//                        Toast.makeText(this, "size " + companiesTempList.size, Toast.LENGTH_LONG)
//                            .show()
                        for (item in companiesTempList) {
                            googleMap.addMarker(
                                MarkerOptions()
                                    .position(
                                        LatLng(
                                            item.latitude,
                                            item.longitude
                                        )
                                    )
                                    .title(item.company_name)
                                    .snippet(item.company_description)
//                            .icon(icon)
                            )
                        }
                    } else {
                        Toast.makeText(this, R.string.companies_not_found, Toast.LENGTH_LONG).show()
                    }

                } else {
                    Toast.makeText(this, "No current location found", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    private fun getNearCompanies(item: CompaniesRes) {

        var cLocation: Location? = null
        var dLocation: Location? = null

        item.let {
            if (it.latitude != 0.0 && it.longitude != 0.0) {
                dLocation = Location("dLoc")
                dLocation!!.latitude = item.latitude
                dLocation!!.longitude = item.longitude

            }
        }
        mLastLocation.let {
            if (it.latitude != 0.0 && it.longitude != 0.0) {
                cLocation = Location("cLoc")
                cLocation!!.latitude = it.latitude
                cLocation!!.longitude = it.longitude
            }
        }

        if (cLocation == null) {
            Toast.makeText(this, R.string.loc_not_ava, Toast.LENGTH_LONG).show()
            return
        }

        if (dLocation != null && dLocation!!.latitude > 0 && dLocation!!.longitude > 0) {
            var distance: Float = Utils.calcDistance(cLocation!!, dLocation)
            if (distance <= AppConstants.DISTANCE) {
                companiesTempList.add(item)
            }
        } else {
            Toast.makeText(this, R.string.loc_not_found, Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    getCurrentLocation()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)

    }

    override fun handleError(e: Throwable?, context: Context?) {
        val errMsg: String = ErrorHandler.handleError(e, context).toString()
        Toast.makeText(applicationContext, errMsg, Toast.LENGTH_SHORT)
            .show()
    }

    override fun handleError(e: String?, context: Context?) {
        Toast.makeText(applicationContext, "" + e, Toast.LENGTH_SHORT)
            .show()
    }

    override fun getCompanies(response: List<CompaniesRes>?) {
        if (response?.size != null) {
            companiesList = response
            if (isPermissionGiven()) {
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
                    return
                }
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings.isMyLocationButtonEnabled = true
                googleMap.uiSettings.isZoomControlsEnabled = true
                getCurrentLocation()
            } else {
                givePermission()
            }
        }
    }
}