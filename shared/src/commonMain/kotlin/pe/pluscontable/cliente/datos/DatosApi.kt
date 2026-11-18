package pe.pluscontable.cliente.datos

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** [DatosCliente] sobre ms-tributario. [http] debe apuntar a su URL base (puerto distinto al de ms-auth). */
class DatosApi(private val http: HttpClient, private val sesion: Sesion) : DatosCliente {
    // Se lee fuera de traducirErrores, que convertiría esta excepción en "No se pudo conectar".
    // El clienteId sale del JWT; si falta, el servidor respondería 403 de todos modos.
    private fun clienteId() = sesion.clienteId ?: throw ApiException("Tu cuenta no está asociada a un cliente", noAutorizado = true)

    override suspend fun obligaciones(): List<Obligacion> {
        val id = clienteId()
        return traducirErrores { http.get("api/tributario/obligaciones") { parameter("clienteId", id) }.body() }
    }

    override suspend fun cobros(): List<Cobro> {
        val id = clienteId()
        return traducirErrores { http.get("api/tributario/clientes/$id/cobros").body() }
    }

    override suspend fun fraccionamientos(): List<Fraccionamiento> {
        val id = clienteId()
        return traducirErrores { http.get("api/tributario/clientes/$id/fraccionamientos").body() }
    }

    override suspend fun incidencias(): List<Incidencia> {
        val id = clienteId()
        return traducirErrores { http.get("api/tributario/clientes/$id/incidencias").body() }
    }

    override suspend fun registrarIncidencia(tipo: String, descripcion: String): Incidencia {
        val id = clienteId()
        return traducirErrores {
            http.post("api/tributario/incidencias") {
                contentType(ContentType.Application.Json)
                setBody(IncidenciaNueva(tipo, id, hoy(), descripcion.trim()))
            }.body()
        }
    }
}
