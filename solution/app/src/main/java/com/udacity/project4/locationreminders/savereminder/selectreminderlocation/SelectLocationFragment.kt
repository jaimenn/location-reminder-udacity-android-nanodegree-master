package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.slider.Slider
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.LocationUtils
import com.udacity.project4.utils.PermissionsResultEvent
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.toLatLng
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    override val viewModel: SaveReminderViewModel by inject()
    private val selectLocationViewModel: SelectLocationViewModel by viewModel()

    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private lateinit var selectedLocationMarker: Marker
    private lateinit var selectedLocationCircle: Circle

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_select_location, container, false
        )

        binding.lifecycleOwner = this
        binding.onSaveButtonClicked = View.OnClickListener { onLocationSelected() }
        binding.viewModel = selectLocationViewModel

        binding.radiusSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
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

            if (it == null) {
                startAtCurrentLocation()
            }
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
    }

    private fun locationPermissionHandler(event: PermissionsResultEvent, handler: () -> Unit) {
        if (event.areAllGranted) {
            handler()
            return
        }

        if (event.shouldShowRequestRationale) {
            viewModel.showSnackBar.postValue(getString(R.string.permission_denied_explanation))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAtCurrentLocation() {
        if (!LocationUtils.hasLocationPermissions()) {
            LocationUtils.requestPermissions {
                locationPermissionHandler(it, this::startAtCurrentLocation)
            }

            return
        }

        fun resetToCurrentLocation() =
            LocationUtils.requestSingleUpdate {
                selectLocationViewModel.setSelectedLocation(it.toLatLng())
            }

        map.isMyLocationEnabled = true

        map.setOnMyLocationButtonClickListener {
            selectLocationViewModel.closeRadiusSelector()
            resetToCurrentLocation()
            true
        }

        resetToCurrentLocation()
    }

    private fun setCameraTo(latLng: LatLng) {
        val cameraPosition =
            CameraPosition.fromLatLngZoom(latLng, selectLocationViewModel.zoomValue)
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)

        map.animateCamera(cameraUpdate)
    }
}
