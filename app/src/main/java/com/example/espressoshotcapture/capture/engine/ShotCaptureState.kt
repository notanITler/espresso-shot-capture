package com.example.espressoshotcapture.capture.engine

enum class ShotCaptureState {
    DISCONNECTED,
    CONNECTED_IDLE,
    TARED,
    ARMED,
    RECORDING,
    TARGET_REACHED,
    SAVED,
    ERROR
}
