package com.flipvehiclewidget.app.presentation.auth

import com.flipvehiclewidget.app.BuildConfig
import net.openid.appauth.ResponseTypeValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    @Test
    fun `token endpoint routes through our own proxy, not Tesla directly`() {
        val request = OAuthConfig.buildAuthorizationRequest()

        // Tesla's real token endpoint requires a client_secret that must never live in this
        // app -- token exchange/refresh must go through our oauth-relay instead.
        assertEquals(
            BuildConfig.PROXY_BASE_URL + "oauth/token",
            request.configuration.tokenEndpoint.toString(),
        )
        assertNotEquals("https://auth.tesla.com/oauth2/v3/token", request.configuration.tokenEndpoint.toString())
        assertNotEquals(
            "https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/token",
            request.configuration.tokenEndpoint.toString(),
        )
    }
}
