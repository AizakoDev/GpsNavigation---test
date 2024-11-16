package com.sergeyapp.gpsnavigation_test.presenetation.map

import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.geometry.Point
import android.Manifest
import android.content.Context
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.sergeyapp.gpsnavigation_test.presenetation.info.InfoMessages.COORDINATES_GETTING_FAILED
import com.sergeyapp.gpsnavigation_test.presenetation.info.InfoMessages.COORDINATES_HAVE_NOT_BEEN_RECEIVED

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _userCoordinates = MutableLiveData<Point>()
    val userCoordinates: LiveData<Point> get() = _userCoordinates

    private val _locationError = MutableLiveData<String>()
    val locationError: LiveData<String> get() = _locationError

    fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission(launcher: ActivityResultLauncher<String>) {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun checkLocationSettings(
        onSuccess: () -> Unit,
        onFailure: (ResolvableApiException?) -> Unit
    ) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(getApplication())

        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    onFailure(exception)
                } else {
                    _locationError.value = "Настройки гео недоступны"
                }
            }
    }

    fun fetchUserCoordinates(context: Context) {
        if (!isPermissionGranted()) {
            _locationError.value = "Нет разрешения на гео"
            return
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    _userCoordinates.value = Point(location.latitude, location.longitude)
                } else {
                    _locationError.value = COORDINATES_HAVE_NOT_BEEN_RECEIVED.toString()
                }
            }
            .addOnFailureListener {
                _locationError.value = COORDINATES_GETTING_FAILED.toString()
            }
    }
}