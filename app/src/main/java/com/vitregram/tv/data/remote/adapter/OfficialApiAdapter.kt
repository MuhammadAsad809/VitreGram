package com.vitregram.tv.data.remote.adapter

import com.vitregram.tv.domain.model.*

/**
 * Adapter for official Instagram Graph API.
 * Suited for creator/professional accounts with Meta developer access tokens.
 */
class OfficialApiAdapter {

    suspend fun getProfessionalFeed(accessToken: String): NetworkResult<List<Post>> {
        // Official Graph API endpoint: graph.instagram.com/me/media
        return NetworkResult.Unsupported(
            featureName = "Official Graph API Feed",
            reason = "Official Graph API only supports business/creator pages with verified Facebook Developer App credentials."
        )
    }

    suspend fun getProfessionalInsights(accessToken: String): NetworkResult<Map<String, Any>> {
        return NetworkResult.Unsupported(
            featureName = "Creator Insights",
            reason = "Requires Instagram Creator permissions."
        )
    }
}
