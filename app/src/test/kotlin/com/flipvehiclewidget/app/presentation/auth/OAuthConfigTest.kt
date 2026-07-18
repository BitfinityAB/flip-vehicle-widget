package com.flipvehiclewidget.app.presentation.auth

import net.openid.appauth.ResponseTypeValues
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OAuthConfigTest {
    @Test
    fun `authorization request targets Tesla auth endpoint with PKCE code flow`() {
        val request = OAuthConfig.buildAuthorizationRequest()

        assertEquals("https://auth.tesla.com/oauth2/v3/authorize", request.configuration.authorizationEndpoint.toString())
        assertEquals(ResponseTypeValues.CODE, request.responseType)
        assertEquals(OAuthConfig.SCOPE, request.scope)
    }
}
