package com.example.espressoshotcapture

import android.content.Context
import androidx.room.Room
import com.example.espressoshotcapture.ble.AndroidDecentScaleGattClient
import com.example.espressoshotcapture.ble.AndroidBleScaleScanner
import com.example.espressoshotcapture.ble.BleScaleScanner
import com.example.espressoshotcapture.ble.DecentScaleGattClient
import com.example.espressoshotcapture.capture.domain.FakeScaleClient
import com.example.espressoshotcapture.capture.domain.ScaleClient
import com.example.espressoshotcapture.persistence.EspressoShotDatabase
import com.example.espressoshotcapture.repository.ShotRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: EspressoShotDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            EspressoShotDatabase::class.java,
            "espresso-shot.db"
        ).build()
    }

    val shotDao by lazy {
        database.shotDao()
    }

    val shotRepository: ShotRepository by lazy {
        ShotRepository(shotDao)
    }

    val scaleClient: ScaleClient by lazy {
        FakeScaleClient()
    }

    val bleScaleScanner: BleScaleScanner by lazy {
        AndroidBleScaleScanner(appContext)
    }

    val decentScaleGattClient: DecentScaleGattClient by lazy {
        AndroidDecentScaleGattClient(appContext)
    }
}
