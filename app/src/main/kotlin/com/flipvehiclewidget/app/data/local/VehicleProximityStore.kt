package com.flipvehiclewidget.app.data.local

import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.flipvehiclewidget.app.di.AppPrefs
import javax.inject.Inject

class VehicleProximityStore @Inject constructor(
    @AppPrefs private val preferences: SharedPreferences,
) {
    @VisibleForTesting
    internal var clockMillis: () -> Long = System::currentTimeMillis

    fun markSeenNow() {
        preferences.edit().putLong(KEY_LAST_SEEN_AT, clockMillis()).apply()
    }

    fun isVehicleNearby(): Boolean {
        val lastSeenAt = preferences.getLong(KEY_LAST_SEEN_AT, NEVER_SEEN)
        return lastSeenAt != NEVER_SEEN && clockMillis() - lastSeenAt <= FRESHNESS_WINDOW_MS
    }

    companion object {
        const val FRESHNESS_WINDOW_MS = 2 * 60 * 1000L
        private const val KEY_LAST_SEEN_AT = "vehicle_beacon_last_seen_at"
        private const val NEVER_SEEN = -1L
    }
}
