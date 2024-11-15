package com.sergeyapp.gpsnavigation_test.presenetation.gps

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// Диалог покажет юзеру необходимость дать разрешения
fun AppCompatActivity.showLocationAccessDeniedDialog() {
    AlertDialog.Builder(this)
        .setTitle("Доступ к местоположению")
        .setMessage("Чтобы использовать эту функцию, предоставьте доступ к местоположению")
        .setPositiveButton("ОК") { _, _ -> }
        .show()
}