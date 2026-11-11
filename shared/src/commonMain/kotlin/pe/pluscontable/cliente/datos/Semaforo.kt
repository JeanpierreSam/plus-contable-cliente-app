package pe.pluscontable.cliente.datos

import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

enum class Semaforo { VERDE, AMARILLO, ROJO }

/** Fecha de hoy "AAAA-MM-DD". shortcut: en UTC (a la noche en Perú ya marca el día siguiente); usar kotlinx-datetime si molesta. */
fun hoy() = Clock.System.now().toString().take(10)

fun dentroDeSieteDias() = (Clock.System.now() + 7.days).toString().take(10)

/** El backend no tiene estado VENCIDA: una obligación está abierta mientras no esté PAGADO ni DECLARADO. */
private fun Obligacion.abierta() = estado != "PAGADO" && estado != "DECLARADO"

/** Lo que se muestra: VENCIDA se deduce con la fecha. */
fun estadoVisible(o: Obligacion, hoy: String) = if (o.abierta() && o.fechaVencimiento < hoy) "VENCIDA" else o.estado

/** Rojo: algo abierto ya venció. Amarillo: algo abierto vence hasta [limite]. Las fechas son ISO: comparar texto = comparar fechas. */
fun semaforo(obligaciones: List<Obligacion>, hoy: String, limite: String): Semaforo {
    val abiertas = obligaciones.filter { it.abierta() }
    return when {
        abiertas.any { it.fechaVencimiento < hoy } -> Semaforo.ROJO
        abiertas.any { it.fechaVencimiento <= limite } -> Semaforo.AMARILLO
        else -> Semaforo.VERDE
    }
}

fun proximoVencimiento(obligaciones: List<Obligacion>, hoy: String) =
    obligaciones.filter { it.abierta() && it.fechaVencimiento >= hoy }.minByOrNull { it.fechaVencimiento }

fun soles(monto: Double): String {
    val c = (monto * 100).roundToLong()
    return "S/ ${c / 100}.${(c % 100).toString().padStart(2, '0')}"
}
