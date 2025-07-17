package com.gmolate.sunday

import android.app.Application
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions

class SundayApplication : Application() {
    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        networkMonitor = NetworkMonitor(this)

        // Inicializar Google Fit options
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(com.google.android.gms.fitness.data.DataType.TYPE_NUTRITION, FitnessOptions.ACCESS_READ)
            .addDataType(com.google.android.gms.fitness.data.DataType.TYPE_NUTRITION, FitnessOptions.ACCESS_WRITE)
            .build()

        // Pre-verificar permisos de Google Fit
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            // Los permisos se solicitarán en la MainActivity
        }
    }

    companion object {
        private lateinit var instance: SundayApplication

        fun getInstance(): SundayApplication {
            return instance
        }
    }

    init {
        instance = this
    }
}
