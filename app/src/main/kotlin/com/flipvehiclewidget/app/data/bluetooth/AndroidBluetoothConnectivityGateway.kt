package com.flipvehiclewidget.app.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

/**
 * Thin adapter over Android's classic-Bluetooth profile-proxy API. Not unit tested —
 * exercised manually on-device (see plan Global Constraints testing philosophy).
 */
class AndroidBluetoothConnectivityGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : BluetoothConnectivityGateway {

    override suspend fun connectedDeviceNames(): List<String> {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return emptyList()

        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: return emptyList()

        return suspendCancellableCoroutine { continuation ->
            adapter.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        val names = proxy.connectedDevices.mapNotNull { it.name }
                        adapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                        if (continuation.isActive) continuation.resume(names)
                    }

                    override fun onServiceDisconnected(profile: Int) = Unit
                },
                BluetoothProfile.A2DP,
            )
        }
    }
}
