package pe.pluscontable.cliente.datos

import kotlinx.serialization.Serializable

// Forma según docs/contrato-api-cliente.md del backend (rama feature/api-cliente). Las fechas llegan como "AAAA-MM-DD".
@Serializable
data class Obligacion(val id: Long, val tipoObligacion: String, val periodo: String, val fechaVencimiento: String, val estado: String)

@Serializable
data class Cobro(val id: Long, val fechaCobro: String, val monto: Double, val estado: String)

@Serializable
data class Cuota(
    val id: Long,
    val numero: Int,
    val fechaVencimiento: String,
    val monto: Double,
    val estado: String,
    val fechaPago: String? = null,
    val enRiesgo: Boolean = false,
)

@Serializable
data class Fraccionamiento(
    val id: Long,
    val tributo: String,
    val montoTotal: Double,
    val numeroCuotas: Int,
    val fechaPrimeraCuota: String,
    val cuotas: List<Cuota> = emptyList(),
)

/** Lo que la app lee del estudio: hoy [DatosLocal] (SQLite), en producción [DatosApi]. Se cambia en moduloApp. */
interface DatosCliente {
    suspend fun obligaciones(): List<Obligacion>
    suspend fun cobros(): List<Cobro>
    suspend fun fraccionamientos(): List<Fraccionamiento>
}
