package com.sergeyapp.gpsnavigation_test.presenetation.gps

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.showLocationAccessDeniedDialog() {
    AlertDialog.Builder(this)
        .setTitle("Доступ к геолокации")
        .setMessage("Для корректной работы приложения необходимо разрешение на доступ к геолокации.")
        .setPositiveButton("Ок") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}