package com.example.espressoshotcapture

import android.content.Context
import androidx.room.Room
import com.example.espressoshotcapture.ble.AndroidDecentScaleGattClient
import com.example.espressoshotcapture.ble.AndroidBleScaleScanner
import com.example.espressoshotcapture.ble.BleScaleScanCandidate
import com.example.espressoshotcapture.ble.BleScaleScanner
import com.example.espressoshotcapture.ble.DecentScaleClient
import com.example.espressoshotcapture.ble.DecentScaleGattClient
import com.example.espressoshotcapture.capture.domain.FakeScaleClient
import com.example.espressoshotcapture.capture.domain.ScaleClient
import com.example.espressoshotcapture.persistence.EspressoShotDatabase
import com.example.espressoshotcapture.persistence.MIGRATION_1_2
import com.example.espressoshotcapture.repository.ShotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val selectedDecentScaleCandidateState =
        MutableStateFlow<BleScaleScanCandidate?>(null)

    val selectedDecentScaleCandidate: StateFlow<BleScaleScanCandidate?> =
        selectedDecentScaleCandidateState.asStateFlow()

    val database: EspressoShotDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            EspressoShotDatabase::class.java,
            "espresso-shot.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
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

    fun createDecentScaleClient(candidate: BleScaleScanCandidate): ScaleClient =
        DecentScaleClient(
            gattClient = decentScaleGattClient,
            candidate = candidate
        )

    fun selectDecentScaleCandidate(candidate: BleScaleScanCandidate) {
        selectedDecentScaleCandidateState.value = candidate
    }
}
