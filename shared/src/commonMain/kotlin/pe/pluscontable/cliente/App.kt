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
import pe.pluscontable.cliente.datos.AuthApi
import pe.pluscontable.cliente.datos.DatosCliente
import pe.pluscontable.cliente.datos.Sesion
import pe.pluscontable.cliente.datos.DatosLocal
import pe.pluscontable.cliente.db.ClienteDb
import pe.pluscontable.cliente.datos.crearHttpClient
import pe.pluscontable.cliente.ui.InicioViewModel
import pe.pluscontable.cliente.ui.LoginViewModel
import pe.pluscontable.cliente.ui.PantallaInicio
import pe.pluscontable.cliente.ui.PantallaLogin

@Serializable object RutaLogin
@Serializable object RutaInicio

/** La plataforma aporta HttpClientEngine y Settings (ver MainApplication en androidApp). */
fun moduloApp(authUrl: String) = module {
    single { Sesion(get()) }
    single { crearHttpClient(get<HttpClientEngine>(), authUrl, get()) }
    single { AuthApi(get(), get()) }
    single { ClienteDb(get()) }
    single<DatosCliente> { DatosLocal(get()) } // al pasar a la API: DatosApi(get())
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
