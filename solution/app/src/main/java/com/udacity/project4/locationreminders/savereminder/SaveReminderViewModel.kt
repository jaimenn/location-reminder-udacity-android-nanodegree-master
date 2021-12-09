package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch

class SaveReminderViewModel(var app: Application, private val dataSource: ReminderDataSource) :
    BaseViewModel(app) {

    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    val latitude = MutableLiveData<Double?>()
    val longitude = MutableLiveData<Double?>()

    private val _selectedPlaceOfInterest = MutableLiveData<PointOfInterest>()
    private val _selectedRadius = MutableLiveData<Float>()

    val selectedPlaceOfInterest: LiveData<PointOfInterest>
        get() = _selectedPlaceOfInterest

    val selectedRadius: LiveData<Float>
        get() = _selectedRadius

    val selectedPlaceOfInterestName = Transformations.map(_selectedPlaceOfInterest) {
        if (it == null) {
            return@map app.getString(R.string.select_location)
        }

        if (it.name.isNullOrBlank()) {
            return@map "Lat: ${it.latLng.latitude} Lon: ${it.latLng.longitude}"
        }

        it.name.replace("\n", "").trim()
    }

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = ""
        reminderDescription.value = ""
        _selectedPlaceOfInterest.value = null
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem): Boolean {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
            return true
        }

        return false
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.radius,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.toast_reminder_saved)
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.latitude == null || reminderData.longitude == null) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }

        if (reminderData.radius == null) {
            showSnackBarInt.value = R.string.err_select_radius
            return false
        }
        return true
    }

    fun setSelectedLocation(placeOfInterest: PointOfInterest) {
        latitude.postValue(placeOfInterest.latLng.latitude)
        longitude.postValue(placeOfInterest.latLng.longitude)
        _selectedPlaceOfInterest.postValue(placeOfInterest)
    }

    fun setSelectedRadius(radius: Float) {
        _selectedRadius.postValue(radius)
    }
}
