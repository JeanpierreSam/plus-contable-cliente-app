package pe.pluscontable.cliente.datos

import kotlinx.serialization.Serializable

/** Cuerpo de `POST /api/tributario/incidencias`. Los demás DTO coinciden con los modelos del dominio (ver Modelos.kt). */
@Serializable
data class IncidenciaNueva(val tipo: String, val clienteId: Long, val fecha: String, val descripcion: String)
