package pe.pluscontable.cliente.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class RepoFalso : ClienteRepositorio {
    val registradas = mutableListOf<Pair<String, String>>()
    override suspend fun obligaciones() = listOf(Obligacion(1, "IGV", "2026-10-01", "2026-11-13", "DECLARADO"))
    override suspend fun cobros() = listOf(Cobro(1, "2026-11-15", 350.0, "PENDIENTE"))
    override suspend fun fraccionamientos() = emptyList<Fraccionamiento>()
    override suspend fun avisos() = emptyList<Aviso>()
    override suspend fun documentos() = emptyList<Documento>()
    override suspend fun descargarDocumento(id: Long) = byteArrayOf(id.toByte())
    override suspend fun incidencias() = emptyList<Incidencia>()
    override suspend fun registrarIncidencia(tipo: String, descripcion: String): Incidencia {
        registradas += tipo to descripcion
        return Incidencia(9, tipo, "2026-11-18", descripcion)
    }
}

class CasosDeUsoTest {
    private val repo = RepoFalso()

    @Test
    fun panelJuntaLoQueDevuelveElRepositorio() = runTest {
        val p = ObtenerPanel(repo)()
        assertEquals(1, p.obligaciones.size)
        assertEquals(350.0, p.cobros.single().monto)
        assertTrue(p.documentos.isEmpty())
    }

    @Test
    fun incidenciaValidaSeRecortaYSeRegistra() = runTest {
        val i = RegistrarIncidencia(repo)("OTRO", "  Falta mi PLAME  ")
        assertEquals("Falta mi PLAME", i.descripcion)
        assertEquals(listOf("OTRO" to "Falta mi PLAME"), repo.registradas)
    }

    @Test
    fun incidenciaInvalidaNoLlegaAlRepositorio() = runTest {
        val caso = RegistrarIncidencia(repo)
        assertFailsWith<ApiException> { caso("OTRO", "   ") }
        assertFailsWith<ApiException> { caso("INVENTADO", "hola") }
        assertFailsWith<ApiException> { caso("OTRO", "x".repeat(MAX_DESCRIPCION + 1)) }
        assertTrue(repo.registradas.isEmpty())
    }

    @Test
    fun descargaPidePorId() = runTest { assertEquals(7, DescargarDocumento(repo)(7).single().toInt()) }
}
