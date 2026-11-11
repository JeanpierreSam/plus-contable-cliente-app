package pe.pluscontable.cliente.datos

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import pe.pluscontable.cliente.db.ClienteDb
import kotlin.test.Test
import kotlin.test.assertEquals

class DatosLocalTest {
    private fun datos() = DatosLocal(ClienteDb(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { ClienteDb.Schema.create(it) }))

    @Test
    fun laBaseNuevaSeSiembraYSeLee() = runTest {
        val d = datos()
        assertEquals(5, d.obligaciones().size)
        assertEquals("2026-11-13", d.obligaciones().first().fechaVencimiento) // más reciente primero
        assertEquals(listOf("2026-11-15", "2026-12-15"), d.cobros().map { it.fechaCobro })
        val f = d.fraccionamientos().single()
        assertEquals(listOf(1, 2, 3), f.cuotas.map { it.numero })
        assertEquals("2026-10-19", f.cuotas.first().fechaPago)
    }
}
