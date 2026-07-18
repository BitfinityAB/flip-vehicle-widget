package com.flipvehiclewidget.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.flipvehiclewidget.app.domain.entity.VehicleCommand
import java.util.concurrent.TimeUnit

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

        val request = OneTimeWorkRequestBuilder<VehicleCommandWorker>()
            .setInputData(
                workDataOf(
                    VehicleCommandWorker.KEY_COMMAND to command.name,
                    VehicleCommandWorker.KEY_APPWIDGET_ID to appWidgetId,
                ),
            )
            .addTag(VehicleCommandWorker.WORK_TAG)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        const val ACTION_EXECUTE_COMMAND = "com.flipvehiclewidget.app.action.EXECUTE_COMMAND"
        const val EXTRA_COMMAND = "com.flipvehiclewidget.app.extra.COMMAND"
    }
}
