package com.vitregram.tv

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.vitregram.tv.core.python.PythonInstagramBridge
import com.vitregram.tv.core.python.PythonRuntimeManager
import com.vitregram.tv.data.instagram.ChaquopyInstagramRepository
import com.vitregram.tv.data.instagram.PythonInstagramDataSource
import com.vitregram.tv.data.local.db.VitreGramDatabase
import com.vitregram.tv.data.local.security.KeystoreManager
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

        // Initialize Chaquopy Python runtime on Android
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        val pythonManager = PythonRuntimeManager.getInstance()
        val pythonBridge = PythonInstagramBridge(pythonManager)
        val pythonDataSource = PythonInstagramDataSource(pythonBridge)

        instagramRepository = ChaquopyInstagramRepository(
            dataSource = pythonDataSource,
            database = database,
            keystoreManager = keystoreManager
        )
    }

    companion object {
        lateinit var instance: TvApplication
            private set
    }
}
