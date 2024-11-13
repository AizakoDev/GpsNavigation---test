package com.sergeyapp.gpsnavigation_test

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sergeyapp.gpsnavigation_test.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.runtime.image.ImageProvider

class MainActivity : AppCompatActivity() {

    companion object {
        private val POINT = Point(55.751280, 37.629720)
        private val CAMERA_POSITION = CameraPosition(POINT, 17.0f, 150.0f, 30.0f)
    }

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
    }

    private fun initView() = with(viewBinding) {

        val map = mapView.mapWindow.map

        // Чтобы изменить положение или масштаб карты, используйте метод Map.move
        // Map.move принимает на вход аргумент CameraPosition,
        // который полностью задает положение, масштаб, наклон и азимут карты.
        map.move(CAMERA_POSITION)

        val placemarkObject = map.mapObjects.addPlacemark().apply {
            geometry = POINT
            setIcon(imageProvider)
        }

        placemarkObject.addTapListener(placemarkTapListener)
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