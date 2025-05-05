package com.argumentor.database.mappers

import com.argumentor.database.entities.*
import com.argumentor.models.Debate
import com.argumentor.models.DebateEntry
import com.argumentor.models.DebatePosition
import com.argumentor.models.DebateStage
import com.argumentor.models.Jugador
import com.argumentor.models.Tema
import com.argumentor.models.Usuario
import java.util.UUID

/**
 * Funciones de mapeo para convertir entre entidades de base de datos y modelos de dominio.
 * Estas funciones facilitan la integración de Room con los modelos existentes.
 */
object DataMappers {

    // Mapeo Usuario -> UserEntity
    fun toUserEntity(usuario: Usuario): UserEntity {
        return UserEntity(
            userId = usuario.id,
            name = usuario.nombre,
            email = null, // Los modelos actuales no tienen email
            password = "default_password" // Esto debe ser actualizado con valores reales
        )
    }

    // Mapeo UserEntity -> Usuario
    fun toUsuario(userEntity: UserEntity): Usuario {
        return Usuario(
            id = userEntity.userId,
            nombre = userEntity.name
        )
    }

    // Mapeo Tema -> TopicEntity
    fun toTopicEntity(tema: Tema): TopicEntity {
        return TopicEntity(
            topicName = tema.nombre,
            description = tema.descripcion ?: ""
        )
    }

    // Mapeo TopicEntity -> Tema
    fun toTema(topicEntity: TopicEntity): Tema {
        return Tema(
            nombre = topicEntity.topicName,
            descripcion = topicEntity.description,
            opinionSeleccionada = "" // Valor por defecto, debe ser actualizado con la postura del usuario
        )
    }

    // Mapeo para UserStanceEntity
    fun toUserStanceEntity(userId: String, tema: Tema): UserStanceEntity {
        return UserStanceEntity(
            userId = userId,
            topicName = tema.nombre,
            stance = tema.opinionSeleccionada
        )
    }

    // Mapeo para actualizar Tema con UserStanceEntity
    fun updateTemaWithStance(tema: Tema, stance: UserStanceEntity?): Tema {
        return stance?.let {
            tema.copy(opinionSeleccionada = it.stance)
        } ?: tema
    }

    // Mapeo Debate -> DebateEntity
    fun toDebateEntity(debate: Debate): DebateEntity {
        return DebateEntity(
            debateId = debate.id.ifEmpty { UUID.randomUUID().toString() },
            title = debate.title,
            description = debate.description,
            authorUserId = debate.author,
            participantFavorUserId = debate.participantFavor,
            participantContraUserId = debate.participantContra,
            status = debate.status.name,
            timestamp = debate.timestamp,
            category = debate.category
        )
    }

    // Mapeo DebateEntity -> Debate
    fun toDebate(debateEntity: DebateEntity): Debate {
        return Debate(
            id = debateEntity.debateId,
            title = debateEntity.title,
            description = debateEntity.description,
            author = debateEntity.authorUserId ?: "",
            currentStage = getDebateStageFromString(debateEntity.status),
            participantFavor = debateEntity.participantFavorUserId,
            participantContra = debateEntity.participantContraUserId,
            status = getDebateStatusFromString(debateEntity.status),
            timestamp = debateEntity.timestamp,
            category = debateEntity.category
        )
    }

    // Mapeo DebateEntry -> ArgumentEntity
    fun toArgumentEntity(entry: DebateEntry): ArgumentEntity {
        return ArgumentEntity(
            argumentId = 0, // ID generado automáticamente
            debateId = entry.debateId,
            userId = entry.userId,
            stage = entry.stage.name,
            position = entry.position.name,
            content = entry.content,
            timestamp = entry.timestamp
        )
    }

    // Mapeo ArgumentEntity -> DebateEntry
    fun toDebateEntry(argumentEntity: ArgumentEntity): DebateEntry {
        return DebateEntry(
            id = argumentEntity.argumentId.toString(),
            debateId = argumentEntity.debateId,
            userId = argumentEntity.userId,
            stage = getDebateStageFromString(argumentEntity.stage),
            content = argumentEntity.content,
            position = getDebatePositionFromString(argumentEntity.position),
            timestamp = argumentEntity.timestamp
        )
    }

    // Convertir string a DebateStage
    fun getDebateStageFromString(stage: String): DebateStage {
        return when (stage) {
            "INTRODUCCION" -> DebateStage.INTRODUCCION
            "REFUTACION1" -> DebateStage.REFUTACION1
            "REFUTACION2" -> DebateStage.REFUTACION2
            "CONCLUSION" -> DebateStage.CONCLUSION
            else -> DebateStage.INTRODUCCION // Valor por defecto
        }
    }

    // Convertir string a DebatePosition
    fun getDebatePositionFromString(position: String): DebatePosition {
        return when (position) {
            "A_FAVOR" -> DebatePosition.A_FAVOR
            "EN_CONTRA" -> DebatePosition.EN_CONTRA
            else -> DebatePosition.A_FAVOR // Valor por defecto
        }
    }

    // Convertir string a Debate.Status
    fun getDebateStatusFromString(status: String): Debate.Status {
        return when (status) {
            "PENDIENTE" -> Debate.Status.PENDIENTE
            "ACTIVO" -> Debate.Status.ACTIVO
            "TERMINADO" -> Debate.Status.TERMINADO
            else -> Debate.Status.PENDIENTE // Valor por defecto
        }
    }

    // Función para actualizar un Jugador con datos de TopicEntities
    fun updateJugadorWithTopics(jugador: Jugador, topics: List<TopicEntity>, stances: List<UserStanceEntity>): Jugador {
        val temas = topics.map { topicEntity ->
            val stance = stances.firstOrNull { it.topicName == topicEntity.topicName }
            Tema(
                nombre = topicEntity.topicName,
                descripcion = topicEntity.description,
                opinionSeleccionada = stance?.stance ?: ""
            )
        }.toMutableList()  // Convert the immutable List to a MutableList
        
        return jugador.copy(listaTemas = temas)
    }
}