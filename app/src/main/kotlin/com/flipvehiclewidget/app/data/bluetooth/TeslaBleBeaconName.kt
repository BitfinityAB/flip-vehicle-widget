package com.flipvehiclewidget.app.data.bluetooth

import java.security.MessageDigest

// Matches teslamotors/vehicle-command (pkg/connector/ble/ble.go).
object TeslaBleBeaconName {
    fun forVin(vin: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(vin.toByteArray(Charsets.UTF_8))
        val hex = digest.take(8).joinToString(separator = "") { byte -> "%02x".format(byte) }
        return "S${hex}C"
    }
}
