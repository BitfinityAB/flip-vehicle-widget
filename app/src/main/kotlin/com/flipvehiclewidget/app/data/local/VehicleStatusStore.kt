package com.flipvehiclewidget.app.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import com.flipvehiclewidget.app.di.AppPrefs
import com.flipvehiclewidget.app.domain.entity.VehicleStatus
import javax.inject.Inject

// Partial renders (a single command's LOADING/ERROR state) don't re-fetch full vehicle status,
// so they fall back to the last successfully fetched snapshot here rather than showing the
// other 3 command icons as an unknown/default state.
class VehicleStatusStore @Inject constructor(
    @AppPrefs private val preferences: SharedPreferences,
) {
    fun save(status: VehicleStatus) {
        preferences.edit {
            putBoolean(KEY_LOCKED, status.locked)
            putBoolean(KEY_CLIMATE_ON, status.climateOn)
            putBoolean(KEY_FRONT_TRUNK_OPEN, status.frontTrunkOpen)
            putBoolean(KEY_REAR_TRUNK_OPEN, status.rearTrunkOpen)
        }
    }

    fun lastKnown(): VehicleStatus? {
        if (!preferences.contains(KEY_LOCKED)) return null
        return VehicleStatus(
            locked = preferences.getBoolean(KEY_LOCKED, true),
            climateOn = preferences.getBoolean(KEY_CLIMATE_ON, false),
            frontTrunkOpen = preferences.getBoolean(KEY_FRONT_TRUNK_OPEN, false),
            rearTrunkOpen = preferences.getBoolean(KEY_REAR_TRUNK_OPEN, false),
        )
    }

    companion object {
        private const val KEY_LOCKED = "vehicle_status_locked"
        private const val KEY_CLIMATE_ON = "vehicle_status_climate_on"
        private const val KEY_FRONT_TRUNK_OPEN = "vehicle_status_front_trunk_open"
        private const val KEY_REAR_TRUNK_OPEN = "vehicle_status_rear_trunk_open"
    }
}
