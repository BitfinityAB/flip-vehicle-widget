package com.flipvehiclewidget.app.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

class WidgetConnectReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        VehicleWidgetProvider.render(
            context,
            AppWidgetManager.getInstance(context),
            appWidgetId,
            WidgetState.Disconnected(checking = true),
        )

        val request = OneTimeWorkRequestBuilder<WidgetConnectWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WidgetConnectWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
