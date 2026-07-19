package com.flipvehiclewidget.app.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class TeslaBleBeaconNameTest {
    @Test
    fun `matches Tesla vehicle-command's VehicleLocalName algorithm`() {
        // Independently verified: `printf '5YJSA1E14FF101183' | shasum -a 1` =
        // d5313fc1b78c28f6ab4fe472d704c4a8aaaf2ce9 -- first 8 bytes hex-encoded is
        // d5313fc1b78c28f6, matching Go's `fmt.Sprintf("S%02xC", digest[:8])`.
        assertEquals("Sd5313fc1b78c28f6C", TeslaBleBeaconName.forVin("5YJSA1E14FF101183"))
    }

    @Test
    fun `different VINs produce different beacon names`() {
        assertEquals(false, TeslaBleBeaconName.forVin("VIN_A") == TeslaBleBeaconName.forVin("VIN_B"))
    }
}
