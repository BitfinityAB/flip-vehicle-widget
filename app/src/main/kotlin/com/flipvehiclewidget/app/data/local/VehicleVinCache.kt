package com.flipvehiclewidget.app.data.local

import android.content.SharedPreferences
import com.flipvehiclewidget.app.di.AppPrefs
import javax.inject.Inject

// The vehicle's VIN is needed to compute its BLE beacon name (see
// data/bluetooth/TeslaBleBeaconName) before a scan can even start, so it's cached the first
// time VehicleRepositoryImpl.getVehicle() succeeds rather than re-fetched on every check.
class VehicleVinCache @Inject constructor(
    @AppPrefs private val preferences: SharedPreferences,
) {
    fun get(): String? = preferences.getString(KEY_VIN, null)

    fun save(vin: String) {
        preferences.edit().putString(KEY_VIN, vin).apply()
    }

    private companion object {
        const val KEY_VIN = "vehicle_vin"
    }
}
