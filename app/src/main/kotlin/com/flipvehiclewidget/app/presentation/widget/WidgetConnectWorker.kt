package com.flipvehiclewidget.app.presentation.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flipvehiclewidget.app.data.bluetooth.VehicleBeaconScanner
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetConnectWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val vehicleBeaconScanner: VehicleBeaconScanner,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        vehicleBeaconScanner.scanOnceActive()
        VehicleWidgetProvider.requestRefresh(applicationContext)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "widget_connect_scan"
    }
}
