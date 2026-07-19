package com.flipvehiclewidget.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.flipvehiclewidget.app.domain.entity.VehicleCommand

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_EXECUTE_COMMAND) return

        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val commandName = intent.getStringExtra(EXTRA_COMMAND) ?: return
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val command = runCatching { VehicleCommand.valueOf(commandName) }.getOrNull() ?: return

        VehicleWidgetProvider.render(
            context,
            AppWidgetManager.getInstance(context),
            appWidgetId,
            WidgetState.Connected(commandStates = mapOf(command to CommandButtonState.LOADING)),
        )

        // Expedited so the tap isn't blocked by Doze/App Standby (seen on-device as background
        // DNS failures). No backoff/retry: retrying a physical actuator command hours later
        // caused an unattended trunk/frunk open in production -- see VehicleCommandWorker.
        val request = OneTimeWorkRequestBuilder<VehicleCommandWorker>()
            .setInputData(
                workDataOf(
                    VehicleCommandWorker.KEY_COMMAND to command.name,
                    VehicleCommandWorker.KEY_APPWIDGET_ID to appWidgetId,
                ),
            )
            .addTag(VehicleCommandWorker.WORK_TAG)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        // Unique per (widget, command) so a fresh tap replaces any pending work, not stacks on it.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$appWidgetId-${command.name}",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        const val ACTION_EXECUTE_COMMAND = "com.flipvehiclewidget.app.action.EXECUTE_COMMAND"
        const val EXTRA_COMMAND = "com.flipvehiclewidget.app.extra.COMMAND"
    }
}
