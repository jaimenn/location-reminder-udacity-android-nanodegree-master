package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.locationreminders.geofence.GeofenceConstants

class SelectLocationViewModel : ViewModel() {

    private val _isRadiusSelectorOpen = MutableLiveData(false)
    private val _selectedLocation = MutableLiveData<PointOfInterest>()

    var zoomValue = 15.5f

    val isRadiusSelectorOpen: LiveData<Boolean>
        get() = _isRadiusSelectorOpen

    val selectedLocation: LiveData<PointOfInterest>
        get() = _selectedLocation

    val radius = MutableLiveData(GeofenceConstants.DEFAULT_RADIUS_IN_METRES)

    fun toggleRadiusSelector() {
        _isRadiusSelectorOpen.postValue(!(_isRadiusSelectorOpen.value ?: false))
    }

    fun setSelectedLocation(pointOfInterest: PointOfInterest) {
        _selectedLocation.postValue(pointOfInterest)
    }

    fun setSelectedLocation(latLng: LatLng) {
        setSelectedLocation(PointOfInterest(latLng, null, null))
    }

    fun closeRadiusSelector() {
        _isRadiusSelectorOpen.postValue(false)
    }
}