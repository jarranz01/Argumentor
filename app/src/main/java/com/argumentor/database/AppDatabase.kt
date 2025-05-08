package com.argumentor.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.argumentor.R
import com.argumentor.database.dao.*
import com.argumentor.database.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Base de datos principal para la aplicación Argumentor.
 * Esta clase sigue el patrón Singleton para garantizar una única instancia de la base de datos.
 */
@Database(
    entities = [
        UserEntity::class,
        TopicEntity::class,
        UserStanceEntity::class,
        DebateEntity::class,
        ArgumentEntity::class
    ],
    version = 2, // Incrementamos la versión para la nueva migración
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun topicDao(): TopicDao
    abstract fun userStanceDao(): UserStanceDao
    abstract fun debateDao(): DebateDao
    abstract fun argumentDao(): ArgumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "argumentor_database"

        // Migración para agregar el campo username a la tabla users
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Añadir la columna username a la tabla users
                try {
                    database.execSQL("ALTER TABLE users ADD COLUMN username TEXT NOT NULL DEFAULT ''")
                    Timber.d("Migración 1-2: Columna username añadida a la tabla users")
                    
                    // Actualizar el username con el valor del name por defecto
                    database.execSQL("UPDATE users SET username = name WHERE username = ''")
                    Timber.d("Migración 1-2: Valores username rellenados con name")
                } catch (e: Exception) {
                    Timber.e(e, "Error en migración 1-2")
                }
            }
        }

        /**
         * Obtiene una instancia de la base de datos.
         * Si no existe una instancia, crea una nueva.
         *
         * @param context El contexto de la aplicación
         * @param scope El scope de corrutina para operaciones iniciales de la base de datos
         * @return La instancia de AppDatabase
         */
        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): AppDatabase {
            // Si se desea forzar una limpieza completa, descomentar
            // cleanDatabase(context)
            
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2) // Agregar migración
                .fallbackToDestructiveMigration() // Como respaldo si falla la migración
                .addCallback(AppDatabaseCallback(scope, context.applicationContext))
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Limpia forzosamente cualquier base de datos existente
         */
        private fun cleanDatabase(context: Context) {
            try {
                // Intentar borrar la base de datos existente y todos sus archivos
                context.deleteDatabase(DATABASE_NAME)
                
                // Si eso no funciona, intentar eliminar los archivos directamente
                val databases = context.databaseList()
                for (db in databases) {
                    if (db.contains(DATABASE_NAME)) {
                        Timber.d("Eliminando base de datos: $db")
                        context.deleteDatabase(db)
                    }
                }
                
                // Si todavía falla, intentar eliminar los archivos manualmente
                val dbFile = context.getDatabasePath(DATABASE_NAME)
                if (dbFile.exists()) {
                    try {
                        dbFile.delete()
                        // Eliminar también otros archivos relacionados
                        File("${dbFile.path}-shm").delete()
                        File("${dbFile.path}-wal").delete()
                        File("${dbFile.path}-journal").delete()
                        Timber.d("Archivos de base de datos eliminados manualmente")
                    } catch (e: Exception) {
                        Timber.e(e, "Error eliminando archivos de base de datos manualmente")
                    }
                }
                
                Timber.d("Base de datos limpiada exitosamente")
            } catch (e: Exception) {
                Timber.e(e, "Error limpiando base de datos")
            }
        }

        /**
         * Callback para operaciones durante la creación de la base de datos.
         */
        private class AppDatabaseCallback(
            private val scope: CoroutineScope,
            private val context: Context
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Timber.d("¡Base de datos creada! Inicializando datos predeterminados...")
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.topicDao(), database.userDao(), context)
                    }
                }
            }
            
            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                Timber.d("¡Migración destructiva de la base de datos! Reinicializando datos...")
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.topicDao(), database.userDao(), context)
                    }
                }
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Timber.d("Base de datos abierta correctamente")
            }
        }

        /**
         * Prepopula la base de datos con datos iniciales.
         */
        private suspend fun populateDatabase(topicDao: TopicDao, userDao: UserDao, context: Context) {
            // Insertar temas iniciales
            val initialTopics = getInitialTopics(context)
            topicDao.insertTopics(initialTopics)
            Timber.d("Temas iniciales cargados: ${initialTopics.size} temas")

            // Insertar un usuario de prueba para desarrollo
            val testUser = UserEntity(
                userId = "test_user_id",
                name = "TestUser",
                email = "test@example.com",
                password = "password123", // En un caso real, esto debería ser hasheado
                username = "testuser"
            )
            userDao.insertUser(testUser)
            Timber.d("Usuario de prueba creado: ${testUser.userId}")
        }

        /**
         * Obtiene una lista de temas iniciales para prepopular la base de datos.
         * Las descripciones se obtienen de los recursos de la aplicación.
         */
        private fun getInitialTopics(context: Context): List<TopicEntity> {
            return listOf(
                TopicEntity(
                    "climate_change", 
                    context.getString(R.string.climate_change_description)
                ),
                TopicEntity(
                    "nuclear_energy", 
                    context.getString(R.string.nuclear_energy_description)
                ),
                TopicEntity(
                    "social_media", 
                    context.getString(R.string.social_media_description)
                ),
                TopicEntity(
                    "online_education", 
                    context.getString(R.string.online_education_description)
                ),
                TopicEntity(
                    "artificial_intelligence", 
                    context.getString(R.string.artificial_intelligence_description)
                ),
                TopicEntity(
                    "abortion", 
                    context.getString(R.string.abortion_description)
                ),
                TopicEntity(
                    "bullfighting", 
                    context.getString(R.string.bullfighting_description)
                ),
                TopicEntity(
                    "film_subsidies", 
                    context.getString(R.string.film_subsidies_description)
                ),
                TopicEntity(
                    "open_borders", 
                    context.getString(R.string.open_borders_description)
                ),
                TopicEntity(
                    "freedom_of_speech", 
                    context.getString(R.string.freedom_of_speech_description)
                ),
                TopicEntity(
                    "marijuana", 
                    context.getString(R.string.marijuana_description)
                )
            )
        }
    }
}