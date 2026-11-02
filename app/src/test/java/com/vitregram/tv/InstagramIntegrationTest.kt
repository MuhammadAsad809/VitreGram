package com.vitregram.tv

import com.vitregram.tv.data.remote.adapter.UnsupportedFeatureHandler
import com.vitregram.tv.domain.model.NetworkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramIntegrationTest {

    private val unsupportedHandler = UnsupportedFeatureHandler()

    @Test
    fun testLiveBroadcastingIsMarkedUnsupported() {
        val result = unsupportedHandler.handleLiveBroadcasting()
        assertTrue(result is NetworkResult.Unsupported)
        val unsupported = result as NetworkResult.Unsupported
        assertEquals("Live Broadcasting", unsupported.featureName)
    }

    @Test
    fun testVoiceVideoCallingIsMarkedUnsupported() {
        val result = unsupportedHandler.handleAudioVideoCalling()
        assertTrue(result is NetworkResult.Unsupported)
        val unsupported = result as NetworkResult.Unsupported
        assertEquals("Voice & Video Calls", unsupported.featureName)
    }

    @Test
    fun testDirectUploadIsNotPermittedOnTV() {
        val result = unsupportedHandler.handleDirectMediaUpload()
        assertEquals(false, result.success)
        assertTrue(result.message?.contains("not permitted") == true)
    }
}
