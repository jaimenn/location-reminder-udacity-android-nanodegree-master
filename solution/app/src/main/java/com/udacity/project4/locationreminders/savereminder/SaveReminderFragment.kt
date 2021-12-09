package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.LocationUtils.areForegroundAndBackgroundLocationPermissionsGranted
import com.udacity.project4.utils.REQUEST_FOREGROUND_AND_BACKGROUND_LOCATION_PERMISSION_RESULT_CODE
import com.udacity.project4.utils.ReminderPreferences
import com.udacity.project4.utils.dp
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {
    companion object {
        private const val TAG = "SaveReminderFragment"
    }

    private lateinit var geofencingClient: GeofencingClient

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
        android.os.Build.VERSION_CODES.Q

    override val viewModel: SaveReminderViewModel by sharedViewModel()

    private lateinit var binding: FragmentSaveReminderBinding

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val resolutionForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                createReminderAndAddGeofence()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocationLayout.setOnClickListener {
            goToSelectLocation()
        }

        ViewCompat.setElevation(binding.progressBar, 100.dp)

        binding.saveReminder.setOnClickListener {
            checkPermissionsAndStartGeofencing()
        }
    }

    /**
     * Starts the permission check and Geofence process only if the Geofence associated with the
     * current hint isn't yet active.
     */
    private fun checkPermissionsAndStartGeofencing() {
        if (areForegroundAndBackgroundLocationPermissionsGranted()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    private fun foregroundLocationPermissionGranted(): Boolean {
        return (
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            )
    }

    @TargetApi(29)
    private fun backgroundLocationPermissionGranted(): Boolean {
        return if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            true
        }
    }

    private fun createReminderAndAddGeofence() {
        val reminder = ReminderDataItem(
            title = viewModel.reminderTitle.value,
            description = viewModel.reminderDescription.value,
            location = viewModel.reminderSelectedLocationStr.value,
            latitude = viewModel.latitude.value,
            longitude = viewModel.longitude.value,
            radius = viewModel.selectedRadius.value
        )

        if (viewModel.validateEnteredData(reminder)) {
            addGeofenceForClue(reminder)
        }
    }

    /*
     * Adds a Geofence for the current clue. This method should be called after the user has granted
     * the location permission.
     */
    @SuppressLint("MissingPermission")
    private fun addGeofenceForClue(reminder: ReminderDataItem) {
        // Build the Geofence Object
        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                viewModel.selectedRadius.value!!
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        // Build the geofence request
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                viewModel.saveReminder(reminder)
                viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }
            .addOnFailureListener {
                viewModel.showToast.value = getString(R.string.geofences_not_added)
            }
    }

    private fun goToSelectLocation() {
        viewModel.navigationCommand.value =
            NavigationCommand.To(SaveReminderFragmentDirections.toSelectLocationFragment())
    }

    /*
 *  When we get the result from asking the user to turn on device location, we call
 *  checkDeviceLocationSettingsAndStartGeofence again to make sure it's actually on, but
 *  we don't resolve the check to keep the user from seeing an endless loop.
 */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnSuccessListener { result ->
            result.locationSettingsStates?.let {
                if (it.isLocationPresent) {
                    createReminderAndAddGeofence()
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
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onClear()
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
            PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (
                requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_LOCATION_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED
                )
        ) {
            // Permission denied.
            Snackbar.make(
                requireView(),
                R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(
                        Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
