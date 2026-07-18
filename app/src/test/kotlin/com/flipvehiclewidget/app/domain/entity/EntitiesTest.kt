package com.flipvehiclewidget.app.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class EntitiesTest {
    @Test
    fun `vehicle entity holds provided fields`() {
        val vehicle = Vehicle(id = 42L, vin = "5YJ3E1EA1PF000001", displayName = "Aziz's Model 3")
        assertEquals(42L, vehicle.id)
        assertEquals("5YJ3E1EA1PF000001", vehicle.vin)
        assertEquals("Aziz's Model 3", vehicle.displayName)
    }

    @Test
    fun `command result holds success and reason`() {
        val failure = CommandResult(success = false, reason = "vehicle_unavailable")
        assertEquals(false, failure.success)
        assertEquals("vehicle_unavailable", failure.reason)
    }

    @Test
    fun `vehicle command has exactly four entries`() {
        assertEquals(4, VehicleCommand.entries.size)
    }
}
