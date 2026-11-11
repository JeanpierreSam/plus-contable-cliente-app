package pe.pluscontable.cliente.datos

import com.russhwolf.settings.Settings
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Guarda el JWT. En Android, [Settings] va sobre EncryptedSharedPreferences. */
class Sesion(private val settings: Settings) {
    val token: String? get() = settings.getStringOrNull(CLAVE_TOKEN)

    /** Claim `clienteId` del JWT (el backend no lo devuelve en /me). Solo para pedir datos; la seguridad la aplica el servidor. */
    val clienteId: Long? get() = token?.let(::clienteIdDe)

    fun guardar(token: String) = settings.putString(CLAVE_TOKEN, token)

    fun cerrar() = settings.remove(CLAVE_TOKEN)

    private companion object { const val CLAVE_TOKEN = "jwt" }
}

@OptIn(ExperimentalEncodingApi::class)
internal fun clienteIdDe(jwt: String): Long? = runCatching {
    val payload = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(jwt.split('.')[1]).decodeToString()
    Json.parseToJsonElement(payload).jsonObject["clienteId"]?.jsonPrimitive?.longOrNull
}.getOrNull()
