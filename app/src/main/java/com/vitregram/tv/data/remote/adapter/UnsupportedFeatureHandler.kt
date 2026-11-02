package com.vitregram.tv.data.remote.adapter

import com.vitregram.tv.domain.model.ActionResult
import com.vitregram.tv.domain.model.NetworkResult

/**
 * Explicitly marks unavailable Instagram interfaces per PRD Section 3 & 7 requirements:
 * "Features must be marked unavailable when Instagram's supported interfaces do not expose them.
 * Do not simulate successful actions."
 */
class UnsupportedFeatureHandler {

    fun handleLiveBroadcasting(): NetworkResult<Nothing> {
        return NetworkResult.Unsupported(
            featureName = "Live Broadcasting",
            reason = "Live video broadcasting requires official Meta partner ingest RTMP permissions and is unavailable for third-party TV clients."
        )
    }

    fun handleAudioVideoCalling(): NetworkResult<Nothing> {
        return NetworkResult.Unsupported(
            featureName = "Voice & Video Calls",
            reason = "Instagram Direct video/voice calling utilizes proprietary WebRTC signaling keys not exposed outside the official mobile application."
        )
    }

    fun handleDirectMediaUpload(): ActionResult {
        return ActionResult(
            success = false,
            message = "Media creation and publishing directly from TV is not permitted by Instagram Web API. Please use the official mobile app to post new content."
        )
    }

    fun handleAccountSwitching(): NetworkResult<Nothing> {
        return NetworkResult.Unsupported(
            featureName = "Multiple Accounts",
            reason = "Multi-account concurrent sessions require separate encrypted keystore containers. Log out to switch accounts."
        )
    }
}
