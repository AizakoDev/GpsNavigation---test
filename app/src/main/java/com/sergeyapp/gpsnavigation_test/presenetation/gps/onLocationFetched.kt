package com.sergeyapp.gpsnavigation_test.presenetation.gps

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Тост -  успешного получения координат
fun AppCompatActivity.onLocationFetched(Latitude: Double?, Longitude: Double?) {
    Toast.makeText(this, "$Latitude, $Longitude", Toast.LENGTH_LONG).show()
}