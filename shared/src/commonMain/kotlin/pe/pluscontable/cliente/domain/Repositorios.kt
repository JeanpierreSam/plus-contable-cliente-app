package pe.pluscontable.cliente.domain

/** Lo que la app lee y escribe del estudio. Lo implementa la capa de datos (hoy [pe.pluscontable.cliente.datos.DatosApi]). */
interface ClienteRepositorio {
    suspend fun obligaciones(): List<Obligacion>
    suspend fun cobros(): List<Cobro>
    suspend fun fraccionamientos(): List<Fraccionamiento>
    suspend fun avisos(): List<Aviso>
    suspend fun documentos(): List<Documento>
    suspend fun descargarDocumento(id: Long): ByteArray
    suspend fun incidencias(): List<Incidencia>
    /** Registra una incidencia del cliente con fecha de hoy. */
    suspend fun registrarIncidencia(tipo: String, descripcion: String): Incidencia
}

/** Sesión del cliente contra ms-auth. */
interface Autenticacion {
    suspend fun login(usuario: String, clave: String): Usuario
    suspend fun yo(): Usuario
    /** Revoca el token en el servidor si se puede; la sesión local se borra siempre. */
    suspend fun logout()
}
