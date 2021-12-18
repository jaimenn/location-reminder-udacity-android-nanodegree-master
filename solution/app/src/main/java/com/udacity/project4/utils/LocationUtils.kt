package com.udacity.project4.utils

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.LatLng
import org.koin.core.context.GlobalContext
import java.util.concurrent.Executors

fun Location.toLatLng() = LatLng(latitude, longitude)

object LocationUtils {
    private const val PROVIDER = LocationManager.GPS_PROVIDER

    private val locationPermissions =
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private val requestExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val locationManager: LocationManager?
        get() =
            GlobalContext.getOrNull()
                ?.koin
                ?.get<Application>()
                ?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    @SuppressLint("MissingPermission")
    fun requestSingleUpdate(block: (Location) -> Unit) {
        fun doRequest() {
            if (hasLocationPermissions()) {
                locationManager?.getLastKnownLocation(PROVIDER)?.let {
                    block(it)
                    return
                }
            } else {
                block(Location(LocationManager.NETWORK_PROVIDER))
            }
        }

        if (!hasLocationPermissions()) {
            PermissionManager.requestPermissions(*locationPermissions) {
                if (it.areAllGranted) {
                    doRequest()
                }
            }
        }
    }

    @TargetApi(29)
    fun Fragment.areForegroundAndBackgroundLocationPermissionsGranted(): Boolean {
        return isForegroundLocationPermissionGranted() && isBackgroundLocationPermissionGranted()
    }

    @TargetApi(29)
    fun Fragment.isForegroundLocationPermissionGranted(): Boolean {
        return (
            PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) && (
            PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
    }

    @TargetApi(29)
    fun Fragment.isBackgroundLocationPermissionGranted(): Boolean {
        return if (runningQ /*|| runningROrLater*/) {
            PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
        } else {
            true
        }
    }

    private fun hasLocationPermissions(): Boolean =
        PermissionManager.arePermissionsGranted(*locationPermissions)

    fun requestPermissions(handler: (PermissionsResultEvent) -> Unit) =
        PermissionManager.requestPermissions(*locationPermissions, handler = handler)
}

const val REQUEST_FOREGROUND_LOCATION_PERMISSIONS_REQUEST_CODE = 34
const val REQUEST_FOREGROUND_AND_BACKGROUND_LOCATION_PERMISSION_RESULT_CODE = 33

private val runningQ = android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.Q
