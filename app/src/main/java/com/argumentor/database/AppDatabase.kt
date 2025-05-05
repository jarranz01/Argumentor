package com.argumentor.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.argumentor.R
import com.argumentor.database.dao.*
import com.argumentor.database.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    version = 1,
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
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "argumentor_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Callback para operaciones durante la creación de la base de datos.
         */
        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.topicDao(), database.userDao())
                    }
                }
            }
        }

        /**
         * Prepopula la base de datos con datos iniciales.
         */
        private suspend fun populateDatabase(topicDao: TopicDao, userDao: UserDao) {
            // Insertar temas iniciales
            val initialTopics = getInitialTopics()
            topicDao.insertTopics(initialTopics)

            // Insertar un usuario de prueba para desarrollo
            val testUser = UserEntity(
                userId = "test_user_id",
                name = "TestUser",
                email = "test@example.com",
                password = "password123" // En un caso real, esto debería ser hasheado
            )
            userDao.insertUser(testUser)
        }

        /**
         * Obtiene una lista de temas iniciales para prepopular la base de datos.
         * En una implementación real, estos deberían obtenerse de resources.
         */
        private fun getInitialTopics(): List<TopicEntity> {
            return listOf(
                TopicEntity("climate_change", "This topic addresses the existence and reality of climate change"),
                TopicEntity("nuclear_energy", "This topic focuses on the use of nuclear energy as a source of electricity"),
                TopicEntity("social_media", "This topic explores the impact of daily use of social media on people's lives"),
                TopicEntity("online_education", "This topic focuses on distance education through digital platforms"),
                TopicEntity("artificial_intelligence", "This topic explores the ethical considerations of AI"),
                TopicEntity("abortion", "This topic discusses the ethical, legal and social aspects of abortion"),
                TopicEntity("bullfighting", "This topic debates the tradition versus animal rights aspects of bullfighting"),
                TopicEntity("film_subsidies", "This topic examines whether government should subsidize film industry"),
                TopicEntity("open_borders", "This topic discusses immigration policies and open borders"),
                TopicEntity("freedom_of_speech", "This topic explores the limits and importance of free speech"),
                TopicEntity("marijuana", "This topic debates the legalization of marijuana")
            )
        }
    }
}