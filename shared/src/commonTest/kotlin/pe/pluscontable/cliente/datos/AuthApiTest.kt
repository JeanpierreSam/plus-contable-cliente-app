package pe.pluscontable.cliente.datos

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthApiTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val sesion = Sesion(MapSettings())
    private val peticiones = mutableListOf<HttpRequestData>()

    private fun api(responder: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        AuthApi(crearHttpClient(MockEngine { peticiones += it; responder(it) }, "http://api.test/", sesion), sesion)

    private val usuarioJson = """{"id":7,"username":"mype1","nombreCompleto":"Bodega Rosita","roles":["CLIENTE_MYPE"]}"""

    @Test
    fun loginCorrectoGuardaTokenYLuegoLoEnvia() = runTest {
        val api = api { req ->
            if (req.url.encodedPath == "/api/auth/login") {
                respond("""{"token":"abc.def.ghi","tipo":"Bearer","expiraEnSegundos":28800,"usuario":$usuarioJson}""", headers = json)
            } else {
                respond(usuarioJson, headers = json)
            }
        }

        assertEquals("Bodega Rosita", api.login(" mype1 ", "secreta").nombreCompleto)
        assertEquals("abc.def.ghi", sesion.token)
        val cuerpo = peticiones[0].body.toByteArray().decodeToString()
        assertTrue("\"username\":\"mype1\"" in cuerpo, cuerpo)
        assertNull(peticiones[0].headers[HttpHeaders.Authorization])

        api.yo()
        assertEquals("/api/auth/me", peticiones[1].url.encodedPath)
        assertEquals("Bearer abc.def.ghi", peticiones[1].headers[HttpHeaders.Authorization])
    }

    @Test
    fun credencialesMalasDanMensajeDelServidorYNoGuardanToken() = runTest {
        val api = api { respond("""{"mensaje":"Credenciales inválidas"}""", HttpStatusCode.Unauthorized, json) }
        val e = assertFailsWith<ApiException> { api.login("mype1", "mala") }
        assertEquals("Credenciales inválidas", e.message)
        assertTrue(e.noAutorizado)
        assertNull(sesion.token)
    }

    @Test
    fun servidorCaidoDaMensajeDeConexion() = runTest {
        val api = api { throw RuntimeException("connection refused") }
        assertEquals("No se pudo conectar con el servidor", assertFailsWith<ApiException> { api.login("a", "b") }.message)
    }

    @Test
    fun logoutBorraSesionAunqueFalleElServidor() = runTest {
        sesion.guardar("viejo")
        api { respondError(HttpStatusCode.InternalServerError) }.logout()
        assertNull(sesion.token)
    }
}
