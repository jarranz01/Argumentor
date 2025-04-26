package com.argumentor.models

import android.app.Application
import timber.log.Timber
/**
 * Clase base de la aplicación que inicializa componentes globales.
 * 
 * Configura Timber para el logging de mensajes de depuración en builds de desarrollo.
 */
class ArgumentorApplication : Application() {
    /**
     * Método llamado al crear la aplicación. Configura el sistema de logging.
     */
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}