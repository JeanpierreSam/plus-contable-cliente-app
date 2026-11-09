package pe.pluscontable.cliente.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.pluscontable.cliente.datos.ApiException
import pe.pluscontable.cliente.datos.AuthApi
import pe.pluscontable.cliente.datos.Usuario

class InicioViewModel(private val auth: AuthApi) : ViewModel() {
    var usuario by mutableStateOf<Usuario?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var sesionTerminada by mutableStateOf(false); private set

    fun cargar() {
        error = null
        viewModelScope.launch {
            try {
                usuario = auth.yo()
            } catch (e: ApiException) {
                // Token vencido (8 h) o revocado: vuelve al login.
                if (e.noAutorizado) cerrarSesion() else error = e.message
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch { auth.logout(); sesionTerminada = true }
    }
}

@Composable
fun PantallaInicio(alSalir: () -> Unit, vm: InicioViewModel = koinViewModel()) {
    LaunchedEffect(Unit) { vm.cargar() }
    LaunchedEffect(vm.sesionTerminada) { if (vm.sesionTerminada) alSalir() }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val u = vm.usuario
        val error = vm.error
        when {
            u != null -> Text("Hola, ${u.nombreCompleto ?: u.username}", style = MaterialTheme.typography.headlineSmall)
            error != null -> {
                Text(error, color = MaterialTheme.colorScheme.error)
                Button(vm::cargar) { Text("Reintentar") }
            }
            else -> CircularProgressIndicator()
        }
        OutlinedButton(vm::cerrarSesion) { Text("Cerrar sesión") }
    }
}
