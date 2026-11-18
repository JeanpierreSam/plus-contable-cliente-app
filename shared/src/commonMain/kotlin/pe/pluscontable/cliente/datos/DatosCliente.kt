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

// IncidenciaDto del backend (ms-tributario); la app ignora registradaPor, clientesAsociados y observaciones.
@Serializable
data class Incidencia(val id: Long, val tipo: String, val fecha: String, val descripcion: String, val registradaEn: String? = null)

@Serializable
data class IncidenciaNueva(val tipo: String, val clienteId: Long, val fecha: String, val descripcion: String)

// DocumentoDto del backend: el expediente legal del cliente (el archivo siempre es PDF).
@Serializable
data class Documento(
    val id: Long,
    val tipo: String,
    val version: Int,
    val fechaEmision: String? = null,
    val nombreArchivo: String,
    val tamanoBytes: Long,
)

val etiquetasDocumento = mapOf("MINUTA" to "Minuta", "VIGENCIA_PODERES" to "Vigencia de poderes", "FICHA_RUC" to "Ficha RUC")

// NotificacionSolDto del backend (Buzón SOL); solo lectura. Las fechas llegan como "AAAA-MM-DD".
@Serializable
data class Aviso(
    val id: Long,
    val tipo: String,
    val fechaRecepcion: String,
    val plazoRespuesta: String? = null,
    val estado: String,
    val fechaAtencion: String? = null,
)

val etiquetasAviso = mapOf("ESQUELA" to "Esquela", "CARTA_INDUCTIVA" to "Carta inductiva", "COMUNICADO" to "Comunicado")

/** Abre un PDF con la app del teléfono; la plataforma lo implementa (androidApp). Lanza si no hay visor. */
fun interface AbridorPdf { fun abrir(nombre: String, bytes: ByteArray) }

/** Tipos (valores de TipoIncidencia del backend) que el cliente puede reportar, con su etiqueta. */
val tiposIncidencia = listOf(
    "ERROR_PROCESO_CONTABLE" to "Error en mis declaraciones",
    "PROBLEMA_SIRE" to "Problema con SIRE",
    "OTRO" to "Otra consulta",
)

/** Lo que la app lee del estudio: hoy [DatosLocal] (SQLite), en producción [DatosApi]. Se cambia en moduloApp. */
interface DatosCliente {
    suspend fun obligaciones(): List<Obligacion>
    suspend fun cobros(): List<Cobro>
    suspend fun fraccionamientos(): List<Fraccionamiento>
    suspend fun avisos(): List<Aviso>
    suspend fun documentos(): List<Documento>
    suspend fun descargarDocumento(id: Long): ByteArray
    suspend fun incidencias(): List<Incidencia>
    /** Registra una incidencia del cliente con fecha de hoy. */
    suspend fun registrarIncidencia(tipo: String, descripcion: String): Incidencia
}
