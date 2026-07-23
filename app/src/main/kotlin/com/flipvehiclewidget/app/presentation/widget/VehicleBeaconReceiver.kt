package com.flipvehiclewidget.app.presentation.widget

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.IntentCompat
import com.flipvehiclewidget.app.data.bluetooth.TeslaBleBeaconName
import com.flipvehiclewidget.app.data.local.VehicleProximityStore
import com.flipvehiclewidget.app.data.local.VehicleVinCache
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

// Reads results directly from the one continuously-running passive scan (started once in
// VehicleBeaconScanner.startIfPossible), rather than spawning a new bounded scan per broadcast:
// that repeatedly restarted scanning and very likely hit Android's undocumented BLE scan-rate
// limit, causing scans to fail instantly instead of actually running.
@AndroidEntryPoint
class VehicleBeaconReceiver : BroadcastReceiver() {

    @Inject
    lateinit var vehicleProximityStore: VehicleProximityStore

    @Inject
    lateinit var vehicleVinCache: VehicleVinCache

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_BEACON_SCAN_RESULT) return
        val vin = vehicleVinCache.get() ?: return
        val expectedName = TeslaBleBeaconName.forVin(vin)
        val results = IntentCompat.getParcelableArrayListExtra(
            intent,
            BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
            ScanResult::class.java,
        )
        if (results == null) {
            Timber.d("onReceive: EXTRA_LIST_SCAN_RESULT missing")
            return
        }

        val now = SystemClock.elapsedRealtimeNanos()
        Timber.d(
            "onReceive: %d results, names=%s, expected=%s",
            results.size,
            results.map { it.scanRecord?.deviceName to (now - it.timestampNanos) / 1_000_000 },
            expectedName,
        )
        val isFreshMatch = results.any {
            it.scanRecord?.deviceName == expectedName && now - it.timestampNanos <= MAX_RESULT_AGE_NANOS
        }
        if (isFreshMatch) {
            vehicleProximityStore.markSeenNow()
            VehicleWidgetProvider.requestRefresh(context)
            VehicleWidgetProvider.scheduleStaleRefresh(context)
        }
    }

    companion object {
        const val ACTION_BEACON_SCAN_RESULT = "com.flipvehiclewidget.app.action.BEACON_SCAN_RESULT"
        private const val MAX_RESULT_AGE_NANOS = 30_000_000_000L
    }
}
