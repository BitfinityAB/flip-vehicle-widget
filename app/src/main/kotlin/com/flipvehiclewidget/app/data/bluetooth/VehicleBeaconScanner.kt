package com.flipvehiclewidget.app.data.bluetooth

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.flipvehiclewidget.app.data.local.VehicleProximityStore
import com.flipvehiclewidget.app.data.local.VehicleVinCache
import com.flipvehiclewidget.app.presentation.widget.VehicleBeaconReceiver
import com.flipvehiclewidget.app.presentation.widget.VehicleWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

// No hardware ScanFilter -- name/service-UUID filters proved unreliable; matched in code instead.
// setLegacy(false): default excludes BLE 5 extended advertising, which the car uses.
class VehicleBeaconScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicleVinCache: VehicleVinCache,
    private val vehicleProximityStore: VehicleProximityStore,
) {
    fun startIfPossible() {
        vehicleVinCache.get() ?: return
        if (!hasScanPermission()) return
        val scanner = scannerOrNull() ?: return
        val settings = ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)
            .build()

        runCatching { scanner.startScan(listOf(), settings, pendingIntent()) }
    }

    fun stop() {
        val scanner = scannerOrNull() ?: return
        runCatching { scanner.stopScan(pendingIntent()) }
    }

    enum class ScanOutcome { FOUND, NOT_FOUND, CANNOT_SCAN }

    suspend fun scanOnceActive(timeoutMs: Long = 20_000): ScanOutcome {
        val vin = vehicleVinCache.get() ?: return ScanOutcome.CANNOT_SCAN
        if (!hasScanPermission()) return ScanOutcome.CANNOT_SCAN
        val scanner = scannerOrNull() ?: return ScanOutcome.CANNOT_SCAN
        val expectedName = TeslaBleBeaconName.forVin(vin)
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setLegacy(false).build()

        val found = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        if (continuation.isActive && result.scanRecord?.deviceName == expectedName) {
                            continuation.resume(true)
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
                continuation.invokeOnCancellation { runCatching { scanner.stopScan(callback) } }
                runCatching { scanner.startScan(listOf(), settings, callback) }
                    .onFailure { if (continuation.isActive) continuation.resume(false) }
            }
        } ?: false

        if (found) {
            vehicleProximityStore.markSeenNow()
            VehicleWidgetProvider.requestRefresh(context)
            VehicleWidgetProvider.scheduleStaleRefresh(context)
        }
        Timber.d("scanOnceActive: found=%s", found)
        return if (found) ScanOutcome.FOUND else ScanOutcome.NOT_FOUND
    }

    // Diagnostic: unfiltered scan, returns every named device seen nearby.
    suspend fun scanAllNearbyNames(timeoutMs: Long = 8_000): List<String> {
        val scanner = scannerOrNull() ?: return emptyList()
        if (!hasScanPermission()) return emptyList()

        val names = linkedSetOf<String>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                result.scanRecord?.deviceName?.let { names.add(it) }
            }
        }
        runCatching {
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setLegacy(false).build()
            scanner.startScan(listOf(), settings, callback)
        }
        delay(timeoutMs)
        runCatching { scanner.stopScan(callback) }
        Timber.d("scanAllNearbyNames found: %s", names)
        return names.toList()
    }

    private fun hasScanPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

    private fun scannerOrNull(): BluetoothLeScanner? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.bluetoothLeScanner

    // FLAG_MUTABLE required: system fills in scan-result extras on this intent.
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
