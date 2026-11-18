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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
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
import pe.pluscontable.cliente.datos.Fraccionamiento
import pe.pluscontable.cliente.datos.Incidencia
import pe.pluscontable.cliente.datos.tiposIncidencia
import pe.pluscontable.cliente.datos.Obligacion
import pe.pluscontable.cliente.datos.Semaforo
import pe.pluscontable.cliente.datos.Usuario
import pe.pluscontable.cliente.datos.dentroDeSieteDias
import pe.pluscontable.cliente.datos.estadoVisible
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
    var fraccionamientos by mutableStateOf<List<Fraccionamiento>>(emptyList()); private set

    var incidencias by mutableStateOf<List<Incidencia>>(emptyList()); private set
    var errorIncidencia by mutableStateOf<String?>(null); private set
    var enviando by mutableStateOf(false); private set

    /** [alTerminar] se llama solo si se registró, para limpiar el formulario. */
    fun enviarIncidencia(tipo: String, descripcion: String, alTerminar: () -> Unit) {
        if (enviando) return
        enviando = true; errorIncidencia = null
        viewModelScope.launch {
            try {
                val nueva = datos.registrarIncidencia(tipo, descripcion)
                incidencias = listOf(nueva) + incidencias
                alTerminar()
            } catch (e: ApiException) {
                if (e.noAutorizado) cerrarSesion() else errorIncidencia = e.message
            } finally {
                enviando = false
            }
        }
    }

    fun cargar() {
        error = null
        viewModelScope.launch {
            try {
                usuario = auth.yo()
                // shortcut: lecturas a SQLite en el hilo principal; son pocas filas. Con la API serán de red y suspenden.
                obligaciones = datos.obligaciones()
                cobros = datos.cobros()
                fraccionamientos = datos.fraccionamientos()
                incidencias = datos.incidencias()
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
    var pestana by rememberSaveable { mutableStateOf(0) }
    Scaffold(bottomBar = {
        NavigationBar {
            listOf("Resumen", "Obligaciones", "Cobros", "Ayuda").forEachIndexed { i, t ->
                NavigationBarItem(selected = pestana == i, onClick = { pestana = i }, icon = {}, label = { Text(t) })
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
                pestana == 2 -> Cobros(vm.cobros, vm.fraccionamientos)
                else -> Incidencias(vm)
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
                Text(if (proxima == null) "Ninguno pendiente" else "${proxima.tipoObligacion} (${proxima.periodo.take(7)}) — ${proxima.fechaVencimiento}")
            }
        }
        OutlinedButton(vm::cerrarSesion) { Text("Cerrar sesión") }
    }
}

@Composable
private fun Vacio(texto: String) = Text(texto, Modifier.padding(16.dp))

@Composable
private fun ListaObligaciones(lista: List<Obligacion>) {
    val hoy = hoy()
    if (lista.isEmpty()) return Vacio("No tienes obligaciones registradas")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(lista, key = { it.id }) { o ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${o.tipoObligacion} · ${o.periodo.take(7)}", style = MaterialTheme.typography.titleSmall)
                    Text("Vence ${o.fechaVencimiento} — ${estadoVisible(o, hoy)}")
                }
            }
        }
    }
}

@Composable
private fun Cobros(cobros: List<Cobro>, fraccionamientos: List<Fraccionamiento>) {
    if (cobros.isEmpty() && fraccionamientos.isEmpty()) return Vacio("No tienes cobros registrados")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // El backend no manda concepto ni periodo del cobro: "Honorarios" es texto fijo.
        items(cobros, key = { "c${it.id}" }) { c ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Honorarios", style = MaterialTheme.typography.titleSmall)
                    Text("${soles(c.monto)} — cobro el ${c.fechaCobro}")
                }
            }
        }
        items(fraccionamientos, key = { "f${it.id}" }) { f ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Fraccionamiento ${f.tributo} · ${soles(f.montoTotal)}", style = MaterialTheme.typography.titleSmall)
                    f.cuotas.forEach {
                        val riesgo = if (it.enRiesgo) " ⚠ en riesgo" else ""
                        Text("Cuota ${it.numero}/${f.numeroCuotas}: ${soles(it.monto)} — vence ${it.fechaVencimiento} — ${it.estado}$riesgo")
                    }
                }
            }
        }
    }
}

@Composable
private fun Incidencias(vm: InicioViewModel) {
    var tipo by rememberSaveable { mutableStateOf(tiposIncidencia.first().first) }
    var texto by rememberSaveable { mutableStateOf("") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reportar un problema o consulta", style = MaterialTheme.typography.titleMedium)
                tiposIncidencia.forEach { (valor, etiqueta) ->
                    FilterChip(selected = tipo == valor, onClick = { tipo = valor }, label = { Text(etiqueta) })
                }
                OutlinedTextField(
                    texto, { texto = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text("Cuéntanos qué pasa") }, minLines = 3,
                )
                vm.errorIncidencia?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    { vm.enviarIncidencia(tipo, texto) { texto = "" } },
                    enabled = texto.isNotBlank() && !vm.enviando,
                ) { Text(if (vm.enviando) "Enviando…" else "Enviar") }
                Text("Mis incidencias", style = MaterialTheme.typography.titleMedium)
                if (vm.incidencias.isEmpty()) Vacio("Aún no has reportado nada")
            }
        }
        items(vm.incidencias, key = { it.id }) { i ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tiposIncidencia.firstOrNull { it.first == i.tipo }?.second ?: i.tipo, style = MaterialTheme.typography.titleSmall)
                    Text("${i.fecha} — ${i.descripcion}")
                }
            }
        }
    }
}
