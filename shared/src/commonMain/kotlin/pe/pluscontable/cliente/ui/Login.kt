package pe.pluscontable.cliente.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.pluscontable.cliente.datos.AuthApi

class LoginViewModel(private val auth: AuthApi) : ViewModel() {
    var usuario by mutableStateOf("")
    var clave by mutableStateOf("")
    var cargando by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set

    fun entrar(alEntrar: () -> Unit) {
        if (usuario.isBlank() || clave.isBlank()) { error = "Usuario y contraseña son obligatorios"; return }
        cargando = true; error = null
        viewModelScope.launch {
            try {
                auth.login(usuario, clave)
                clave = ""
                alEntrar()
            } catch (e: Exception) {
                error = e.message
            } finally {
                cargando = false
            }
        }
    }
}

@Composable
fun PantallaLogin(alEntrar: () -> Unit, vm: LoginViewModel = koinViewModel()) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Plus Contable", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            vm.usuario, { vm.usuario = it }, Modifier.fillMaxWidth(),
            label = { Text("Usuario") }, singleLine = true, enabled = !vm.cargando,
        )
        OutlinedTextField(
            vm.clave, { vm.clave = it }, Modifier.fillMaxWidth(),
            label = { Text("Contraseña") }, singleLine = true, enabled = !vm.cargando,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button({ vm.entrar(alEntrar) }, Modifier.fillMaxWidth(), enabled = !vm.cargando) {
            if (vm.cargando) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Ingresar")
        }
    }
}
