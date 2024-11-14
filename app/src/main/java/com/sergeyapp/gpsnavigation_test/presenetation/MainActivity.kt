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
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.ScreenPoint
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.TextStyle
import com.yandex.runtime.image.ImageProvider

class MainActivity : AppCompatActivity() {

    // fixme del
    companion object {
        private val POINT = Point(55.751280, 37.629720)
        private val CAMERA_POSITION = CameraPosition(POINT, 17.0f, 150.0f, 30.0f)
    }

    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

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

        MapKitFactory.initialize(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()

        // Инициализация FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Запуск запроса разрешения
        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getUserLocation()
            } else {
                showLocationAccessDeniedDialog()
            }
        }

        // Проверяем разрешение и запрашиваем при необходимости
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getUserLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
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
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                // присваимваем координаты юзера
                userLatitude = it.latitude
                userLongitude = it.longitude
            } ?: run {
                // Обработка случая, когда location равно null
            }
        }.addOnFailureListener {
            // Обработка ошибки получения местоположения
        }
    }

    private fun showLocationAccessDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Доступ к геолокации")
            .setMessage("Для корректной работы приложения необходимо разрешение на доступ к геолокации.")
            .setPositiveButton("Ок") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun initView() = with(viewBinding) {

        val map = mapView.mapWindow.map

        // Чтобы изменить положение или масштаб карты, используйте метод Map.move
        // Map.move принимает на вход аргумент CameraPosition,
        // который полностью задает положение, масштаб, наклон и азимут карты.

        val pinsCollection = map.mapObjects.addCollection()

        val points = listOf(
            Point(59.936046, 30.326869),
            Point(59.938185, 30.32808),
            Point(59.937376, 30.33621),
            Point(59.934517, 30.335059),
        )

        // тест линии между метками листа
        val polyline = Polyline(points)

        val polylineObject = map.mapObjects.addPolyline(polyline)
        polylineObject.apply {
            strokeWidth = 5f
            setStrokeColor(ContextCompat.getColor(this@MainActivity, R.color.black))
            outlineWidth = 1f
            outlineColor = ContextCompat.getColor(this@MainActivity, R.color.black)
        }

        points.forEach { point ->
            pinsCollection.addPlacemark().apply {
                geometry = point
                setIcon(imageProvider)
                setText("Широта: ${point.latitude}\n Долгота: ${point.longitude}", TextStyle().apply {
                    size = 10f
                    placement = TextStyle.Placement.RIGHT
                    offset = 5f
                },)
            }
        }

        val placemarkObject = map.mapObjects.addPlacemark().apply {
            geometry = POINT
            setIcon(imageProvider)

            // настройки текста для метки
            setText("Test icon", TextStyle().apply {
                size = 10f
                placement = TextStyle.Placement.RIGHT
                offset = 5f
            },)
        }

        placemarkObject.addTapListener(placemarkTapListener)

        // Test
        var pointNumber = 0
        viewBinding.btnNextPoint.setOnClickListener {
            map.move(CameraPosition(points[pointNumber], 17.0f, 150.0f, 30.0f))
            pointNumber++
            if (pointNumber == points.size -1) pointNumber = 0
        }

        viewBinding.btnCreatePoint.setOnClickListener {
            val centerX = mapView.mapWindow.width() / 2f
            val centerY = mapView.mapWindow.height() / 2f
            val centerPoint = ScreenPoint(centerX, centerY)
            // For example, worldPoint = (59.935493, 30.327392)
            val worldPoint = mapView.mapWindow.screenToWorld(centerPoint)
            map.mapObjects.addPlacemark().apply {
                geometry = worldPoint!!
                setIcon(ImageProvider.fromResource(this@MainActivity, R.drawable.placemark_icon))
            }
        }

        viewBinding.btnMoveUserPosition.setOnClickListener {
            map.move(CameraPosition(Point(userLatitude, userLongitude), 17.0f, 150.0f, 30.0f))
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