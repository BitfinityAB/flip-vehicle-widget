package com.flipvehiclewidget.app.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.flipvehiclewidget.app.R
import com.flipvehiclewidget.app.domain.entity.ConnectionState
import com.flipvehiclewidget.app.domain.entity.VehicleCommand
import com.flipvehiclewidget.app.domain.usecase.CheckBluetoothConnectionUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VehicleWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun checkBluetoothConnectionUseCase(): CheckBluetoothConnectionUseCase
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult: PendingResult? = goAsync()
        val useCase = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            .checkBluetoothConnectionUseCase()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            renderAll(context, appWidgetManager, appWidgetIds, useCase)
            pendingResult?.finish()
        }
    }

    companion object {
        private val BUTTON_IDS: Map<VehicleCommand, Int> = mapOf(
            VehicleCommand.TOGGLE_TRUNK to R.id.button_toggle_trunk,
            VehicleCommand.TOGGLE_FRUNK to R.id.button_toggle_frunk,
            VehicleCommand.TOGGLE_CLIMATE to R.id.button_toggle_climate,
            VehicleCommand.TOGGLE_LOCKS to R.id.button_toggle_locks,
        )

        internal suspend fun renderAll(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            checkBluetoothConnectionUseCase: CheckBluetoothConnectionUseCase,
        ) {
            val connectionState = runCatching { checkBluetoothConnectionUseCase() }
                .getOrDefault(ConnectionState.DISCONNECTED)
            val state = if (connectionState == ConnectionState.CONNECTED) {
                WidgetState.Connected()
            } else {
                WidgetState.Disconnected
            }
            for (appWidgetId in appWidgetIds) {
                render(context, appWidgetManager, appWidgetId, state)
            }
        }

        fun render(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, state: WidgetState) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            when (state) {
                is WidgetState.Disconnected -> {
                    views.setViewVisibility(R.id.text_not_connected, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.connected_container, android.view.View.GONE)
                }

                is WidgetState.Connected -> {
                    views.setViewVisibility(R.id.text_not_connected, android.view.View.GONE)
                    views.setViewVisibility(R.id.connected_container, android.view.View.VISIBLE)

                    for ((command, viewId) in BUTTON_IDS) {
                        val buttonState = state.commandStates[command] ?: CommandButtonState.IDLE
                        views.setBoolean(viewId, "setEnabled", buttonState != CommandButtonState.LOADING)
                        views.setOnClickPendingIntent(viewId, buildCommandPendingIntent(context, appWidgetId, command))
                    }
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun buildCommandPendingIntent(context: Context, appWidgetId: Int, command: VehicleCommand): PendingIntent {
            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_EXECUTE_COMMAND
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(WidgetActionReceiver.EXTRA_COMMAND, command.name)
                data = android.net.Uri.parse("flipwidget://command/$appWidgetId/${command.name}")
            }
            return PendingIntent.getBroadcast(
                context,
                "$appWidgetId-${command.name}".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
