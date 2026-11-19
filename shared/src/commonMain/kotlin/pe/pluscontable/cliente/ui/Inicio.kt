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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.pluscontable.cliente.domain.ApiException
import pe.pluscontable.cliente.domain.Autenticacion
import pe.pluscontable.cliente.domain.DescargarDocumento
import pe.pluscontable.cliente.domain.ObtenerPanel
import pe.pluscontable.cliente.domain.RegistrarIncidencia
import pe.pluscontable.cliente.domain.Cobro
import pe.pluscontable.cliente.domain.Fraccionamiento
import pe.pluscontable.cliente.domain.AbridorPdf
import pe.pluscontable.cliente.domain.Aviso
import pe.pluscontable.cliente.domain.Documento
import pe.pluscontable.cliente.domain.etiquetasAviso
import pe.pluscontable.cliente.domain.etiquetasDocumento
import pe.pluscontable.cliente.domain.Incidencia
import pe.pluscontable.cliente.domain.tiposIncidencia
import pe.pluscontable.cliente.domain.Obligacion
import pe.pluscontable.cliente.domain.Semaforo
import pe.pluscontable.cliente.domain.Usuario
import pe.pluscontable.cliente.domain.dentroDeSieteDias
import pe.pluscontable.cliente.domain.estadoVisible
import pe.pluscontable.cliente.domain.hoy
import pe.pluscontable.cliente.domain.proximoVencimiento
import pe.pluscontable.cliente.domain.semaforo
import pe.pluscontable.cliente.domain.soles

class InicioViewModel(
    private val auth: Autenticacion,
    private val obtenerPanel: ObtenerPanel,
    private val registrarIncidencia: RegistrarIncidencia,
    private val descargarDocumento: DescargarDocumento,
    private val abridor: AbridorPdf,
) : ViewModel() {
    var usuario by mutableStateOf<Usuario?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var sesionTerminada by mutableStateOf(false); private set
    var obligaciones by mutableStateOf<List<Obligacion>>(emptyList()); private set
    var cobros by mutableStateOf<List<Cobro>>(emptyList()); private set
    var fraccionamientos by mutableStateOf<List<Fraccionamiento>>(emptyList()); private set

    var avisos by mutableStateOf<List<Aviso>>(emptyList()); private set
    var documentos by mutableStateOf<List<Documento>>(emptyList()); private set
    var errorDocumento by mutableStateOf<String?>(null); private set
    var descargando by mutableStateOf<Long?>(null); private set

    fun abrirDocumento(d: Documento) {
        if (descargando != null) return
        descargando = d.id; errorDocumento = null
        viewModelScope.launch {
            try {
                abridor.abrir(d.nombreArchivo, descargarDocumento(d.id))
            } catch (e: ApiException) {
                if (e.noAutorizado) cerrarSesion() else errorDocumento = e.message
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorDocumento = "No se pudo abrir el PDF. ¿Tienes un visor de PDF instalado?"
            } finally {
                descargando = null
            }
        }
    }

    var incidencias by mutableStateOf<List<Incidencia>>(emptyList()); private set
    var errorIncidencia by mutableStateOf<String?>(null); private set
    var enviando by mutableStateOf(false); private set

    /** [alTerminar] se llama solo si se registró, para limpiar el formulario. */
    fun enviarIncidencia(tipo: String, descripcion: String, alTerminar: () -> Unit) {
        if (enviando) return
        enviando = true; errorIncidencia = null
        viewModelScope.launch {
            try {
                val nueva = registrarIncidencia(tipo, descripcion)
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
                val panel = obtenerPanel()
                obligaciones = panel.obligaciones
                cobros = panel.cobros
                fraccionamientos = panel.fraccionamientos
                incidencias = panel.incidencias
                avisos = panel.avisos
                documentos = panel.documentos
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
            listOf("Resumen", "Obligaciones", "Cobros", "Avisos", "Documentos", "Ayuda").forEachIndexed { i, t ->
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
                pestana == 3 -> Avisos(vm.avisos)
                pestana == 4 -> Documentos(vm)
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

@Composable
private fun Documentos(vm: InicioViewModel) {
    if (vm.documentos.isEmpty()) return Vacio("No tienes documentos archivados")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.errorDocumento?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        items(vm.documentos, key = { it.id }) { d ->
            Card(Modifier.fillMaxWidth().clickable(enabled = vm.descargando == null) { vm.abrirDocumento(d) }) {
                Column(Modifier.padding(16.dp)) {
                    Text("${etiquetasDocumento[d.tipo] ?: d.tipo} · v${d.version}", style = MaterialTheme.typography.titleSmall)
                    Text("${d.fechaEmision?.let { "Emitido $it · " }.orEmpty()}${d.tamanoBytes / 1024 + 1} KB")
                    if (vm.descargando == d.id) Text("Abriendo…")
                }
            }
        }
    }
}

@Composable
private fun Avisos(avisos: List<Aviso>) {
    if (avisos.isEmpty()) return Vacio("No tienes avisos de SUNAT")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(avisos, key = { it.id }) { a ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(etiquetasAviso[a.tipo] ?: a.tipo, style = MaterialTheme.typography.titleSmall)
                    Text("Recibido ${a.fechaRecepcion}")
                    if (a.estado == "ATENDIDA") Text("Atendido${a.fechaAtencion?.let { " el $it" }.orEmpty()}")
                    else Text("Pendiente${a.plazoRespuesta?.let { " — plazo $it" }.orEmpty()}")
                }
            }
        }
    }
}
