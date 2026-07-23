package com.flipvehiclewidget.app.presentation.widget

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class WidgetStaleRefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        VehicleWidgetProvider.requestRefresh(applicationContext)
        return Result.success()
    }
}
