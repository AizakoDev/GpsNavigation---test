package com.sergeyapp.gpsnavigation_test.presenetation.map

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.geometry.Point

fun AppCompatActivity.placemarkInfo(point: Point) {
    Toast.makeText(
        this,
        "Ширина: ${point.latitude} Долгота: ${point.longitude})",
        Toast.LENGTH_SHORT
    ).show()
}