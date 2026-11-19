package pe.pluscontable.cliente.domain

const val MAX_DESCRIPCION = 1000

/** Todo lo que muestra la pantalla de inicio, leído de una vez. */
data class Panel(
    val obligaciones: List<Obligacion>,
    val cobros: List<Cobro>,
    val fraccionamientos: List<Fraccionamiento>,
    val incidencias: List<Incidencia>,
    val avisos: List<Aviso>,
    val documentos: List<Documento>,
)

class ObtenerPanel(private val repo: ClienteRepositorio) {
    suspend operator fun invoke() = Panel(
        repo.obligaciones(), repo.cobros(), repo.fraccionamientos(), repo.incidencias(), repo.avisos(), repo.documentos(),
    )
}

class RegistrarIncidencia(private val repo: ClienteRepositorio) {
    /** Valida antes de llamar al servidor. shortcut: el límite de [MAX_DESCRIPCION] es nuestro; confirmar el del backend. */
    suspend operator fun invoke(tipo: String, descripcion: String): Incidencia {
        val texto = descripcion.trim()
        if (tiposIncidencia.none { it.first == tipo }) throw ApiException("Elige un tipo de incidencia")
        if (texto.isEmpty()) throw ApiException("Describe el problema o la consulta")
        if (texto.length > MAX_DESCRIPCION) throw ApiException("La descripción no puede pasar de $MAX_DESCRIPCION caracteres")
        return repo.registrarIncidencia(tipo, texto)
    }
}

class DescargarDocumento(private val repo: ClienteRepositorio) {
    suspend operator fun invoke(id: Long) = repo.descargarDocumento(id)
}
