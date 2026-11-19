package pe.pluscontable.cliente.datos

import kotlin.test.Test
import kotlin.test.assertEquals

class SemaforoTest {
    private fun o(vence: String, estado: String) = Obligacion(1, "IGV", "2026-10-01", vence, estado)
    private val hoy = "2026-11-11"
    private val limite = "2026-11-18"

    @Test
    fun rojoSiDeclaradaSinPagoYaVencio() =
        assertEquals(Semaforo.ROJO, semaforo(listOf(o("2026-11-01", "DECLARADO"), o("2026-11-13", "PENDIENTE")), hoy, limite))

    @Test
    fun pagoParcialVencidoEsRojo() =
        assertEquals(Semaforo.ROJO, semaforo(listOf(o("2026-11-01", "PAGO_PARCIAL")), hoy, limite))

    @Test
    fun sinDeclararPasadaNoEsVencidaPeroNoEsVerde() {
        assertEquals(Semaforo.AMARILLO, semaforo(listOf(o("2026-11-01", "PENDIENTE")), hoy, limite))
        assertEquals("PENDIENTE", estadoVisible(o("2026-11-01", "PENDIENTE"), hoy))
    }

    @Test
    fun amarilloSiVenceEnLaSemana() =
        assertEquals(Semaforo.AMARILLO, semaforo(listOf(o("2026-11-13", "PENDIENTE")), hoy, limite))

    @Test
    fun verdeSiTodoCerradoOLejano() =
        assertEquals(Semaforo.VERDE, semaforo(listOf(o("2026-10-01", "PAGADO"), o("2026-12-02", "DECLARADO"), o("2026-12-15", "PENDIENTE")), hoy, limite))

    @Test
    fun proximoIgnoraCerradasYPasadas() {
        val lista = listOf(o("2026-11-20", "PENDIENTE"), o("2026-11-13", "PENDIENTE"), o("2026-11-12", "PAGADO"), o("2026-11-01", "PENDIENTE"))
        assertEquals("2026-11-13", proximoVencimiento(lista, hoy)?.fechaVencimiento)
    }

    @Test
    fun vencidaSeDeduceConLaFecha() {
        assertEquals("VENCIDA", estadoVisible(o("2026-11-01", "DECLARADO"), hoy))
        assertEquals("VENCIDA", estadoVisible(o("2026-11-01", "PAGO_PARCIAL"), hoy))
        assertEquals("PAGADO", estadoVisible(o("2026-11-01", "PAGADO"), hoy))
        assertEquals("PENDIENTE", estadoVisible(o("2026-11-13", "PENDIENTE"), hoy))
    }

    @Test
    fun solesConDosDecimales() = assertEquals("S/ 350.00", soles(350.0)).also { assertEquals("S/ 12.05", soles(12.05)) }
}
