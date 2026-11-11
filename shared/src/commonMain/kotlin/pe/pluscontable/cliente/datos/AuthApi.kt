package pe.pluscontable.cliente.datos

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// DTO copiados de ms-auth (dto/LoginRequest, LoginResponse, UserDto).
@Serializable data class LoginRequest(val username: String, val password: String)

@Serializable
data class Usuario(
    val id: Long,
    val username: String,
    val nombreCompleto: String? = null,
    val roles: List<String> = emptyList(),
)

@Serializable data class LoginResponse(val token: String, val tipo: String, val expiraEnSegundos: Long, val usuario: Usuario)

@Serializable private data class ErrorApi(val mensaje: String = "")

/** Error de la API con un mensaje listo para mostrar. [noAutorizado]: el token o las credenciales no sirven. */
class ApiException(mensaje: String, val noAutorizado: Boolean = false) : Exception(mensaje)

fun crearHttpClient(engine: HttpClientEngine, baseUrl: String, sesion: Sesion) = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    // defaultRequest se evalúa en cada petición: siempre manda el token vigente.
    defaultRequest {
        url(baseUrl)
        sesion.token?.let { bearerAuth(it) }
    }
}

class AuthApi(private val http: HttpClient, private val sesion: Sesion) {

    suspend fun login(usuario: String, clave: String): Usuario = traducirErrores {
        val r: LoginResponse = http.post("api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(usuario.trim(), clave))
        }.body()
        sesion.guardar(r.token)
        r.usuario
    }

    suspend fun yo(): Usuario = traducirErrores { http.get("api/auth/me").body() }

    /** Revoca el token en el servidor si se puede; la sesión local se borra siempre. */
    suspend fun logout() {
        try {
            http.post("api/auth/logout")
        } catch (e: CancellationException) {
            sesion.cerrar(); throw e
        } catch (_: Exception) {
        }
        sesion.cerrar()
    }
}

internal suspend fun <T> traducirErrores(bloque: suspend () -> T): T = try {
    bloque()
} catch (e: ResponseException) {
    val status = e.response.status
    // 401 y 403 llegan sin cuerpo en ms-tributario: el mensaje es nuestro.
    val mensaje = try { e.response.body<ErrorApi>().mensaje } catch (_: Exception) { "" }
    throw if (status == HttpStatusCode.Unauthorized) {
        ApiException(mensaje.ifBlank { "Usuario o contraseña incorrectos" }, noAutorizado = true)
    } else if (status == HttpStatusCode.Forbidden) {
        ApiException(mensaje.ifBlank { "No tienes acceso a esta información" })
    } else {
        ApiException(mensaje.ifBlank { "Error del servidor (${status.value})" })
    }
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    throw ApiException("No se pudo conectar con el servidor")
}
