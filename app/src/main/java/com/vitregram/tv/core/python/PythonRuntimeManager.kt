package com.vitregram.tv.core.python

import android.content.Context
import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the single lifecycle of the Chaquopy Python runtime environment.
 * Ensures Python is safely initialized once, on a background worker or application startup,
 * with thread-safe access to Python modules.
 */
class PythonRuntimeManager private constructor() {

    private val mutex = Mutex()
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Initializes the Chaquopy Python platform if not already started.
     */
    suspend fun initialize(context: Context) {
        if (Python.isStarted()) {
            _isInitialized.value = true
            return
        }

        mutex.withLock {
            if (!Python.isStarted()) {
                try {
                    Log.i(TAG, "Starting Chaquopy Python AndroidPlatform...")
                    Python.start(AndroidPlatform(context.applicationContext))
                    _isInitialized.value = true
                    Log.i(TAG, "Chaquopy Python runtime initialized successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize Chaquopy Python runtime", e)
                    _isInitialized.value = false
                }
            } else {
                _isInitialized.value = true
            }
        }
    }

    /**
     * Returns the global Python engine instance.
     */
    fun getPython(): Python {
        check(Python.isStarted()) { "Python runtime has not been initialized. Call initialize() first." }
        return Python.getInstance()
    }

    /**
     * Safely retrieves a Python module by name.
     */
    fun getModule(moduleName: String): PyObject {
        return getPython().getModule(moduleName)
    }

    companion object {
        private const val TAG = "PythonRuntimeManager"

        @Volatile
        private var instance: PythonRuntimeManager? = null

        fun getInstance(): PythonRuntimeManager {
            return instance ?: synchronized(this) {
                instance ?: PythonRuntimeManager().also { instance = it }
            }
        }
    }
}
