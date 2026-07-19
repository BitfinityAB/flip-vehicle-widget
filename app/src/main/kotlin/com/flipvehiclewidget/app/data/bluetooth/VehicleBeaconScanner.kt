package com.flipvehiclewidget.app.data.bluetooth

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.flipvehiclewidget.app.data.local.VehicleVinCache
import com.flipvehiclewidget.app.presentation.widget.VehicleBeaconReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// Registers a system-managed BLE scan (filters + PendingIntent) instead of an app-side scan
// loop: Android keeps this running in the Bluetooth stack and only wakes VehicleBeaconReceiver
// on a match/match-lost event, so detecting the car from a distance doesn't need a foreground
// scan or a continuously running process. Scan registrations don't survive a reboot (the
// Bluetooth stack resets), so VehicleBeaconBootReceiver re-registers on BOOT_COMPLETED.
class VehicleBeaconScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicleVinCache: VehicleVinCache,
) {
    fun startIfPossible() {
        val vin = vehicleVinCache.get() ?: return
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return
        val scanner = bluetoothLeScanner() ?: return

        val filter = ScanFilter.Builder().setDeviceName(TeslaBleBeaconName.forVin(vin)).build()
        val settings = ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH or ScanSettings.CALLBACK_TYPE_MATCH_LOST)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .build()

        runCatching { scanner.startScan(listOf(filter), settings, pendingIntent()) }
    }

    fun stop() {
        val scanner = bluetoothLeScanner() ?: return
        runCatching { scanner.stopScan(pendingIntent()) }
    }

    private fun bluetoothLeScanner(): BluetoothLeScanner? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.bluetoothLeScanner

    // Must be FLAG_MUTABLE: the system fills in EXTRA_CALLBACK_TYPE/EXTRA_LIST_SCAN_RESULT on
    // this intent when a scan result fires, which an immutable PendingIntent would reject.
    private fun pendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            SCAN_REQUEST_CODE,
            Intent(context, VehicleBeaconReceiver::class.java).setAction(VehicleBeaconReceiver.ACTION_BEACON_SCAN_RESULT),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val SCAN_REQUEST_CODE = 9100
    }
}
