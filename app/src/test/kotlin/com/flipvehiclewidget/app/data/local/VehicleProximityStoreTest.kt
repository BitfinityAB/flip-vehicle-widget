package com.flipvehiclewidget.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VehicleProximityStoreTest {
    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
    private val store = VehicleProximityStore(prefs)

    @Test
    fun `not nearby before any sighting`() {
        assertFalse(store.isVehicleNearby())
    }

    @Test
    fun `nearby right after a sighting`() {
        store.clockMillis = { 1_000L }
        store.markSeenNow()

        assertTrue(store.isVehicleNearby())
    }

    @Test
    fun `still nearby just inside the freshness window`() {
        store.clockMillis = { 1_000L }
        store.markSeenNow()

        store.clockMillis = { 1_000L + 2 * 60_000L - 1 }
        assertTrue(store.isVehicleNearby())
    }

    @Test
    fun `not nearby once the freshness window has elapsed`() {
        store.clockMillis = { 1_000L }
        store.markSeenNow()

        store.clockMillis = { 1_000L + 2 * 60_000L + 1 }
        assertFalse(store.isVehicleNearby())
    }
}
