package pe.pluscontable.cliente

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import pe.pluscontable.cliente.domain.AbridorPdf
import java.io.File

/** Guarda el PDF en la caché de la app y lo abre con el visor del teléfono. */
class AbridorPdfAndroid(private val contexto: Context) : AbridorPdf {
    override fun abrir(nombre: String, bytes: ByteArray) {
        val dir = File(contexto.cacheDir, "pdf").apply { mkdirs() }
        // El nombre viene del servidor: solo su parte final y sin caracteres raros.
        val archivo = File(dir, nombre.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "documento.pdf" })
        archivo.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
        contexto.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
