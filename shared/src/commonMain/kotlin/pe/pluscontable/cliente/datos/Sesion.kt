package pe.pluscontable.cliente.datos

import com.russhwolf.settings.Settings

/** Guarda el JWT. En Android, [Settings] va sobre EncryptedSharedPreferences. */
class Sesion(private val settings: Settings) {
    val token: String? get() = settings.getStringOrNull(CLAVE_TOKEN)

    fun guardar(token: String) = settings.putString(CLAVE_TOKEN, token)

    fun cerrar() = settings.remove(CLAVE_TOKEN)

    private companion object { const val CLAVE_TOKEN = "jwt" }
}
