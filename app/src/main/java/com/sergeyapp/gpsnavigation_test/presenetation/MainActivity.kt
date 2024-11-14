package com.sergeyapp.gpsnavigation_test.presenetation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sergeyapp.gpsnavigation_test.R
import com.sergeyapp.gpsnavigation_test.databinding.ActivityMainBinding
import com.sergeyapp.gpsnavigation_test.presenetation.gps.showLocationAccessDeniedDialog
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.ScreenPoint
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.TextStyle
import com.yandex.runtime.image.ImageProvider

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

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

        // Инициализация карты
        MapKitFactory.initialize(this)
        // Инициализация FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()

        getGeoPermission()

        check()
    }

    // Запуск запроса разрешения
    fun getGeoPermission() {
        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getUserLocation()
            } else {
                // пользователь не дал разрешения
                this.showLocationAccessDeniedDialog()
            }
        }
    }

    // Проверяем разрешение и запрашиваем при необходимости
    private fun check(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getUserLocation()
            return true
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return false
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Запрос разрешений
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Код для получения местоположения, если разрешения предоставлены
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                userLatitude = it.latitude
                userLongitude = it.longitude
            } ?: run {
                // Обработка, если location равно null
            }
        }.addOnFailureListener {
            // Обработка ошибки получения местоположения
        }
    }

    private fun initView() = with(viewBinding) {

        val map = mapView.mapWindow.map

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

        // todo поинт по центру заменить на долгое нажатие
        viewBinding.btnCreatePoint.setOnClickListener {
            val centerX = mapView.mapWindow.width() / 2f
            val centerY = mapView.mapWindow.height() / 2f
            val centerPoint = ScreenPoint(centerX, centerY)

            val worldPoint = mapView.mapWindow.screenToWorld(centerPoint)
            map.mapObjects.addPlacemark().apply {
                geometry = worldPoint!!
                setIcon(ImageProvider.fromResource(this@MainActivity, R.drawable.placemark_icon))
            }
        }

        viewBinding.floatingActionButtonMoveUserPosition.setOnClickListener {
            if(check()) map.move(CameraPosition(Point(userLatitude, userLongitude), 17.0f, 150.0f, 30.0f))
            Toast.makeText(this@MainActivity, "${userLatitude} ${userLongitude}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        viewBinding.mapView.onStart()
    }

    override fun onStop() {
        viewBinding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

}