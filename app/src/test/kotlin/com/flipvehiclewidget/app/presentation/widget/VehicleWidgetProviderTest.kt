package com.flipvehiclewidget.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.flipvehiclewidget.app.R
import com.flipvehiclewidget.app.domain.entity.VehicleCommand
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAppWidgetManager

@RunWith(RobolectricTestRunner::class)
class VehicleWidgetProviderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val shadowManager: ShadowAppWidgetManager = shadowOf(appWidgetManager)
    private val appWidgetId = shadowManager.createWidget(VehicleWidgetProvider::class.java, R.layout.widget_layout)

    @Test
    fun `disconnected state shows not-connected text and hides buttons`() {
        VehicleWidgetProvider.render(context, appWidgetManager, appWidgetId, WidgetState.Disconnected)

        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.text_not_connected).visibility)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.connected_container).visibility)
    }

    @Test
    fun `connected state shows buttons and hides not-connected text`() {
        val state = WidgetState.Connected(
            commandStates = VehicleCommand.entries.associateWith { CommandButtonState.IDLE },
        )

        VehicleWidgetProvider.render(context, appWidgetManager, appWidgetId, state)

        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(android.view.View.GONE, view.findViewById<android.view.View>(R.id.text_not_connected).visibility)
        assertEquals(android.view.View.VISIBLE, view.findViewById<android.view.View>(R.id.connected_container).visibility)
    }

    @Test
    fun `loading command shows disabled trunk button`() {
        val state = WidgetState.Connected(
            commandStates = mapOf(VehicleCommand.TOGGLE_TRUNK to CommandButtonState.LOADING),
        )

        VehicleWidgetProvider.render(context, appWidgetManager, appWidgetId, state)

        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(false, view.findViewById<android.widget.Button>(R.id.button_toggle_trunk).isEnabled)
    }
}
