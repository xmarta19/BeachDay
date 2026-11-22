package pt.umaia.beachday.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Várias contas com sessão iniciada ao mesmo tempo, como nas apps da Google.
 *
 * O Firebase só guarda uma sessão por "FirebaseApp". Por isso cada conta tem a sua
 * própria FirebaseApp (conta_xxxx), com a sessão guardada no telemóvel.
 * Mudar de conta é só escolher qual delas está ativa: não é preciso password.
 */
// O contexto guardado é sempre o da aplicação (applicationContext), que vive tanto como a app:
// não prende nenhum ecrã, por isso aqui não há fuga de memória.
@SuppressLint("StaticFieldLeak")
object GestorContas {
    private lateinit var contexto: Context
    private lateinit var preferencias: SharedPreferences

    private val _ativa = MutableStateFlow<FirebaseApp?>(null)

    /** Sessão ativa (null = convidado). */
    val ativa: StateFlow<FirebaseApp?> get() = _ativa

    val authAtiva: FirebaseAuth?
        get() = _ativa.value?.let { FirebaseAuth.getInstance(it) }

    @Synchronized
    fun iniciar(context: Context) {
        if (::contexto.isInitialized) return
        contexto = context.applicationContext
        preferencias = contexto.getSharedPreferences("sessoes", Context.MODE_PRIVATE)

        // Quem já tinha sessão iniciada numa versão anterior da app continua com ela
        if (!preferencias.contains("ativa") && FirebaseAuth.getInstance().currentUser != null) {
            preferencias.edit {
                putString("ativa", FirebaseApp.DEFAULT_APP_NAME)
                putStringSet("apps", setOf(FirebaseApp.DEFAULT_APP_NAME))
            }
        }

        _ativa.value = preferencias.getString("ativa", null)?.let { obterApp(it) }
    }

    private fun nomes(): Set<String> =
        preferencias.getStringSet("apps", emptySet()).orEmpty().toSet()

    fun obterApp(nome: String): FirebaseApp =
        FirebaseApp.getApps(contexto).find { it.name == nome }
            ?: FirebaseApp.initializeApp(contexto, FirebaseOptions.fromResource(contexto)!!, nome)

    /** Sessão já iniciada com este email, se existir. */
    fun sessaoCom(email: String): FirebaseApp? =
        nomes().map { obterApp(it) }.find { app ->
            FirebaseAuth.getInstance(app).currentUser?.email.equals(email, ignoreCase = true)
        }

    /** Espaço novo para iniciar sessão noutra conta. */
    fun novaSessao(): FirebaseApp {
        val nome = "conta_" + UUID.randomUUID().toString().take(8)
        preferencias.edit { putStringSet("apps", nomes() + nome) }
        return obterApp(nome)
    }

    fun ativar(app: FirebaseApp?) {
        preferencias.edit { putString("ativa", app?.name) }
        _ativa.value = app
    }

    /** Esquece uma sessão que já terminou. */
    fun esquecer(app: FirebaseApp) {
        preferencias.edit { putStringSet("apps", nomes() - app.name) }
        if (app.name != FirebaseApp.DEFAULT_APP_NAME) app.delete()
    }

    /** Todas as sessões guardadas (para terminar sessão em todas). */
    fun todas(): List<FirebaseApp> = nomes().map { obterApp(it) }
}
