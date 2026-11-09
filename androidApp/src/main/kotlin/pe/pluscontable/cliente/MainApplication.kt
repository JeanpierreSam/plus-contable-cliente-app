package pe.pluscontable.cliente

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // security-crypto está deprecado sin reemplazo oficial; sigue cifrando con Android Keystore.
        @Suppress("DEPRECATION")
        val prefs = EncryptedSharedPreferences.create(
            this,
            "sesion",
            MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        startKoin {
            modules(
                module {
                    single<Settings> { SharedPreferencesSettings(prefs) }
                    single<HttpClientEngine> { OkHttp.create() }
                },
                moduloApp(BuildConfig.AUTH_URL),
            )
        }
    }
}
