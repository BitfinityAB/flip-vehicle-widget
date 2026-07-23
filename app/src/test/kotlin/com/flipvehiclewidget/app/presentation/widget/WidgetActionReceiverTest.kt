package com.flipvehiclewidget.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.flipvehiclewidget.app.R
import com.flipvehiclewidget.app.domain.entity.VehicleCommand
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAppWidgetManager

@RunWith(RobolectricTestRunner::class)
class WidgetActionReceiverTest {
    init {
        VehicleWidgetProvider.updateDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val shadowManager: ShadowAppWidgetManager = shadowOf(appWidgetManager)
    private val appWidgetId = shadowManager.createWidget(VehicleWidgetProvider::class.java, R.layout.widget_layout)

    @Before
    fun setUp() {
        val config = Configuration.Builder().build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @After
    fun resetWidgetProviderDispatcher() {
        VehicleWidgetProvider.updateDispatcher = kotlinx.coroutines.Dispatchers.IO
    }

    @Test
    fun `enqueues a command worker and renders loading state`() {
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = WidgetActionReceiver.ACTION_EXECUTE_COMMAND
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(WidgetActionReceiver.EXTRA_COMMAND, VehicleCommand.TOGGLE_TRUNK.name)
        }

        WidgetActionReceiver().onReceive(context, intent)

        val workInfos = WorkManager.getInstance(context).getWorkInfosByTag(VehicleCommandWorker.WORK_TAG).get()
        assertEquals(1, workInfos.size)

        val view = shadowManager.getViewFor(appWidgetId)
        assertEquals(false, view.findViewById<android.view.View>(R.id.button_toggle_trunk).isEnabled)
    }

    @Test
    fun `a second tap for the same button replaces rather than stacks a second pending job`() {
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = WidgetActionReceiver.ACTION_EXECUTE_COMMAND
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(WidgetActionReceiver.EXTRA_COMMAND, VehicleCommand.TOGGLE_TRUNK.name)
        }

        WidgetActionReceiver().onReceive(context, intent)
        WidgetActionReceiver().onReceive(context, intent)

        val uniqueWorkInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("$appWidgetId-${VehicleCommand.TOGGLE_TRUNK.name}")
            .get()
        assertEquals(1, uniqueWorkInfos.size)
    }
}
