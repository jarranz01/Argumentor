package com.argumentor.database.dao

import androidx.room.*
import com.argumentor.database.entities.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceder y modificar los datos de usuarios en la base de datos.
 */
@Dao
interface UserDao {
    /**
     * Inserta un nuevo usuario o reemplaza uno existente con el mismo ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Obtiene un usuario por su ID.
     */
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    /**
     * Obtiene un usuario por su nombre de usuario.
     */
    @Query("SELECT * FROM users WHERE name = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    /**
     * Actualiza los datos de un usuario existente.
     */
    @Update
    suspend fun updateUser(user: UserEntity)

    /**
     * Elimina un usuario.
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)

    /**
     * Obtiene todos los usuarios ordenados por nombre.
     */
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>
}