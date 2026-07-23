package com.flipvehiclewidget.app.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.VisibleForTesting
import com.flipvehiclewidget.app.R
import com.flipvehiclewidget.app.data.local.VehicleProximityStore
import com.flipvehiclewidget.app.data.local.VehicleStatusStore
import com.flipvehiclewidget.app.domain.entity.ConnectionState
import com.flipvehiclewidget.app.domain.entity.VehicleCommand
import com.flipvehiclewidget.app.domain.entity.VehicleStatus
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import com.flipvehiclewidget.app.domain.usecase.CheckBluetoothConnectionUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VehicleWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun checkBluetoothConnectionUseCase(): CheckBluetoothConnectionUseCase
        fun vehicleRepository(): VehicleRepository
        fun vehicleStatusStore(): VehicleStatusStore
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult: PendingResult? = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)

        CoroutineScope(SupervisorJob() + updateDispatcher).launch {
            renderAll(
                context,
                appWidgetManager,
                appWidgetIds,
                entryPoint.checkBluetoothConnectionUseCase(),
                entryPoint::vehicleRepository,
                entryPoint::vehicleStatusStore,
            )
            pendingResult?.finish()
        }
    }

    companion object {
        @VisibleForTesting
        internal var updateDispatcher: CoroutineDispatcher = Dispatchers.IO

        // vehicle_widget_info.xml sets updatePeriodMillis="0" (system-scheduled refresh is too
        // coarse -- 30 min minimum -- for "walk up to the car and tap a button"), so onUpdate
        // only ever fires once, at widget-add time, unless something explicitly triggers it.
        // Called by MainActivity on launch and by VehicleBeaconReceiver whenever the BLE
        // proximity signal changes.
        fun requestRefresh(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, VehicleWidgetProvider::class.java))
            if (appWidgetIds.isEmpty()) return
            context.sendBroadcast(
                Intent(context, VehicleWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                },
            )
        }

        // Nothing else triggers a refresh when the proximity freshness window silently expires
        // with no new sighting, so the widget would otherwise stay frozen showing "Connected".
        // Reschedules (REPLACE) on every fresh sighting so it always fires just after the last one.
        fun scheduleStaleRefresh(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetStaleRefreshWorker>()
                .setInitialDelay(VehicleProximityStore.FRESHNESS_WINDOW_MS + 10_000, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(STALE_REFRESH_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        private const val STALE_REFRESH_WORK_NAME = "widget_stale_refresh"

        private data class CommandTile(
            val containerId: Int,
            val iconId: Int,
            val progressId: Int,
            val activeIcon: Int,
            val inactiveIcon: Int,
            val isActive: (VehicleStatus) -> Boolean,
        )

        private val COMMAND_TILES: Map<VehicleCommand, CommandTile> = mapOf(
            VehicleCommand.TOGGLE_FRUNK to CommandTile(
                containerId = R.id.button_toggle_frunk,
                iconId = R.id.icon_toggle_frunk,
                progressId = R.id.progress_toggle_frunk,
                activeIcon = R.drawable.ic_frunk_open,
                inactiveIcon = R.drawable.ic_frunk_closed,
                isActive = { it.frontTrunkOpen },
            ),
            VehicleCommand.TOGGLE_TRUNK to CommandTile(
                containerId = R.id.button_toggle_trunk,
                iconId = R.id.icon_toggle_trunk,
                progressId = R.id.progress_toggle_trunk,
                activeIcon = R.drawable.ic_trunk_open,
                inactiveIcon = R.drawable.ic_trunk_closed,
                isActive = { it.rearTrunkOpen },
            ),
            VehicleCommand.TOGGLE_CLIMATE to CommandTile(
                containerId = R.id.button_toggle_climate,
                iconId = R.id.icon_toggle_climate,
                progressId = R.id.progress_toggle_climate,
                activeIcon = R.drawable.ic_climate,
                inactiveIcon = R.drawable.ic_climate_off,
                isActive = { it.climateOn },
            ),
            VehicleCommand.TOGGLE_LOCKS to CommandTile(
                containerId = R.id.button_toggle_locks,
                iconId = R.id.icon_toggle_locks,
                progressId = R.id.progress_toggle_locks,
                activeIcon = R.drawable.ic_lock_open,
                inactiveIcon = R.drawable.ic_lock,
                isActive = { !it.locked },
            ),
        )

        internal suspend fun renderAll(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            checkBluetoothConnectionUseCase: CheckBluetoothConnectionUseCase,
            // Suppliers, not instances: VehicleRepository pulls in the authenticated network
            // client (TokenManager -> EncryptedSharedPreferences -> real AndroidKeyStore), which
            // must not be constructed at all when disconnected -- both to avoid needless work on
            // every refresh and because Robolectric's widget tests can't provide a real KeyStore.
            vehicleRepository: () -> VehicleRepository,
            vehicleStatusStore: () -> VehicleStatusStore,
        ) {
            val connectionState = runCatching { checkBluetoothConnectionUseCase() }
                .getOrDefault(ConnectionState.DISCONNECTED)
            val state = if (connectionState == ConnectionState.CONNECTED) {
                WidgetState.Connected(status = fetchAndCacheVehicleStatus(vehicleRepository(), vehicleStatusStore()))
            } else {
                WidgetState.Disconnected()
            }
            for (appWidgetId in appWidgetIds) {
                render(context, appWidgetManager, appWidgetId, state)
            }
        }

        // Falls back to the last successfully fetched status (rather than null/unknown) so a
        // transient vehicle_data failure doesn't reset every icon to its default appearance.
        private suspend fun fetchAndCacheVehicleStatus(
            vehicleRepository: VehicleRepository,
            vehicleStatusStore: VehicleStatusStore,
        ): VehicleStatus? {
            val vehicle = vehicleRepository.getVehicle().getOrNull() ?: return vehicleStatusStore.lastKnown()
            val status = vehicleRepository.getVehicleStatus(vehicle).getOrNull() ?: return vehicleStatusStore.lastKnown()
            vehicleStatusStore.save(status)
            return status
        }

        fun render(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, state: WidgetState) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            when (state) {
                is WidgetState.Disconnected -> {
                    views.setViewVisibility(R.id.disconnected_container, View.VISIBLE)
                    views.setViewVisibility(R.id.connected_container, View.GONE)
                    views.setViewVisibility(R.id.icon_connect, if (state.checking) View.GONE else View.VISIBLE)
                    views.setViewVisibility(R.id.progress_connect, if (state.checking) View.VISIBLE else View.GONE)
                    views.setBoolean(R.id.button_connect, "setEnabled", !state.checking)
                    views.setOnClickPendingIntent(R.id.button_connect, buildConnectPendingIntent(context, appWidgetId))
                }

                is WidgetState.Connected -> {
                    views.setViewVisibility(R.id.disconnected_container, View.GONE)
                    views.setViewVisibility(R.id.connected_container, View.VISIBLE)

                    for ((command, tile) in COMMAND_TILES) {
                        val buttonState = state.commandStates[command] ?: CommandButtonState.IDLE
                        val loading = buttonState == CommandButtonState.LOADING
                        val isActive = state.status?.let(tile.isActive) ?: false

                        views.setViewVisibility(tile.iconId, if (loading) View.GONE else View.VISIBLE)
                        views.setViewVisibility(tile.progressId, if (loading) View.VISIBLE else View.GONE)
                        views.setImageViewResource(tile.iconId, if (isActive) tile.activeIcon else tile.inactiveIcon)
                        views.setInt(
                            tile.containerId,
                            "setBackgroundResource",
                            if (isActive) R.drawable.widget_tile_active else R.drawable.widget_tile_inactive,
                        )
                        views.setBoolean(tile.containerId, "setEnabled", !loading)
                        views.setOnClickPendingIntent(tile.containerId, buildCommandPendingIntent(context, appWidgetId, command))
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

        private fun buildConnectPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, WidgetConnectReceiver::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = android.net.Uri.parse("flipwidget://connect/$appWidgetId")
            }
            return PendingIntent.getBroadcast(
                context,
                "$appWidgetId-connect".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
