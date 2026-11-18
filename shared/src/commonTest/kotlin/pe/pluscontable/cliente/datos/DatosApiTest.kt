package pe.pluscontable.cliente.datos

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Los JSON son los ejemplos de docs/contrato-api-cliente.md del backend.
@OptIn(ExperimentalEncodingApi::class)
class DatosApiTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val peticiones = mutableListOf<HttpRequestData>()

    private fun jwt(payload: String) =
        "cabecera." + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(payload.encodeToByteArray()) + ".firma"

    private fun api(token: String?, cuerpo: String = "[]", status: HttpStatusCode = HttpStatusCode.OK): DatosApi {
        val sesion = Sesion(MapSettings())
        token?.let(sesion::guardar)
        val http = crearHttpClient(MockEngine { peticiones += it; respond(cuerpo, status, json) }, "http://api.test/", sesion)
        return DatosApi(http, sesion)
    }

    private val token = jwt("""{"sub":"cliente.demo","roles":["CLIENTE_MYPE"],"clienteId":7}""")

    @Test
    fun clienteIdSaleDelJwt() {
        assertEquals(7L, clienteIdDe(token))
        assertNull(clienteIdDe(jwt("""{"sub":"estudio"}""")))
        assertNull(clienteIdDe("basura"))
    }

    @Test
    fun obligacionesPideConClienteIdYLeeElContrato() = runTest {
        val lista = api(
            token,
            """[{"id":301,"clienteId":7,"tipoObligacion":"IGV","periodo":"2026-10-01","fechaVencimiento":"2026-11-13",
                "estado":"PENDIENTE","fechaDeclaracion":null,"numeroFormulario":null,"montoDeterminado":null}]""",
        ).obligaciones()
        assertEquals("IGV", lista.single().tipoObligacion)
        assertEquals("/api/tributario/obligaciones", peticiones[0].url.encodedPath)
        assertEquals("7", peticiones[0].url.parameters["clienteId"])
    }

    @Test
    fun cobrosYFraccionamientosUsanLaRutaDelCliente() = runTest {
        assertEquals(350.0, api(token, """[{"id":55,"fechaCobro":"2026-11-15","monto":350.00,"estado":"PENDIENTE"}]""").cobros().single().monto)
        assertEquals("/api/tributario/clientes/7/cobros", peticiones[0].url.encodedPath)

        val f = api(
            token,
            """[{"id":9,"clienteId":7,"tributo":"IGV","montoTotal":3000.00,"numeroCuotas":3,"fechaPrimeraCuota":"2026-11-20",
                "cuotas":[{"id":90,"numero":1,"fechaVencimiento":"2026-11-20","monto":1000.00,"estado":"PENDIENTE","fechaPago":null,"enRiesgo":false}]}]""",
        ).fraccionamientos().single()
        assertEquals(1, f.cuotas.single().numero)
        assertEquals("/api/tributario/clientes/7/fraccionamientos", peticiones[1].url.encodedPath)
    }

    @Test
    fun sinClienteIdEnElTokenNoLlamaAlServidor() = runTest {
        val e = assertFailsWith<ApiException> { api(jwt("""{"sub":"estudio"}""")).cobros() }
        assertTrue(e.noAutorizado)
        assertTrue(peticiones.isEmpty())
    }

    @Test
    fun cuerpoVacioEnForbiddenDaMensajePropio() = runTest {
        val e = assertFailsWith<ApiException> { api(token, "", HttpStatusCode.Forbidden).obligaciones() }
        assertEquals("No tienes acceso a esta información", e.message)
        assertTrue(!e.noAutorizado)
    }

    @Test
    fun incidenciasSeListanYSeRegistranConElClienteDelToken() = runTest {
        val json = """{"id":5,"tipo":"OTRO","fecha":"2026-11-17","descripcion":"Hola","registradaPor":"cliente.demo","registradaEn":"2026-11-17T10:00:00","clientesAsociados":1,"observaciones":null}"""
        assertEquals("OTRO", api(token, "[$json]").incidencias().single().tipo)
        assertEquals("/api/tributario/clientes/7/incidencias", peticiones[0].url.encodedPath)

        assertEquals(5L, api(token, json, HttpStatusCode.Created).registrarIncidencia("OTRO", " Hola ").id)
        val p = peticiones[1]
        assertEquals("/api/tributario/incidencias", p.url.encodedPath)
        val cuerpo = (p.body as io.ktor.http.content.TextContent).text
        assertTrue(""""clienteId":7""" in cuerpo && """"descripcion":"Hola"""" in cuerpo, cuerpo)
    }

    @Test
    fun documentosSeListanYElArchivoSeDescargaComoBytes() = runTest {
        val lista = api(
            token,
            """[{"id":3,"tipo":"FICHA_RUC","version":1,"fechaEmision":null,"nombreArchivo":"ruc.pdf","tamanoBytes":2048,"subidoEn":"2026-11-01T09:00:00"}]""",
        ).documentos()
        assertEquals("ruc.pdf", lista.single().nombreArchivo)
        assertEquals("/api/tributario/clientes/7/documentos", peticiones[0].url.encodedPath)

        assertEquals("%PDF", api(token, "%PDF-1.4").descargarDocumento(3).decodeToString().take(4))
        assertEquals("/api/tributario/clientes/7/documentos/3/archivo", peticiones[1].url.encodedPath)
    }
}
