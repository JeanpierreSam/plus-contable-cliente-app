package pe.pluscontable.cliente.datos

import pe.pluscontable.cliente.db.ClienteDb

/** [DatosCliente] sobre la base SQLite de la app. La primera vez se llena con datos de ejemplo. */
class DatosLocal(db: ClienteDb) : DatosCliente {
    private val q = db.esquemaQueries

    init {
        if (q.totalObligaciones().executeAsOne() == 0L) sembrar()
    }

    override suspend fun obligaciones() = q.obligaciones { id, tipo, periodo, vence, estado ->
        Obligacion(id, tipo, periodo, vence, estado)
    }.executeAsList()

    override suspend fun cobros() = q.cobros { id, fecha, monto, estado -> Cobro(id, fecha, monto, estado) }.executeAsList()

    override suspend fun fraccionamientos(): List<Fraccionamiento> {
        val cuotas = q.cuotas { id, fraccId, numero, vence, monto, estado, pago, riesgo ->
            fraccId to Cuota(id, numero.toInt(), vence, monto, estado, pago, riesgo != 0L)
        }.executeAsList().groupBy({ it.first }, { it.second })
        return q.fraccionamientos { id, tributo, total, n, primera ->
            Fraccionamiento(id, tributo, total, n.toInt(), primera, cuotas[id].orEmpty())
        }.executeAsList()
    }

    override suspend fun documentos() = q.documentos { id, tipo, version, emision, nombre, tam ->
        Documento(id, tipo, version.toInt(), emision, nombre, tam)
    }.executeAsList()

    // shortcut: PDF de una página generado aquí; con la API se descarga el archivo real.
    override suspend fun descargarDocumento(id: Long): ByteArray {
        val d = documentos().first { it.id == id }
        return pdfDeEjemplo(etiquetasDocumento[d.tipo] ?: d.tipo).encodeToByteArray()
    }

    override suspend fun incidencias() = q.incidencias { id, tipo, fecha, desc, en -> Incidencia(id, tipo, fecha, desc, en) }.executeAsList()

    override suspend fun registrarIncidencia(tipo: String, descripcion: String): Incidencia {
        val fecha = hoy()
        q.insertarIncidencia(tipo, fecha, descripcion.trim(), fecha)
        return incidencias().first()
    }

    // shortcut: datos de ejemplo de un solo cliente; desaparecen cuando los datos vengan de la API.
    // Reflejan el contrato: no hay VENCIDA (se deduce con la fecha) y los cobros son solo pendientes.
    private fun sembrar() = q.transaction {
        q.insertarObligacion(1, "IGV", "2026-10-01", "2026-11-13", "PENDIENTE")
        q.insertarObligacion(2, "PLAME", "2026-10-01", "2026-11-13", "PENDIENTE")
        q.insertarObligacion(3, "IGV", "2026-09-01", "2026-10-14", "PAGADO")
        q.insertarObligacion(4, "PLAME", "2026-09-01", "2026-10-14", "DECLARADO")
        q.insertarObligacion(5, "RENTA", "2026-08-01", "2026-09-15", "PENDIENTE")
        q.insertarDocumento(1, "FICHA_RUC", 1, "2026-01-10", "ficha-ruc.pdf", 400)
        q.insertarDocumento(2, "VIGENCIA_PODERES", 2, "2026-03-05", "vigencia-poderes.pdf", 400)
        q.insertarCobro(1, "2026-11-15", 350.0, "PENDIENTE")
        q.insertarCobro(2, "2026-12-15", 350.0, "PENDIENTE")
        q.insertarFraccionamiento(1, "IGV", 3000.0, 3, "2026-10-20")
        q.insertarCuota(1, 1, 1, "2026-10-20", 1000.0, "PAGADA", "2026-10-19", 0)
        q.insertarCuota(2, 1, 2, "2026-11-20", 1000.0, "PENDIENTE", null, 0)
        q.insertarCuota(3, 1, 3, "2026-12-20", 1000.0, "PENDIENTE", null, 0)
    }
}

private fun pdfDeEjemplo(titulo: String): String {
    val contenido = "BT /F1 18 Tf 72 720 Td ($titulo - documento de ejemplo) Tj ET"
    val objetos = listOf(
        "<< /Type /Catalog /Pages 2 0 R >>",
        "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
        "<< /Length ${contenido.length} >>\nstream\n$contenido\nendstream",
        "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
    )
    val sb = StringBuilder("%PDF-1.4\n")
    val pos = objetos.mapIndexed { i, o -> sb.length.also { sb.append("${i + 1} 0 obj\n$o\nendobj\n") } }
    val xref = sb.length
    sb.append("xref\n0 ${objetos.size + 1}\n0000000000 65535 f \n")
    pos.forEach { sb.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
    sb.append("trailer\n<< /Size ${objetos.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF")
    return sb.toString()
}
