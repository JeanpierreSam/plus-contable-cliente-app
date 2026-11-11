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

    // shortcut: datos de ejemplo de un solo cliente; desaparecen cuando los datos vengan de la API.
    // Reflejan el contrato: no hay VENCIDA (se deduce con la fecha) y los cobros son solo pendientes.
    private fun sembrar() = q.transaction {
        q.insertarObligacion(1, "IGV", "2026-10-01", "2026-11-13", "PENDIENTE")
        q.insertarObligacion(2, "PLAME", "2026-10-01", "2026-11-13", "PENDIENTE")
        q.insertarObligacion(3, "IGV", "2026-09-01", "2026-10-14", "PAGADO")
        q.insertarObligacion(4, "PLAME", "2026-09-01", "2026-10-14", "DECLARADO")
        q.insertarObligacion(5, "RENTA", "2026-08-01", "2026-09-15", "PENDIENTE")
        q.insertarCobro(1, "2026-11-15", 350.0, "PENDIENTE")
        q.insertarCobro(2, "2026-12-15", 350.0, "PENDIENTE")
        q.insertarFraccionamiento(1, "IGV", 3000.0, 3, "2026-10-20")
        q.insertarCuota(1, 1, 1, "2026-10-20", 1000.0, "PAGADA", "2026-10-19", 0)
        q.insertarCuota(2, 1, 2, "2026-11-20", 1000.0, "PENDIENTE", null, 0)
        q.insertarCuota(3, 1, 3, "2026-12-20", 1000.0, "PENDIENTE", null, 0)
    }
}
