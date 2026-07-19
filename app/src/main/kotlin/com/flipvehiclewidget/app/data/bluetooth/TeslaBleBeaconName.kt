package com.flipvehiclewidget.app.data.bluetooth

import java.security.MessageDigest

// Matches Tesla's own vehicle-command project (pkg/connector/ble/ble.go, VehicleLocalName):
// https://github.com/teslamotors/vehicle-command -- the BLE advertisement local name Tesla
// vehicles broadcast, and what Tesla's own app scans for to detect a nearby car before it's
// unlocked, paired, or even in Bluetooth-classic (A2DP) range: "S" + first 8 bytes of
// SHA-1(vin) as lowercase hex + "C".
object TeslaBleBeaconName {
    fun forVin(vin: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(vin.toByteArray(Charsets.UTF_8))
        val hex = digest.take(8).joinToString(separator = "") { byte -> "%02x".format(byte) }
        return "S${hex}C"
    }
}
