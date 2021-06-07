package cgg.gov.`in`.task.ui

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cgg.gov.`in`.task.R
import cgg.gov.`in`.task.`interface`.ServiceInterface
import cgg.gov.`in`.task.adapter.CompaniesAdapter
import cgg.gov.`in`.task.adapter.CustomInfoWindowAdapter
import cgg.gov.`in`.task.databinding.EmpBottomSheetBinding
import cgg.gov.`in`.task.error_handler.ErrorHandler
import cgg.gov.`in`.task.error_handler.ErrorHandlerInterface
import cgg.gov.`in`.task.model.CompaniesRes
import cgg.gov.`in`.task.utils.AppConstants
import cgg.gov.`in`.task.utils.CustomProgressDialog
import cgg.gov.`in`.task.utils.Utils
import cgg.gov.`in`.task.viewmodel.MapViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetDialog
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
    private var customProgressDialog: CustomProgressDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
//        setSupportActionBar(toolbar)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        customProgressDialog = CustomProgressDialog(this)


    }

    private fun callService() {
        myAttendanceViewModel = MapViewModel(this, application)
        if (Utils.checkInternetConnection(this)) {
            customProgressDialog?.show()
            myAttendanceViewModel?.getServiceResponse()
        } else {
            Toast.makeText(this, getString(R.string.plz_check_int), Toast.LENGTH_LONG).show()
        }
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
        locationRequest.interval = (1000 * 10).toLong() //10sec
        locationRequest.fastestInterval = 1000 * 2 //2 sec

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

                    val icon = BitmapDescriptorFactory.fromBitmap(
                        BitmapFactory.decodeResource(
                            this.resources,
                            R.drawable.location_current
                        )
                    )

                    val adapter = CustomInfoWindowAdapter(this)
                    googleMap.setInfoWindowAdapter(adapter)


                    var markerMap: HashMap<String, String> = HashMap<String, String>()

                    val current_marker: Marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(
                                LatLng(
                                    mLastLocation.latitude,
                                    mLastLocation.longitude
                                )
                            )
                            .title(getString(R.string.current_loc))
                            .snippet(address)
                            .icon(icon)
                    )
                    current_marker.showInfoWindow()
                    current_marker.tag = getString(R.string.current_loc)

                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(mLastLocation.latitude, mLastLocation.longitude))
                        .zoom(5f)
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
                        Toast.makeText(
                            this,
                            "Nearby " + companiesTempList.size + " companies found",
                            Toast.LENGTH_LONG
                        )
                            .show()

                        val icon = BitmapDescriptorFactory.fromBitmap(
                            BitmapFactory.decodeResource(
                                this.resources,
                                R.drawable.location_blue
                            )
                        )
                        var pos = 0
                        for (item in companiesTempList) {
                            val marker: Marker = googleMap.addMarker(
                                MarkerOptions()
                                    .position(
                                        LatLng(
                                            item.latitude,
                                            item.longitude
                                        )
                                    )

                                    .title(item.avg_rating.toString())
                                    .icon(icon)
                            )
                            marker.tag = pos.toString();
                            pos++
                        }

                        googleMap.setOnMarkerClickListener { marker ->
                            if (marker.isInfoWindowShown) {
                                marker.hideInfoWindow()
                            } else {
                                marker.showInfoWindow()
                                val pos: String = marker.tag as String
                                if (!pos.equals(getString(R.string.current_loc)))
                                    showBottomsheetDialog(
                                        companiesTempList.get(pos.toInt()),
                                        pos.toInt()
                                    )
                            }
                            true
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


    private fun showBottomsheetDialog(item: CompaniesRes, pos: Int) {
        val empBottomSheetBinding: EmpBottomSheetBinding = DataBindingUtil.inflate(
            LayoutInflater.from(this),
            R.layout.emp_bottom_sheet, null, false
        )
        val dialog: Dialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        dialog.setContentView(empBottomSheetBinding.getRoot())
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        dialog.show()

        val distance = (item.distance) / 1000
        empBottomSheetBinding.tvName.setText(item.company_name)
        empBottomSheetBinding.tvDes.setText(item.company_description)
        empBottomSheetBinding.tvRating.setText(item.avg_rating.toString())
        empBottomSheetBinding.tvDistance.setText("%.1f".format(distance) + " km")
        empBottomSheetBinding.ratingbar.rating = item.avg_rating.toFloat()

        if (!TextUtils.isEmpty(item.company_image_url)) {
            empBottomSheetBinding.pBar.setVisibility(View.VISIBLE)
            Glide.with(this)
                .load(item.company_image_url)
                .error(R.drawable.login_user)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        empBottomSheetBinding.pBar.setVisibility(View.GONE)
                        empBottomSheetBinding.ivUser.setVisibility(View.VISIBLE)
                        empBottomSheetBinding.ivUser.setImageDrawable(
                            resources.getDrawable(
                                R.drawable.login_user
                            )
                        )
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        empBottomSheetBinding.pBar.setVisibility(View.GONE)
                        empBottomSheetBinding.ivUser.setVisibility(View.VISIBLE)
                        return false
                    }
                })
                .into(empBottomSheetBinding.ivUser)

        }

        setAdapter(empBottomSheetBinding, pos)
    }

    private fun setAdapter(binding: EmpBottomSheetBinding, pos: Int) {
        var list: MutableList<CompaniesRes> = mutableListOf<CompaniesRes>()
        list.addAll(companiesTempList)
        list.removeAt(pos)

        val attendanceAdapter = CompaniesAdapter(this, list)
        binding.recyclerview.setAdapter(attendanceAdapter)
        binding.recyclerview.setLayoutManager(
            LinearLayoutManager(
                this,
                RecyclerView.HORIZONTAL,
                false
            )
        )

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
                item.distance = distance
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
        customProgressDialog?.dismiss()
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

//    override fun onInfoWindowClick(marker: Marker) {


//        String actionId = markerMap . get (marker.getId());
//
//        if (actionId.equals("action_one")) {
//            Intent i = new Intent(MainActivity.this, ActivityOne.class);
//            startActivity(i);
//        } else if (actionId.equals("action_two")) {
//
//        }
//    }


}