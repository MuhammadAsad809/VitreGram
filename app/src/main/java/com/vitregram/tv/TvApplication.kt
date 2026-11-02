package com.vitregram.tv

import android.app.Application
import com.vitregram.tv.data.local.db.VitreGramDatabase
import com.vitregram.tv.data.local.security.KeystoreManager
import com.vitregram.tv.data.remote.adapter.OfficialApiAdapter
import com.vitregram.tv.data.remote.adapter.UnsupportedFeatureHandler
import com.vitregram.tv.data.remote.adapter.WebSessionAdapter
import com.vitregram.tv.data.repository.InstagramRepositoryImpl
import com.vitregram.tv.domain.repository.InstagramRepository

/**
 * Main Application container initializing database, secure storage, and repository.
 */
class TvApplication : Application() {

    lateinit var database: VitreGramDatabase
        private set

    lateinit var keystoreManager: KeystoreManager
        private set

    lateinit var instagramRepository: InstagramRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Room Database
        database = VitreGramDatabase.getInstance(this)

        // Initialize Android Keystore-backed secure token storage
        keystoreManager = KeystoreManager(this)

        // Initialize integration adapters
        val officialAdapter = OfficialApiAdapter()
        val webSessionAdapter = WebSessionAdapter(keystoreManager)
        val unsupportedHandler = UnsupportedFeatureHandler()

        instagramRepository = InstagramRepositoryImpl(
            officialAdapter = officialAdapter,
            webSessionAdapter = webSessionAdapter,
            unsupportedHandler = unsupportedHandler,
            database = database,
            keystoreManager = keystoreManager
        )
    }

    companion object {
        lateinit var instance: TvApplication
            private set
    }
}
