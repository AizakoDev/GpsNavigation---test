package com.sergeyapp.gpsnavigation_test.presenetation.map

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.sergeyapp.gpsnavigation_test.data.LocalStorage
import com.sergeyapp.gpsnavigation_test.data.PlacemarkData
import com.sergeyapp.gpsnavigation_test.databinding.ActivityMapBinding
import com.sergeyapp.gpsnavigation_test.presenetation.gps.onLocationFetched
import com.sergeyapp.gpsnavigation_test.presenetation.gps.showLocationAccessDeniedDialog
import com.sergeyapp.gpsnavigation_test.presenetation.info.InfoMessages.COORDINATES_GETTING_FAILED
import com.sergeyapp.gpsnavigation_test.presenetation.info.InfoMessages.COORDINATES_HAVE_NOT_BEEN_RECEIVED
import com.sergeyapp.gpsnavigation_test.presenetation.info.showUserInfo
import com.sergeyapp.gpsnavigation_test.presenetation.info.InfoMessages.USE_LONG_TAP_TO_SCREEN
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.MapWindow
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.runtime.image.ImageProvider

class MapActivity : AppCompatActivity(),
    InputListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>

    private val localStorage = LocalStorage()

    private lateinit var mapWindow: MapWindow
    private lateinit var map: Map

    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    private val placemarks = mutableListOf<PlacemarkData>()

    private lateinit var viewBinding: ActivityMapBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewBinding = ActivityMapBinding.inflate(layoutInflater).also { setContentView(it.root) }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация карты
        MapKitFactory.initialize(this)

        mapWindow = viewBinding.myMapView.mapWindow
        map = mapWindow.map

        // Слушатель карты - от интерфейса InputListener
        map.addInputListener(this)

        loadAndDisplayPlacemarks()

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

        // центровка на позиции юзера
        viewBinding.floatingActionButtonMoveUserPosition.setOnClickListener {
            if(checkAndRequestPermission()) {
                runUserPosition()
            }
        }
    }

    private fun buildRouteToPlacemark(destination: Point) {
        // Местоположение юзера
        val userLocation = Point(userLatitude, userLongitude)
        // основная фабрика для маршрута, принимает тип работы офлайн/онлайн режим
        val drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)
        // Определяет параметры транспортного средства, по умолчанию выглядит как пеший
        val vehicleOptions = VehicleOptions()
        //  параметры построения маршрута
        val drivingOptions = DrivingOptions()
        // точки маршрута
        val points = buildList {
            add(RequestPoint(userLocation, RequestPointType.WAYPOINT, null, null))
            add(RequestPoint(destination, RequestPointType.WAYPOINT, null, null))
        }
        // механизм для получения результатов построения маршрута
        val drivingRouteListener = object : DrivingSession.DrivingRouteListener {
            override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
                // Отображение маршрута
                if (drivingRoutes.isNotEmpty()) {
                    val mapObjects = mapWindow.map.mapObjects
                    val route = drivingRoutes[0]
                    mapObjects.addPolyline(route.geometry)
                }
            }
            override fun onDrivingRoutesError(p0: com.yandex.runtime.Error) {}
        }

        // построения маршрута по двум точками + если нужно остановить процесс построения маршрута // drivingSession.cancel()
        val drivingSession = drivingRouter.requestRoutes(
            points,
            drivingOptions,
            vehicleOptions,
            drivingRouteListener
        )
    }

    // слушатель нажатий на метку вешается на созданную метку
    private val placemarkTapListener = MapObjectTapListener { mapObject, point ->
        // placemarkInfo - выводит координаты метки
        placemarkInfo(point)
        // Получение информации о метке
        val placemark = mapObject as? PlacemarkMapObject
        // Название метки
        val placemarkData = placemark?.userData as? PlacemarkData
        val placemarkTitle = placemarkData?.title ?: "Поинт без названия"
        // Показать диалог
        showPlacemarkDialog(placemark, placemarkTitle, Point(point.latitude, point.longitude))
        true
    }

    private fun loadAndDisplayPlacemarks() {
        val savedPlacemarks = localStorage.loadPlacemarks(this)
        placemarks.clear()
        placemarks.addAll(savedPlacemarks)
        savedPlacemarks.forEach { addPlacemarkToMap(it) }
    }

    private fun addPlacemarkToMap(placemarkData: PlacemarkData) {
        val placemark = map.mapObjects.addPlacemark(Point(placemarkData.latitude, placemarkData.longitude))
        placemark.setIcon(ImageProvider.fromResource(this, R.drawable.placemark_icon))
        // слушатель нажатий
        placemark.addTapListener(placemarkTapListener)
        // Привязываем данные метки к userData
        placemark.userData = placemarkData
    }

    // Короткий там по карте
    override fun onMapTap(map: Map, point: Point) {
        showUserInfo(USE_LONG_TAP_TO_SCREEN)
    }

    // Долгий тап по карте
    override fun onMapLongTap(map: Map, point: Point) {
        val latitude = point.latitude
        val longitude = point.longitude

        // Диалог для ввода названия метки
        val inputField = EditText(this).apply {
            hint = "Введите название метки"
        }
        AlertDialog.Builder(this)
            .setTitle("Добавить метку")
            .setView(inputField)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val title = inputField.text.toString().ifBlank { "Без названия" }

                // Создание новой метки
                val placemarkData = PlacemarkData(point.latitude, point.longitude, title)
                placemarks.add(placemarkData)
                localStorage.savePlacemarks(this, placemarks)

                // Добавление метки на карту
                val placemark = map.mapObjects.addPlacemark(Point(latitude, longitude))
                placemark.setIcon(ImageProvider.fromResource(this, R.drawable.placemark_icon))

                // Текст метки
                placemark.setText(title, TextStyle().apply {
                    size = 10f
                    placement = TextStyle.Placement.RIGHT
                    offset = 5f
                })

                // Listener метки
                placemark.addTapListener(placemarkTapListener)

                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // Диалог по клику на метку
    private fun showPlacemarkDialog(placemark: PlacemarkMapObject?, title: String, address: Point) {
        placemark?.let {
            val placemarkData = it.userData as? PlacemarkData
            AlertDialog.Builder(this)
                .setTitle("Метка: $title")
                .setMessage("Широта: ${address.latitude} Долгота: ${address.longitude}")
                .setPositiveButton("Построить маршрут") { _, _ ->
                    // Логика построения маршрута
                    buildRouteToPlacemark(it.geometry)
                }
                .setNegativeButton("Удалить") { _, _ ->
                    // Удаление метки
                    map.mapObjects.remove(it)
                    placemarkData?.let { data ->
                        localStorage.deletePlacemark(this, data)
                    }
                }
                .setNeutralButton("Отмена", null)
                .show()
        }
    }

    private fun runUserPosition() {
        map.move(CameraPosition(Point(userLatitude, userLongitude), 17.0f, 150.0f, 30.0f))
    }

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
                    // после получения координат перемещаемся на позицию юзера
                    runUserPosition()
                } else {
                    showUserInfo(COORDINATES_HAVE_NOT_BEEN_RECEIVED)
                }
            }
            .addOnFailureListener {
                showUserInfo(COORDINATES_GETTING_FAILED)
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