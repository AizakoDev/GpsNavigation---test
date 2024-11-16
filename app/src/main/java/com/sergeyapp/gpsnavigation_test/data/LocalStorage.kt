package com.sergeyapp.gpsnavigation_test.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.gson.Gson

class LocalStorage() {

    private val PREFS_NAME = "placemark_prefs"
    private val KEY_PLACEMARKS = "placemarks"

    fun savePlacemarks(context: Context, placemarks: List<PlacemarkData>) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(placemarks)
        editor.putString(KEY_PLACEMARKS, json)
        editor.apply()
    }

    fun loadPlacemarks(context: Context): List<PlacemarkData> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val json = sharedPreferences.getString(KEY_PLACEMARKS, null)
        return if (!json.isNullOrEmpty()) {
            Gson().fromJson(json, Array<PlacemarkData>::class.java).toList()
        } else {
            emptyList()
        }
    }

    fun deletePlacemark(context: Context, placemark: PlacemarkData) {
        val placemarks = loadPlacemarks(context).toMutableList()
        placemarks.removeIf { it.latitude == placemark.latitude && it.longitude == placemark.longitude }
        savePlacemarks(context = context, placemarks)
    }

}