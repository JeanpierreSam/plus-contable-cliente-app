package pe.pluscontable.cliente.datos

import kotlin.test.Test
import kotlin.test.assertEquals

class SemaforoTest {
    private fun o(vence: String, estado: String) = Obligacion(1, "IGV", "2026-10", vence, estado)
    private val hoy = "2026-11-11"
    private val limite = "2026-11-18"

    @Test
    fun rojoSiAlgoSinPresentarYaVencio() =
        assertEquals(Semaforo.ROJO, semaforo(listOf(o("2026-11-01", "VENCIDA"), o("2026-11-13", "PENDIENTE")), hoy, limite))

    @Test
    fun amarilloSiVenceEnLaSemana() =
        assertEquals(Semaforo.AMARILLO, semaforo(listOf(o("2026-11-13", "PENDIENTE")), hoy, limite))

    @Test
    fun verdeSiTodoPresentadoOLejano() =
        assertEquals(Semaforo.VERDE, semaforo(listOf(o("2026-10-01", "PRESENTADA"), o("2026-12-15", "PENDIENTE")), hoy, limite))

    @Test
    fun proximoIgnoraPresentadasYPasadas() {
        val lista = listOf(o("2026-11-20", "PENDIENTE"), o("2026-11-13", "PENDIENTE"), o("2026-11-12", "PRESENTADA"), o("2026-11-01", "VENCIDA"))
        assertEquals("2026-11-13", proximoVencimiento(lista, hoy)?.vence)
    }

    @Test
    fun solesConDosDecimales() = assertEquals("S/ 350.00", soles(350.0)).also { assertEquals("S/ 12.05", soles(12.05)) }
}
