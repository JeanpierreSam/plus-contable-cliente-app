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
        assertEquals("PENDIENTE", d.obligaciones().first().estado) // más reciente primero
        assertEquals(3, d.cobros().size)
        assertEquals(listOf(1, 2), d.cuotas(1).map { it.numero })
        assertEquals(emptyList(), d.cuotas(2))
    }
}
