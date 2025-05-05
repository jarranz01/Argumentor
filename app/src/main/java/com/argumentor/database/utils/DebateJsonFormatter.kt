package com.argumentor.database.utils

import com.argumentor.database.entities.ArgumentEntity
import com.argumentor.database.entities.DebateEntity
import com.argumentor.database.entities.UserEntity
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.Gson

/**
 * Utilidad para formatear debates en formato JSON para su análisis por un LLM.
 */
class DebateJsonFormatter {

    private val gson = Gson()
    
    /**
     * Convierte un debate con sus argumentos al formato JSON requerido para el LLM.
     *
     * @param debate Entidad del debate
     * @param arguments Lista de argumentos del debate
     * @param favorUser Usuario que defiende la postura a favor
     * @param contraUser Usuario que defiende la postura en contra
     * @return String con la representación JSON del debate
     */
    fun formatDebateToJson(
        debate: DebateEntity,
        arguments: List<ArgumentEntity>,
        favorUser: UserEntity?,
        contraUser: UserEntity?
    ): String {
        val jsonObject = JsonObject()
        val debateObject = JsonObject()
        
        // Añadir información del tema
        debateObject.addProperty("tema", debate.title)
        
        // Añadir información de los participantes
        val participantesObject = JsonObject()
        participantesObject.addProperty("pro", favorUser?.name ?: debate.participantFavorUserId)
        participantesObject.addProperty("con", contraUser?.name ?: debate.participantContraUserId)
        debateObject.add("participantes", participantesObject)
        
        // Añadir fases del debate
        val fasesObject = JsonObject()
        
        // Agrupar argumentos por fase
        val argumentsByStage = arguments.groupBy { it.stage }
        
        // Añadir cada fase
        addPhaseToJson(fasesObject, "introduccion", argumentsByStage["INTRODUCCION"] ?: emptyList())
        addPhaseToJson(fasesObject, "refutacion_1", argumentsByStage["REFUTACION1"] ?: emptyList())
        addPhaseToJson(fasesObject, "refutacion_2", argumentsByStage["REFUTACION2"] ?: emptyList())
        addPhaseToJson(fasesObject, "conclusion", argumentsByStage["CONCLUSION"] ?: emptyList())
        
        debateObject.add("fases", fasesObject)
        jsonObject.add("debate", debateObject)
        
        return gson.toJson(jsonObject)
    }
    
    /**
     * Añade una fase del debate al objeto JSON.
     */
    private fun addPhaseToJson(fasesObject: JsonObject, phaseName: String, arguments: List<ArgumentEntity>) {
        val phaseArray = JsonArray()
        
        // Ordenar argumentos: primero el "pro" (A_FAVOR) y luego el "con" (EN_CONTRA)
        val sortedArguments = arguments.sortedBy { 
            if (it.position == "A_FAVOR") 0 else 1 
        }
        
        for (argument in sortedArguments) {
            val argumentObject = JsonObject()
            argumentObject.addProperty("rol", if (argument.position == "A_FAVOR") "pro" else "con")
            argumentObject.addProperty("texto", argument.content)
            phaseArray.add(argumentObject)
        }
        
        fasesObject.add(phaseName, phaseArray)
    }
}