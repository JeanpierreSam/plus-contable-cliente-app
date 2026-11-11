package pe.pluscontable.cliente.datos

import kotlinx.serialization.Serializable

// Forma PROVISIONAL de los DTO: se ajusta a docs/contrato-api-cliente.md cuando exista (HU-A5).
@Serializable
data class Obligacion(val id: Long, val tributo: String, val periodo: String, val vence: String, val estado: String)

@Serializable
data class Cobro(val id: Long, val concepto: String, val periodo: String, val monto: Double, val vence: String, val estado: String)

@Serializable
data class Cuota(val numero: Int, val vence: String, val monto: Double, val estado: String)

/**
 * Lo que la app lee del estudio. Hoy lo cubre [pe.pluscontable.cliente.datos.db.DatosDemo];
 * para pasar a la API se escribe un DatosApi(http) con las rutas del PLAN y se cambia una línea en moduloApp.
 */
interface DatosCliente {
    suspend fun obligaciones(): List<Obligacion>
    suspend fun cobros(): List<Cobro>
    suspend fun cuotas(cobroId: Long): List<Cuota>
}
