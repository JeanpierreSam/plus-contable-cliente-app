package pe.pluscontable.cliente.datos

import pe.pluscontable.cliente.domain.*
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

    @Test
    fun registrarIncidenciaLaDevuelveYLaListaPrimero() = runTest {
        val d = datos()
        assertEquals(0, d.incidencias().size)
        d.registrarIncidencia("OTRO", "primera")
        val segunda = d.registrarIncidencia("PROBLEMA_SIRE", " segunda ")
        assertEquals("segunda", segunda.descripcion)
        assertEquals(listOf("segunda", "primera"), d.incidencias().map { it.descripcion })
    }

    @Test
    fun documentosDeEjemploSeDescarganComoPdf() = runTest {
        val d = datos()
        assertEquals(2, d.documentos().size)
        assertEquals("%PDF-1.4", d.descargarDocumento(1).decodeToString().take(8))
    }

    @Test
    fun avisosDeEjemploMasRecientePrimero() = runTest {
        assertEquals(listOf(1L, 2L), datos().avisos().map { it.id })
    }
}
