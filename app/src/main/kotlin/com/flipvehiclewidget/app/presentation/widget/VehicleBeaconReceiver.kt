package com.flipvehiclewidget.app.presentation.widget

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flipvehiclewidget.app.data.local.VehicleProximityStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Fired by the system (see data/bluetooth/VehicleBeaconScanner) when the vehicle's BLE beacon
// is first seen or lost, independent of whether the app process is currently running.
@AndroidEntryPoint
class VehicleBeaconReceiver : BroadcastReceiver() {

    @Inject
    lateinit var vehicleProximityStore: VehicleProximityStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_BEACON_SCAN_RESULT) return
        val callbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1)
        vehicleProximityStore.setVehicleNearby(callbackType != ScanSettings.CALLBACK_TYPE_MATCH_LOST)
        VehicleWidgetProvider.requestRefresh(context)
    }

    companion object {
        const val ACTION_BEACON_SCAN_RESULT = "com.flipvehiclewidget.app.action.BEACON_SCAN_RESULT"
    }
}
