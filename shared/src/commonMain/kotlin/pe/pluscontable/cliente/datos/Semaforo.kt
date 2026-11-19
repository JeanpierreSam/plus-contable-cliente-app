package pe.pluscontable.cliente.datos

import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

enum class Semaforo { VERDE, AMARILLO, ROJO }

/** Perú es UTC-5 todo el año (sin horario de verano): se resta 5 h para obtener la fecha de Lima. */
private fun fechaLima(dias: Int = 0) = (Clock.System.now() - 5.hours + dias.days).toString().take(10)

/** Fecha de hoy en Lima, "AAAA-MM-DD". */
fun hoy() = fechaLima()

fun dentroDeSieteDias() = fechaLima(7)

/** Con saldo por pagar: declarada sin pago o con pago parcial. Es lo único que puede estar VENCIDA. */
private fun Obligacion.conSaldo() = estado == "DECLARADO" || estado == "PAGO_PARCIAL"

/** Sin pagar del todo (incluye sin declarar, que se muestra aparte y nunca como VENCIDA). */
private fun Obligacion.abierta() = estado != "PAGADO"

private fun Obligacion.vencida(hoy: String) = conSaldo() && fechaVencimiento < hoy

/** Lo que se muestra: VENCIDA = fecha pasada y saldo pendiente; sin declarar y pagada nunca. */
fun estadoVisible(o: Obligacion, hoy: String) = if (o.vencida(hoy)) "VENCIDA" else o.estado

/**
 * Rojo: algo con saldo ya venció. Amarillo: algo sin pagar vence hasta [limite] o está sin declarar con la fecha pasada.
 * Las fechas son ISO: comparar texto = comparar fechas.
 */
fun semaforo(obligaciones: List<Obligacion>, hoy: String, limite: String): Semaforo {
    val abiertas = obligaciones.filter { it.abierta() }
    return when {
        abiertas.any { it.vencida(hoy) } -> Semaforo.ROJO
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
