package pe.pluscontable.cliente.datos

import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

enum class Semaforo { VERDE, AMARILLO, ROJO }

/** Fecha de hoy "AAAA-MM-DD". shortcut: en UTC (a la noche en Perú ya marca el día siguiente); usar kotlinx-datetime si molesta. */
fun hoy() = Clock.System.now().toString().take(10)

fun dentroDeSieteDias() = (Clock.System.now() + 7.days).toString().take(10)

/** Rojo: algo sin presentar ya venció. Amarillo: algo sin presentar vence hasta [limite]. Las fechas son ISO: comparar texto = comparar fechas. */
fun semaforo(obligaciones: List<Obligacion>, hoy: String, limite: String): Semaforo {
    val abiertas = obligaciones.filter { it.estado != "PRESENTADA" }
    return when {
        abiertas.any { it.vence < hoy } -> Semaforo.ROJO
        abiertas.any { it.vence <= limite } -> Semaforo.AMARILLO
        else -> Semaforo.VERDE
    }
}

fun proximoVencimiento(obligaciones: List<Obligacion>, hoy: String) =
    obligaciones.filter { it.estado != "PRESENTADA" && it.vence >= hoy }.minByOrNull { it.vence }

fun soles(monto: Double): String {
    val c = (monto * 100).roundToLong()
    return "S/ ${c / 100}.${(c % 100).toString().padStart(2, '0')}"
}
