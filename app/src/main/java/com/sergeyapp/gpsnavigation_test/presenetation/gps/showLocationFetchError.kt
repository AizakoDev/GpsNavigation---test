package com.sergeyapp.gpsnavigation_test.presenetation.gps

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Тост - местоположения не получено
fun AppCompatActivity.showLocationFetchError() {
    Toast.makeText(this, "Ошибка при получении координат", Toast.LENGTH_SHORT).show()
}