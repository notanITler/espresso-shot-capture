package com.example.espressoshotcapture

import android.app.Application

class EspressoShotCaptureApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}
