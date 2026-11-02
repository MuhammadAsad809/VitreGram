package com.vitregram.tv.domain.usecase

import com.vitregram.tv.domain.model.*
import com.vitregram.tv.domain.repository.InstagramRepository

class GetHomeFeedUseCase(private val repository: InstagramRepository) {
    suspend operator fun invoke(cursor: String? = null): NetworkResult<List<Post>> {
        return repository.getFeed(cursor)
    }
}

class GetReelsUseCase(private val repository: InstagramRepository) {
    suspend operator fun invoke(cursor: String? = null): NetworkResult<List<Reel>> {
        return repository.getReels(cursor)
    }
}

class GetStoriesUseCase(private val repository: InstagramRepository) {
    suspend operator fun invoke(): NetworkResult<List<Story>> {
        return repository.getStories()
    }
}

class GetExploreUseCase(private val repository: InstagramRepository) {
    suspend operator fun invoke(cursor: String? = null): NetworkResult<List<Post>> {
        return repository.getExplore(cursor)
    }
}

class SearchUseCase(private val repository: InstagramRepository) {
    suspend operator fun invoke(query: String): NetworkResult<Pair<List<User>, List<Reel>>> {
        return repository.search(query)
    }
}

class LikePostUseCase(private val repository: InstagramRepository) {
    suspend operator fun invoke(postId: String): ActionResult {
        return repository.likePost(postId)
    }
}

class SavePostUseCase(private val repository: InstagramRepository) {
    suspend operator fun invoke(postId: String): ActionResult {
        return repository.savePost(postId)
    }
}

class GetProfileUseCase(private val repository: InstagramRepository) {
    suspend operator fun invoke(username: String): NetworkResult<User> {
        return repository.getProfile(username)
    }
}
