package com.sergeyapp.gpsnavigation_test.presenetation.gps

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Тост - если координаты получить не удалось
fun AppCompatActivity.showLocationUnavailableMessage() {
    Toast.makeText(this, "Не удалось получить координаты", Toast.LENGTH_SHORT).show()
}