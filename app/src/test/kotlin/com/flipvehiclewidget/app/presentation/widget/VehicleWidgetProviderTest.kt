package com.flipvehiclewidget.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.flipvehiclewidget.app.R
import com.flipvehiclewidget.app.data.local.VehicleStatusStore
import com.flipvehiclewidget.app.domain.entity.Vehicle
import com.flipvehiclewidget.app.domain.entity.VehicleCommand
import com.flipvehiclewidget.app.domain.entity.VehicleStatus
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAppWidgetManager

@RunWith(RobolectricTestRunner::class)
class VehicleWidgetProviderTest {
    init {
        VehicleWidgetProvider.updateDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val shadowManager: ShadowAppWidgetManager = shadowOf(appWidgetManager)
    private val appWidgetId = shadowManager.createWidget(VehicleWidgetProvider::class.java, R.layout.widget_layout)

    private val vehicle = Vehicle(7L, "5YJ3E1EA1PF000001", "Car")

    @After
    fun resetWidgetProviderDispatcher() {
        VehicleWidgetProvider.updateDispatcher = kotlinx.coroutines.Dispatchers.IO
    }

    private fun repositoryReturning(status: VehicleStatus): VehicleRepository = mockk {
        coEvery { getVehicle() } returns Result.success(vehicle)
        coEvery { getVehicleStatus(vehicle) } returns Result.success(status)
    }

    private fun statusStore(lastKnown: VehicleStatus? = null): VehicleStatusStore = mockk(relaxed = true) {
        every { this@mockk.lastKnown() } returns lastKnown
    }

    @Test
    fun `disconnected state shows connect icon and hides vehicle buttons`() {
        VehicleWidgetProvider.render(context, appWidgetManager, appWidgetId, WidgetState.Disconnected())

        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.disconnected_container).visibility)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.connected_container).visibility)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.icon_connect).visibility)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.progress_connect).visibility)
        assertEquals(true, view.findViewById<android.view.View>(R.id.button_connect).isEnabled)
    }

    @Test
    fun `disconnected state while checking shows spinner and disables connect tile`() {
        VehicleWidgetProvider.render(context, appWidgetManager, appWidgetId, WidgetState.Disconnected(checking = true))

        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(false, view.findViewById<android.view.View>(R.id.button_connect).isEnabled)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.icon_connect).visibility)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.progress_connect).visibility)
    }

    @Test
    fun `connected state shows buttons and hides disconnected container`() {
        val state = WidgetState.Connected(
            commandStates = VehicleCommand.entries.associateWith { CommandButtonState.IDLE },
        )

        VehicleWidgetProvider.render(context, appWidgetManager, appWidgetId, state)

        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.disconnected_container).visibility)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.connected_container).visibility)
    }

    @Test
    fun `loading command shows spinner and disables the trunk tile`() {
        val state = WidgetState.Connected(
            commandStates = mapOf(VehicleCommand.TOGGLE_TRUNK to CommandButtonState.LOADING),
        )

        VehicleWidgetProvider.render(context, appWidgetManager, appWidgetId, state)

        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(false, view.findViewById<android.view.View>(R.id.button_toggle_trunk).isEnabled)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.icon_toggle_trunk).visibility)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.progress_toggle_trunk).visibility)
    }

    @Test
    fun `open trunk and unlocked doors render active icons`() {
        val status = VehicleStatus(locked = false, climateOn = false, frontTrunkOpen = false, rearTrunkOpen = true)
        val state = WidgetState.Connected(status = status)

        VehicleWidgetProvider.render(context, appWidgetManager, appWidgetId, state)

        val view = shadowManager.getViewFor(appWidgetId)
        assertIconResource(view, R.id.icon_toggle_trunk, R.drawable.ic_trunk_open)
        assertIconResource(view, R.id.icon_toggle_locks, R.drawable.ic_lock_open)
        assertIconResource(view, R.id.icon_toggle_frunk, R.drawable.ic_frunk_closed)
    }

    @Test
    fun `null status renders every tile in its inactive icon`() {
        val state = WidgetState.Connected(status = null)

        VehicleWidgetProvider.render(context, appWidgetManager, appWidgetId, state)

        val view = shadowManager.getViewFor(appWidgetId)
        assertIconResource(view, R.id.icon_toggle_locks, R.drawable.ic_lock)
        assertIconResource(view, R.id.icon_toggle_climate, R.drawable.ic_climate_off)
    }

    private fun assertIconResource(root: android.view.View, iconViewId: Int, expectedDrawableRes: Int) {
        val drawable = root.findViewById<android.widget.ImageView>(iconViewId).drawable
        assertEquals(expectedDrawableRes, org.robolectric.Shadows.shadowOf(drawable).createdFromResId)
    }

    @Test
    fun `renderAll shows connected state when bluetooth check reports connected`() = kotlinx.coroutines.test.runTest {
        val proximityStore = io.mockk.mockk<com.flipvehiclewidget.app.data.local.VehicleProximityStore> {
            io.mockk.every { isVehicleNearby() } returns true
        }
        val useCase = com.flipvehiclewidget.app.domain.usecase.CheckBluetoothConnectionUseCase(
            com.flipvehiclewidget.app.data.bluetooth.BluetoothConnectionManager(proximityStore),
        )
        val status = VehicleStatus(locked = true, climateOn = false, frontTrunkOpen = false, rearTrunkOpen = false)

        VehicleWidgetProvider.renderAll(
            context,
            appWidgetManager,
            intArrayOf(appWidgetId),
            useCase,
            { repositoryReturning(status) },
            { statusStore() },
        )

        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.connected_container).visibility)
    }

    @Test
    fun `renderAll never touches the vehicle repository when disconnected`() = kotlinx.coroutines.test.runTest {
        val proximityStore = io.mockk.mockk<com.flipvehiclewidget.app.data.local.VehicleProximityStore> {
            io.mockk.every { isVehicleNearby() } returns false
        }
        val useCase = com.flipvehiclewidget.app.domain.usecase.CheckBluetoothConnectionUseCase(
            com.flipvehiclewidget.app.data.bluetooth.BluetoothConnectionManager(proximityStore),
        )

        VehicleWidgetProvider.renderAll(
            context,
            appWidgetManager,
            intArrayOf(appWidgetId),
            useCase,
            { error("vehicleRepository must not be resolved while disconnected") },
            { error("vehicleStatusStore must not be resolved while disconnected") },
        )

        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.disconnected_container).visibility)
    }
}
