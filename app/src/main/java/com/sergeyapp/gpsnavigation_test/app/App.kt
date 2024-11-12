package com.sergeyapp.gpsnavigation_test.app

import android.app.Application
import com.sergeyapp.gpsnavigation_test.BuildConfig
import com.yandex.mapkit.MapKitFactory

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
     }

}