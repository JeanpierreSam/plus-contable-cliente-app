package pe.pluscontable.cliente

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import pe.pluscontable.cliente.datos.AuthApi
import pe.pluscontable.cliente.datos.DatosApi
import pe.pluscontable.cliente.datos.Sesion
import pe.pluscontable.cliente.datos.crearHttpClient
import pe.pluscontable.cliente.domain.Autenticacion
import pe.pluscontable.cliente.domain.ClienteRepositorio
import pe.pluscontable.cliente.domain.DescargarDocumento
import pe.pluscontable.cliente.domain.ObtenerPanel
import pe.pluscontable.cliente.domain.RegistrarIncidencia
import pe.pluscontable.cliente.ui.InicioViewModel
import pe.pluscontable.cliente.ui.LoginViewModel
import pe.pluscontable.cliente.ui.PantallaInicio
import pe.pluscontable.cliente.ui.PantallaLogin

@Serializable object RutaLogin
@Serializable object RutaInicio

private val TRIBUTARIO = named("tributario")

/** La plataforma aporta HttpClientEngine y Settings (ver MainApplication en androidApp). Cada servicio tiene su URL base. */
fun moduloApp(authUrl: String, tributarioUrl: String) = module {
    single { Sesion(get()) }
    single { crearHttpClient(get<HttpClientEngine>(), authUrl, get()) }
    single(TRIBUTARIO) { crearHttpClient(get<HttpClientEngine>(), tributarioUrl, get()) }
    single<Autenticacion> { AuthApi(get(), get()) }
    single<ClienteRepositorio> { DatosApi(get(TRIBUTARIO), get()) }
    factoryOf(::ObtenerPanel)
    factoryOf(::RegistrarIncidencia)
    factoryOf(::DescargarDocumento)
    viewModelOf(::LoginViewModel)
    viewModelOf(::InicioViewModel)
}

@Composable
fun App() {
    val sesion = koinInject<Sesion>()
    MaterialTheme {
        Surface {
            val nav = rememberNavController()
            NavHost(nav, startDestination = if (sesion.token != null) RutaInicio else RutaLogin) {
                composable<RutaLogin> {
                    PantallaLogin(alEntrar = { nav.navigate(RutaInicio) { popUpTo(RutaLogin) { inclusive = true } } })
                }
                composable<RutaInicio> {
                    PantallaInicio(alSalir = { nav.navigate(RutaLogin) { popUpTo(RutaInicio) { inclusive = true } } })
                }
            }
        }
    }
}
