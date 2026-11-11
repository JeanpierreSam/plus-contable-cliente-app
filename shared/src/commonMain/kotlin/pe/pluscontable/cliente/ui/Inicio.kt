package pe.pluscontable.cliente.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.pluscontable.cliente.datos.ApiException
import pe.pluscontable.cliente.datos.AuthApi
import pe.pluscontable.cliente.datos.Cobro
import pe.pluscontable.cliente.datos.DatosCliente
import pe.pluscontable.cliente.datos.Cuota
import pe.pluscontable.cliente.datos.Obligacion
import pe.pluscontable.cliente.datos.Semaforo
import pe.pluscontable.cliente.datos.Usuario
import pe.pluscontable.cliente.datos.dentroDeSieteDias
import pe.pluscontable.cliente.datos.hoy
import pe.pluscontable.cliente.datos.proximoVencimiento
import pe.pluscontable.cliente.datos.semaforo
import pe.pluscontable.cliente.datos.soles

class InicioViewModel(private val auth: AuthApi, private val datos: DatosCliente) : ViewModel() {
    var usuario by mutableStateOf<Usuario?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var sesionTerminada by mutableStateOf(false); private set
    var obligaciones by mutableStateOf<List<Obligacion>>(emptyList()); private set
    var cobros by mutableStateOf<List<Cobro>>(emptyList()); private set
    var cobroAbierto by mutableStateOf<Cobro?>(null); private set
    var cuotas by mutableStateOf<List<Cuota>>(emptyList()); private set

    fun cargar() {
        error = null
        viewModelScope.launch {
            try {
                usuario = auth.yo()
                // shortcut: lecturas a SQLite en el hilo principal; son pocas filas. Con la API serán de red y suspenden.
                obligaciones = datos.obligaciones()
                cobros = datos.cobros()
            } catch (e: ApiException) {
                // Token vencido (8 h) o revocado: vuelve al login.
                if (e.noAutorizado) cerrarSesion() else error = e.message
            }
        }
    }

    fun abrirCobro(c: Cobro?) {
        cobroAbierto = c
        if (c == null) return
        viewModelScope.launch { cuotas = datos.cuotas(c.id) }
    }

    fun cerrarSesion() {
        viewModelScope.launch { auth.logout(); sesionTerminada = true }
    }
}

@Composable
fun PantallaInicio(alSalir: () -> Unit, vm: InicioViewModel = koinViewModel()) {
    LaunchedEffect(Unit) { vm.cargar() }
    LaunchedEffect(vm.sesionTerminada) { if (vm.sesionTerminada) alSalir() }
    var pestana by rememberSaveable { mutableStateOf(0) }
    Scaffold(bottomBar = {
        NavigationBar {
            listOf("Resumen", "Obligaciones", "Cobros").forEachIndexed { i, t ->
                NavigationBarItem(selected = pestana == i, onClick = { pestana = i; vm.abrirCobro(null) }, icon = {}, label = { Text(t) })
            }
        }
    }) { margen ->
        Box(Modifier.padding(margen).fillMaxSize().padding(16.dp)) {
            val error = vm.error
            when {
                error != null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Button(vm::cargar) { Text("Reintentar") }
                }
                vm.usuario == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                pestana == 0 -> Resumen(vm)
                pestana == 1 -> ListaObligaciones(vm.obligaciones)
                vm.cobroAbierto != null -> DetalleCobro(vm.cobroAbierto!!, vm.cuotas) { vm.abrirCobro(null) }
                else -> ListaCobros(vm.cobros, vm::abrirCobro)
            }
        }
    }
}

@Composable
private fun Resumen(vm: InicioViewModel) {
    val hoy = hoy()
    val estado = semaforo(vm.obligaciones, hoy, dentroDeSieteDias())
    val proxima = proximoVencimiento(vm.obligaciones, hoy)
    val (color, texto) = when (estado) {
        Semaforo.VERDE -> Color(0xFF2E7D32) to "Estás al día"
        Semaforo.AMARILLO -> Color(0xFFF9A825) to "Tienes algo por vencer esta semana"
        Semaforo.ROJO -> Color(0xFFC62828) to "Tienes obligaciones vencidas"
    }
    val u = vm.usuario
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Hola, ${u?.nombreCompleto ?: u?.username}", style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(color))
            Text(texto, style = MaterialTheme.typography.titleMedium)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Próximo vencimiento", style = MaterialTheme.typography.labelLarge)
                Text(if (proxima == null) "Ninguno pendiente" else "${proxima.tributo} (${proxima.periodo}) — ${proxima.vence}")
            }
        }
        OutlinedButton(vm::cerrarSesion) { Text("Cerrar sesión") }
    }
}

@Composable
private fun Vacio(texto: String) = Text(texto, Modifier.padding(16.dp))

@Composable
private fun ListaObligaciones(lista: List<Obligacion>) {
    if (lista.isEmpty()) return Vacio("No tienes obligaciones registradas")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(lista, key = { it.id }) { o ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${o.tributo} · ${o.periodo}", style = MaterialTheme.typography.titleSmall)
                    Text("Vence ${o.vence} — ${o.estado}")
                }
            }
        }
    }
}

@Composable
private fun ListaCobros(lista: List<Cobro>, alAbrir: (Cobro) -> Unit) {
    if (lista.isEmpty()) return Vacio("No tienes cobros registrados")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(lista, key = { it.id }) { c ->
            Card(Modifier.fillMaxWidth().clickable { alAbrir(c) }) {
                Column(Modifier.padding(16.dp)) {
                    Text("${c.concepto} · ${c.periodo}", style = MaterialTheme.typography.titleSmall)
                    Text("${soles(c.monto)} — vence ${c.vence} — ${c.estado}")
                }
            }
        }
    }
}

@Composable
private fun DetalleCobro(c: Cobro, cuotas: List<Cuota>, alVolver: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(alVolver) { Text("← Cobros") }
        Text("${c.concepto} · ${c.periodo}", style = MaterialTheme.typography.titleMedium)
        Text("${soles(c.monto)} — ${c.estado}")
        Text("Cuotas", style = MaterialTheme.typography.labelLarge)
        if (cuotas.isEmpty()) Text("Sin fraccionamiento")
        cuotas.forEach { Text("Cuota ${it.numero}: ${soles(it.monto)} — vence ${it.vence} — ${it.estado}") }
    }
}
