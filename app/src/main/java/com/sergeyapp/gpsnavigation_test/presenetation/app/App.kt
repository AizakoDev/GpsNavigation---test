package com.sergeyapp.gpsnavigation_test.presenetation.app

import android.app.Application
import com.sergeyapp.gpsnavigation_test.BuildConfig
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.navigation.internal.RoutePositionBinding

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        // init yandex map sdk
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
     }

}