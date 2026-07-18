package com.flipvehiclewidget.app.presentation.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class VehicleCommandWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.failure()

    companion object {
        const val KEY_COMMAND = "command"
        const val KEY_APPWIDGET_ID = "appWidgetId"
        const val WORK_TAG = "vehicle_command_worker"
    }
}
