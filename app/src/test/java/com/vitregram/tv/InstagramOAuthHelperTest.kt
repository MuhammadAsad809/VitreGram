package com.vitregram.tv

import com.vitregram.tv.data.remote.oauth.InstagramOAuthHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Instagram OAuth 2.0 helper logic ported from xiandanin/instagram-login-android.
 */
class InstagramOAuthHelperTest {

    private val testClientId = "test_client_id_123"
    private val testClientSecret = "test_client_secret_abc"
    private val testRedirectUri = "https://www.instagram.com/"

    @Test
    fun testGetRedirectOAuthUrl() {
        val url = InstagramOAuthHelper.getRedirectOAuthUrl(testClientId, testRedirectUri)

        assertTrue(url.startsWith(InstagramOAuthHelper.URL_OAUTH))
        assertTrue(url.contains("client_id=$testClientId"))
        assertTrue(url.contains("redirect_uri=$testRedirectUri"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("scope=user_profile,user_media"))
    }

    @Test
    fun testParseCodeFromUrlStandard() {
        val redirectUrl = "https://www.instagram.com/?code=AQD123456789xyz"
        val code = InstagramOAuthHelper.parseCodeFromUrl(redirectUrl, testRedirectUri)

        assertNotNull(code)
        assertEquals("AQD123456789xyz", code)
    }

    @Test
    fun testParseCodeFromUrlWithTrailingHashFragment() {
        // Instagram OAuth frequently appends '#_' to the redirect code
        val redirectUrl = "https://www.instagram.com/?code=AQD123456789xyz#_"
        val code = InstagramOAuthHelper.parseCodeFromUrl(redirectUrl, testRedirectUri)

        assertNotNull(code)
        assertEquals("AQD123456789xyz", code)
    }

    @Test
    fun testParseCodeFromUnrelatedUrlReturnsNull() {
        val unrelatedUrl = "https://www.google.com/?search=instagram"
        val code = InstagramOAuthHelper.parseCodeFromUrl(unrelatedUrl, testRedirectUri)

        assertNull(code)
    }

    @Test
    fun testQueryStringToParams() {
        val queryString = "client_id=123&response_type=code&state=secure_state"
        val params = InstagramOAuthHelper.queryStringToParams(queryString)

        assertEquals("123", params["client_id"])
        assertEquals("code", params["response_type"])
        assertEquals("secure_state", params["state"])
    }

    @Test
    fun testBuildAccessTokenParams() {
        val code = "test_auth_code_999"
        val params = InstagramOAuthHelper.buildAccessTokenParams(
            clientId = testClientId,
            clientSecret = testClientSecret,
            redirectUri = testRedirectUri,
            code = code
        )

        assertEquals(testClientId, params["client_id"])
        assertEquals(testClientSecret, params["client_secret"])
        assertEquals("authorization_code", params["grant_type"])
        assertEquals(testRedirectUri, params["redirect_uri"])
        assertEquals(code, params["code"])
    }
}
