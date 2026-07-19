package com.flipvehiclewidget.app.data.local

import android.content.SharedPreferences
import com.flipvehiclewidget.app.di.AppPrefs
import javax.inject.Inject

// Written by VehicleBeaconReceiver whenever the system-managed BLE scan (see
// data/bluetooth/VehicleBeaconScanner) finds or loses the vehicle's beacon, read by
// BluetoothConnectionManager. Backed by SharedPreferences rather than an in-memory field
// because the receiver and any reader can run in different process lifetimes -- a BLE scan
// result can wake up a killed process.
class VehicleProximityStore @Inject constructor(
    @AppPrefs private val preferences: SharedPreferences,
) {
    fun isVehicleNearby(): Boolean = preferences.getBoolean(KEY_NEARBY, false)

    fun setVehicleNearby(nearby: Boolean) {
        preferences.edit().putBoolean(KEY_NEARBY, nearby).apply()
    }

    private companion object {
        const val KEY_NEARBY = "vehicle_beacon_nearby"
    }
}
