package com.flipvehiclewidget.app.data.local

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.openid.appauth.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TokenManagerTest {
    @Test
    fun `isAuthenticated is false when nothing saved`() {
        val preferences = mockk<SharedPreferences>()
        every { preferences.getString("auth_state", null) } returns null

        val tokenManager = TokenManager(preferences, authService = mockk())

        assertFalse(tokenManager.isAuthenticated())
    }

    @Test
    fun `saveAuthState writes serialized json under auth_state key`() {
        val preferences = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        val savedJson = slot<String>()
        every { preferences.edit() } returns editor
        every { editor.putString("auth_state", capture(savedJson)) } returns editor
        every { editor.apply() } returns Unit

        val tokenManager = TokenManager(preferences, authService = mockk())
        tokenManager.saveAuthState(AuthState())

        verify { editor.putString("auth_state", any()) }
        assertEquals(AuthState().jsonSerializeString(), savedJson.captured)
    }
}
