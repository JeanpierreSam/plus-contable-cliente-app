package pe.pluscontable.cliente.datos

import pe.pluscontable.cliente.db.ClienteDb

/** [DatosCliente] sobre la base SQLite de la app. La primera vez se llena con datos de ejemplo. */
class DatosLocal(db: ClienteDb) : DatosCliente {
    private val q = db.esquemaQueries

    init {
        if (q.totalObligaciones().executeAsOne() == 0L) sembrar()
    }

    override suspend fun obligaciones() = q.obligaciones { id, tributo, periodo, vence, estado ->
        Obligacion(id, tributo, periodo, vence, estado)
    }.executeAsList()

    override suspend fun cobros() = q.cobros { id, concepto, periodo, monto, vence, estado ->
        Cobro(id, concepto, periodo, monto, vence, estado)
    }.executeAsList()

    override suspend fun cuotas(cobroId: Long) = q.cuotas(cobroId) { _, numero, vence, monto, estado ->
        Cuota(numero.toInt(), vence, monto, estado)
    }.executeAsList()

    // shortcut: datos de ejemplo de un solo cliente; desaparecen cuando los datos vengan de la API.
    private fun sembrar() = q.transaction {
        q.insertarObligacion(1, "IGV-Renta mensual", "2026-10", "2026-11-13", "PENDIENTE")
        q.insertarObligacion(2, "PLAME", "2026-10", "2026-11-13", "PENDIENTE")
        q.insertarObligacion(3, "IGV-Renta mensual", "2026-09", "2026-10-14", "PRESENTADA")
        q.insertarObligacion(4, "PLAME", "2026-09", "2026-10-14", "PRESENTADA")
        q.insertarObligacion(5, "IGV-Renta mensual", "2026-08", "2026-09-15", "VENCIDA")
        q.insertarCobro(1, "Honorarios contables", "2026-10", 350.0, "2026-11-05", "PENDIENTE")
        q.insertarCobro(2, "Honorarios contables", "2026-09", 350.0, "2026-10-05", "PAGADO")
        q.insertarCobro(3, "Regularización de planilla", "2026-08", 120.0, "2026-09-05", "PAGADO")
        q.insertarCuota(1, 1, "2026-11-05", 175.0, "PENDIENTE")
        q.insertarCuota(1, 2, "2026-12-05", 175.0, "PENDIENTE")
    }
}
