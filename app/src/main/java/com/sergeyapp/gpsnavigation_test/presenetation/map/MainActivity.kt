package com.sergeyapp.gpsnavigation_test.presenetation.map

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.sergeyapp.gpsnavigation_test.R
import com.sergeyapp.gpsnavigation_test.databinding.ActivityMainBinding
import com.sergeyapp.gpsnavigation_test.presenetation.gps.onLocationFetched
import com.sergeyapp.gpsnavigation_test.presenetation.gps.showLocationAccessDeniedDialog
import com.sergeyapp.gpsnavigation_test.presenetation.gps.showLocationFetchError
import com.sergeyapp.gpsnavigation_test.presenetation.gps.showLocationUnavailableMessage
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.MapWindow
import com.yandex.mapkit.map.TextStyle
import com.yandex.runtime.image.ImageProvider

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var mapWindow: MapWindow
    private lateinit var map: Map

    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    // fixme тестовый список для меток
    val points = listOf(
        Point(59.936046, 30.326869),
        Point(59.938185, 30.32808),
        Point(59.937376, 30.33621),
        Point(59.934517, 30.335059),
    )

    // иконка метки
    val imageProvider by lazy { ImageProvider.fromResource(this, R.drawable.placemark_icon) }

    // слушатель нажатий на метку вешается на созданную метку
    private val placemarkTapListener = MapObjectTapListener { _, point ->
        Toast.makeText(
            this@MainActivity,
            "Координаты метки (${point.longitude}, ${point.latitude})",
            Toast.LENGTH_SHORT
        ).show()
        true
    }

    private lateinit var viewBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewBinding = ActivityMainBinding.inflate(layoutInflater).also { setContentView(it.root) }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация карты
        MapKitFactory.initialize(this)

        mapWindow = viewBinding.myMapView.mapWindow
        map = mapWindow.map

        // Инициализация FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Инициализация обработчиков
        initPermissionLauncher()
        initSettingsLauncher()

        // Проверка разрешения и вызов fetchUserCoordinates, если все настроено
        if (isPermissionGranted()) {
            fetchUserCoordinates()
        } else {
            checkAndRequestPermission()
        }

        initView()
    }

    // fixme initView()
    private fun initView() = with(viewBinding) {

        val pinsCollection = map.mapObjects.addCollection()

        points.forEach { point ->
            pinsCollection.addPlacemark().apply {
                geometry = point
                setIcon(imageProvider)
                setText("Широта: ${point.latitude}\n Долгота: ${point.longitude}", TextStyle().apply {
                    size = 10f
                    placement = TextStyle.Placement.RIGHT
                    offset = 5f
                },)
                addTapListener(placemarkTapListener)
            }
        }

        viewBinding.floatingActionButtonMoveUserPosition.setOnClickListener {
            if(checkAndRequestPermission()) {
                runUserPosition()
            }
        }
    }

    private fun runUserPosition() {
        map.move(CameraPosition(Point(userLatitude, userLongitude), 17.0f, 150.0f, 30.0f))
    }

//    private fun placeMarketGenerate() {
//        val centerX = mapView.mapWindow.width() / 2f
//        val centerY = mapView.mapWindow.height() / 2f
//        val centerPoint = ScreenPoint(centerX, centerY)
//
//        val worldPoint = mapView.mapWindow.screenToWorld(centerPoint)
//        map.mapObjects.addPlacemark().apply {
//            geometry = worldPoint!!
//            setIcon(ImageProvider.fromResource(this@MainActivity, R.drawable.placemark_icon))
//        }
//    }

    // Инициализация обработчика разрешений
    private fun initPermissionLauncher() {
    locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Проверяем настройки после получения разрешения
            checkLocationSettings()
        } else {
            showLocationAccessDeniedDialog()
        }
    }
}

    // Инициализация обработчика настроек геолокации
    private fun initSettingsLauncher() {
        locationSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                fetchUserCoordinates()
            } else {
                showLocationAccessDeniedDialog()
            }
        }
    }

    // Проверка и запрос разрешения
    private fun checkAndRequestPermission(): Boolean {
        if (isPermissionGranted()) {
            checkLocationSettings()
            return true
        } else {
            requestLocationPermission()
            return false
        }
    }

    // Проверяет, предоставлено ли разрешение
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Запрашивает разрешение
    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Проверяет настройки геолокации
    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this)

        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                fetchUserCoordinates()
            }
            .addOnFailureListener { exception ->
                handleLocationSettingsFailure(exception)
            }
    }

    // Обрабатывает ошибку при проверке настроек геолокации
    private fun handleLocationSettingsFailure(exception: Exception) {
        if (exception is ResolvableApiException) {
            try {
                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                locationSettingsLauncher.launch(intentSenderRequest)
            } catch (sendEx: IntentSender.SendIntentException) {
                // Ошибка отправки Intent
            }
        } else {
            showLocationAccessDeniedDialog()
        }
    }

    // Получение координат пользователя
    private fun fetchUserCoordinates() {
        if (!isPermissionGranted()) {
            requestLocationPermission()
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    onLocationFetched(userLatitude, userLongitude)
                    // fixme
                    // после получения координат перемещаемся на позицию юзера
                    runUserPosition()
                } else {
                    showLocationUnavailableMessage()
                }
            }
            .addOnFailureListener {
                showLocationFetchError()
            }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        viewBinding.myMapView.onStart()
    }

    override fun onStop() {
        viewBinding.myMapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

}