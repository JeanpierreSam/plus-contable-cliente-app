package pe.pluscontable.cliente.domain

import kotlinx.serialization.Serializable

// Modelos del dominio. shortcut: llevan @Serializable porque la API los devuelve con esta forma y se reutilizan como DTO;
// separar DTO y modelo (con mapeadores) cuando el contrato y el dominio dejen de coincidir.
// Las fechas son "AAAA-MM-DD".
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

@Serializable
data class Incidencia(val id: Long, val tipo: String, val fecha: String, val descripcion: String, val registradaEn: String? = null)

/** El expediente legal del cliente (el archivo siempre es PDF). */
@Serializable
data class Documento(
    val id: Long,
    val tipo: String,
    val version: Int,
    val fechaEmision: String? = null,
    val nombreArchivo: String,
    val tamanoBytes: Long,
)

/** Notificación del Buzón SOL; solo lectura. */
@Serializable
data class Aviso(
    val id: Long,
    val tipo: String,
    val fechaRecepcion: String,
    val plazoRespuesta: String? = null,
    val estado: String,
    val fechaAtencion: String? = null,
)

@Serializable
data class Usuario(
    val id: Long,
    val username: String,
    val nombreCompleto: String? = null,
    val roles: List<String> = emptyList(),
)

val etiquetasDocumento = mapOf("MINUTA" to "Minuta", "VIGENCIA_PODERES" to "Vigencia de poderes", "FICHA_RUC" to "Ficha RUC")

val etiquetasAviso = mapOf("ESQUELA" to "Esquela", "CARTA_INDUCTIVA" to "Carta inductiva", "COMUNICADO" to "Comunicado")

/** Tipos (valores de TipoIncidencia del backend) que el cliente puede reportar, con su etiqueta. */
val tiposIncidencia = listOf(
    "ERROR_PROCESO_CONTABLE" to "Error en mis declaraciones",
    "PROBLEMA_SIRE" to "Problema con SIRE",
    "OTRO" to "Otra consulta",
)

/** Error de la API con un mensaje listo para mostrar. [noAutorizado]: el token o las credenciales no sirven. */
class ApiException(mensaje: String, val noAutorizado: Boolean = false) : Exception(mensaje)

/** Abre un PDF con la app del teléfono; la plataforma lo implementa (androidApp). Lanza si no hay visor. */
fun interface AbridorPdf { fun abrir(nombre: String, bytes: ByteArray) }
