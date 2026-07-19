package com.flipvehiclewidget.app.presentation.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flipvehiclewidget.app.data.bluetooth.VehicleBeaconScanner
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// BLE scan registrations don't survive a reboot (the Bluetooth stack resets), so re-register
// the vehicle beacon scan here. See data/bluetooth/VehicleBeaconScanner for why this is a
// system-managed scan rather than an app-side loop.
@AndroidEntryPoint
class VehicleBeaconBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var vehicleBeaconScanner: VehicleBeaconScanner

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        vehicleBeaconScanner.startIfPossible()
    }
}
