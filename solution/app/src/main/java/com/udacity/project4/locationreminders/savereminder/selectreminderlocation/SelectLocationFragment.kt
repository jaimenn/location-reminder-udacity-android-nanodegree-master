package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.LocationUtils.isForegroundLocationPermissionGranted
import com.udacity.project4.utils.REQUEST_FOREGROUND_LOCATION_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    override val viewModel: SaveReminderViewModel by sharedViewModel()
    private val selectLocationViewModel: SelectLocationViewModel by viewModel()

    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var selectedLocationMarker: Marker
    private lateinit var selectedLocationCircle: Circle

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var lastKnownLocation: Location? = null

    private val resolutionForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                setCurrentLocation()
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    enableMyLocation()
                }.show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        savedInstanceState?.let {
            lastKnownLocation = it.getParcelable(LAST_KNOWN_LOCATION)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_select_location, container, false
        )

        binding.lifecycleOwner = this
        binding.onSaveButtonClicked = View.OnClickListener { onLocationSelected() }
        binding.viewModel = selectLocationViewModel

        binding.radiusSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) { // TODO here
            }

            override fun onStopTrackingTouch(slider: Slider) {
                selectLocationViewModel.closeRadiusSelector()
            }
        })

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        setupGoogleMap()

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(LAST_KNOWN_LOCATION, lastKnownLocation)
    }

    private fun setupGoogleMap() {
        val mapFragment = childFragmentManager
            .findFragmentByTag(getString(R.string.map_fragment)) as? SupportMapFragment
            ?: return

        selectLocationViewModel.radius.observe(viewLifecycleOwner) {
            if (!::selectedLocationCircle.isInitialized) {
                return@observe
            }

            selectedLocationCircle.radius =
                it?.toDouble() ?: GeofenceConstants.DEFAULT_RADIUS_IN_METRES.toDouble()
        }

        selectLocationViewModel.selectedLocation.observe(viewLifecycleOwner) {
            selectedLocationMarker.position = it.latLng
            selectedLocationCircle.center = it.latLng
            setCameraTo(it.latLng)
        }

        mapFragment.getMapAsync(this)
    }

    private fun onLocationSelected() {
        selectLocationViewModel.closeRadiusSelector()
        viewModel.setSelectedLocation(selectLocationViewModel.selectedLocation.value!!)
        viewModel.setSelectedRadius(selectLocationViewModel.radius.value!!)
        viewModel.navigationCommand.postValue(NavigationCommand.Back)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        selectLocationViewModel.closeRadiusSelector()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun setMapType(mapType: Int): Boolean {
            map.mapType = mapType
            return true
        }

        return when (item.itemId) {
            R.id.normal_map -> setMapType(GoogleMap.MAP_TYPE_NORMAL)
            R.id.hybrid_map -> setMapType(GoogleMap.MAP_TYPE_HYBRID)
            R.id.terrain_map -> setMapType(GoogleMap.MAP_TYPE_TERRAIN)
            R.id.satellite_map -> setMapType(GoogleMap.MAP_TYPE_SATELLITE)

            else -> false
        }
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map

        map.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireContext(),
                R.raw.map_style
            )
        )

        val markerOptions = MarkerOptions()
            .position(map.cameraPosition.target)
            .title(getString(R.string.dropped_pin))
            .draggable(true)

        selectedLocationMarker = map.addMarker(markerOptions)

        val circleOptions = CircleOptions()
            .center(map.cameraPosition.target)
            .fillColor(ResourcesCompat.getColor(resources, R.color.sliderLayoutBackground, null))
            .strokeColor(ResourcesCompat.getColor(resources, R.color.sliderLayoutBackground, null))
            .strokeWidth(4f)
            .radius(GeofenceConstants.DEFAULT_RADIUS_IN_METRES.toDouble())

        selectedLocationCircle = map.addCircle(circleOptions)

        viewModel.selectedPlaceOfInterest.value.let {
            selectLocationViewModel.setSelectedLocation(
                it ?: PointOfInterest(map.cameraPosition.target, null, null)
            )
        }

        map.setOnMapClickListener {
            if (selectLocationViewModel.isRadiusSelectorOpen.value == true) {
                selectLocationViewModel.closeRadiusSelector()
            } else {
                selectLocationViewModel.setSelectedLocation(it)
            }
        }

        map.setOnPoiClickListener {
            if (selectLocationViewModel.isRadiusSelectorOpen.value == true) {
                selectLocationViewModel.closeRadiusSelector()
            } else {
                selectLocationViewModel.setSelectedLocation(it)
            }
        }

        map.setOnCameraMoveListener {
            selectLocationViewModel.zoomValue = map.cameraPosition.zoom
        }

        enableMyLocation()
    }

    private fun setCameraTo(latLng: LatLng) {
        val cameraPosition =
            CameraPosition.fromLatLngZoom(latLng, selectLocationViewModel.zoomValue)
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)

        map.animateCamera(cameraUpdate)
    }

    /**
     * Starts the permission check only if permissions have not been granted yet
     */
    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isForegroundLocationPermissionGranted()) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            checkDeviceLocationSettings()
        } else {
            map.isMyLocationEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false
            lastKnownLocation = null
            requestForegroundLocationPermissions()
        }
    }

    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())

        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnSuccessListener { result ->
            result.locationSettingsStates?.let {
                if (it.isLocationPresent) {
                    setCurrentLocation()
                }
            }
        }

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    resolutionForResult.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(this.javaClass.name, sendEx.toString())
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setCurrentLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    moveCameraToLocation(
                        LatLng(
                            location.latitude,
                            location.longitude
                        )
                    )
                    lastKnownLocation = location
                } else {
                    lastKnownLocation.let { lastKnownLocation ->
                        if (lastKnownLocation != null) {
                            moveCameraToLocation(
                                LatLng(
                                    lastKnownLocation.latitude,
                                    lastKnownLocation.longitude
                                )
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                exception.message?.let { Log.e(TAG, it) }
                map.uiSettings.isMyLocationButtonEnabled = false
            }
    }

    private fun moveCameraToLocation(latLng: LatLng) {
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM.toFloat())
        )
    }

    /*
 *  Requests ACCESS_FINE_LOCATION permission
 */
    @TargetApi(29)
    fun Fragment.requestForegroundLocationPermissions() {
        if (isForegroundLocationPermissionGranted()) return

        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = REQUEST_FOREGROUND_LOCATION_PERMISSIONS_REQUEST_CODE

        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_FOREGROUND_LOCATION_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    requestForegroundLocationPermissions()
                }.show()
            }
        }
    }

    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName
    }
}

private const val DEFAULT_ZOOM = 15
private const val LAST_KNOWN_LOCATION = "last_known_location"
