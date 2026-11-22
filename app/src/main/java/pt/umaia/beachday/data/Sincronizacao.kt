package pt.umaia.beachday.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.room.withTransaction
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class Sincronizacao(context: Context) {
    private val baseDados = AppDatabase.obter(context)
    private val praiaDao = baseDados.praiaDao()
    private val perfilDao = baseDados.perfilDao()
    private val preferencias = context.getSharedPreferences("beachday", Context.MODE_PRIVATE)

    init {
        GestorContas.iniciar(context)
    }

    // Conta ativa (muda quando se muda de conta)
    private val utilizador: FirebaseUser?
        get() = GestorContas.authAtiva?.currentUser

    val emailAtual: String?
        get() = utilizador?.email

    val nomeAtual: String?
        // Contas de email têm o nome vazio (""), não null
        get() = utilizador?.displayName?.takeIf { it.isNotBlank() }

    /** Foto da conta Google (se houver). */
    val fotoAtual: String?
        get() = utilizador?.let { conta ->
            (conta.providerData.firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }?.photoUrl
                ?: conta.photoUrl)?.toString()
        }

    val emailVerificado: Boolean
        get() = utilizador?.isEmailVerified == true

    val temGoogle: Boolean
        get() = utilizador?.providerData.orEmpty()
            .any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

    val temPassword: Boolean
        get() = utilizador?.providerData.orEmpty()
            .any { it.providerId == EmailAuthProvider.PROVIDER_ID }

    /** Nome da sessão ativa, para a lista de contas saber onde está cada uma. */
    val sessaoAtual: String?
        get() = GestorContas.ativa.value?.name

    // ---------- Iniciar sessão ----------

    suspend fun entrar(email: String, password: String) {
        iniciarSessao(email) { auth -> auth.signInWithEmailAndPassword(email, password).await() }
    }

    suspend fun entrarComGoogle(idToken: String, email: String) {
        iniciarSessao(email) { auth ->
            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        }
    }

    suspend fun criarConta(email: String, password: String) {
        iniciarSessao(null) { auth ->
            auth.createUserWithEmailAndPassword(email, password).await()
            auth.currentUser?.sendEmailVerification()?.await()
        }
    }

    /**
     * Inicia sessão numa conta sem mexer nas outras:
     * usa a sessão que já existia para esse email ou cria uma nova.
     * Se falhar (password errada...), a conta ativa continua a mesma.
     */
    private suspend fun iniciarSessao(email: String?, entrar: suspend (FirebaseAuth) -> Unit) {
        val existente = email?.let { GestorContas.sessaoCom(it) }
        val app = existente ?: GestorContas.novaSessao()
        try {
            entrar(FirebaseAuth.getInstance(app))
        } catch (e: Exception) {
            if (existente == null) GestorContas.esquecer(app)
            throw e
        }
        mudarPara(app)
    }

    /** Sessão que ainda está iniciada para esta conta (para mudar sem password). */
    fun sessaoValida(nome: String?, email: String): FirebaseApp? {
        if (nome == null) return null
        val app = GestorContas.todas().find { it.name == nome } ?: return null
        val conta = FirebaseAuth.getInstance(app).currentUser ?: return null
        return if (conta.email.equals(email, ignoreCase = true)) app else null
    }

    /**
     * Muda a conta ativa sem perder dados:
     * 1. Envia para a nuvem os dados da conta que estava ativa.
     * 2. Ativa a outra conta.
     * 3. Limpa do telemóvel os dados da conta anterior.
     * 4. Junta os dados da conta nova (e os do convidado, se não havia conta).
     */
    suspend fun mudarPara(app: FirebaseApp) {
        val anterior = GestorContas.ativa.value
        if (anterior?.name == app.name) {
            juntarDados()
            return
        }
        val tinhaConta = utilizador != null
        if (tinhaConta) enviarComLimite()
        GestorContas.ativar(app)
        if (tinhaConta) limparDadosLocais()
        juntarDados()
    }

    private suspend fun enviarComLimite() {
        try {
            withTimeout(5.seconds) { enviar(esperar = true) }
        } catch (e: TimeoutCancellationException) {
            // Sem rede: o Firestore guarda o envio e manda-o mais tarde
            Log.i("BeachDay", "Envio para a nuvem adiado", e)
        }
    }

    // ---------- Conta ativa ----------

    suspend fun reenviarVerificacao() {
        utilizador?.sendEmailVerification()?.await()
    }

    /** Vai buscar ao Firebase o estado mais recente (por exemplo, se o email já foi verificado). */
    suspend fun recarregar() {
        utilizador?.reload()?.await()
    }

    /** Apaga a cópia no Firestore, a conta no Firebase e os dados da conta neste telemóvel. */
    suspend fun apagarConta() {
        val app = GestorContas.ativa.value ?: return
        val conta = utilizador ?: return
        documento()?.delete()?.await()
        try {
            conta.delete().await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            // A conta não foi apagada: volta a pôr a cópia na nuvem
            enviar(esperar = true)
            throw e
        }
        GestorContas.ativar(null)
        GestorContas.esquecer(app)
        limparDadosLocais()
    }

    suspend fun recuperarPassword(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
    }

    /**
     * Termina a sessão da conta ativa. Os dados ficam na nuvem
     * e o telemóvel volta ao modo convidado.
     */
    suspend fun terminarSessao() {
        val app = GestorContas.ativa.value ?: return
        enviarComLimite()
        FirebaseAuth.getInstance(app).signOut()
        GestorContas.ativar(null)
        GestorContas.esquecer(app)
        limparDadosLocais()
    }

    /** Termina a sessão em todas as contas guardadas neste telemóvel. */
    suspend fun terminarTodas() {
        if (utilizador != null) terminarSessao()
        GestorContas.todas().forEach { app ->
            try {
                FirebaseAuth.getInstance(app).signOut()
                GestorContas.esquecer(app)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Se uma falhar, continua com as outras
                Log.w("BeachDay", "Não foi possível terminar a sessão ${app.name}", e)
            }
        }
    }

    private suspend fun limparDadosLocais() {
        baseDados.withTransaction {
            praiaDao.limparFavoritas()
            praiaDao.apagarVisitas()
            perfilDao.apagarTodos()
        }
        preferencias.edit { putInt("idPerfil", 0) }
    }

    private fun documento(): DocumentReference? {
        val app = GestorContas.ativa.value ?: return null
        val conta = utilizador ?: return null
        return FirebaseFirestore.getInstance(app)
            .collection("utilizadores")
            .document(conta.uid)
            .collection("dados")
            .document("copia")
    }

    suspend fun juntarDados() {
        val documento = documento() ?: return
        val remoto = documento.get().await()

        if (remoto.exists()) {
            (remoto.get("favoritas") as? List<*>)?.forEach { valor ->
                (valor as? Number)?.toInt()?.let { praiaDao.definirFavorita(it, true) }
            }

            val visitasLocais = praiaDao.todasVisitas()
            (remoto.get("visitas") as? List<*>)?.forEach { valor ->
                val mapa = valor as? Map<*, *> ?: return@forEach
                val idPraia = (mapa["idPraia"] as? Number)?.toInt() ?: return@forEach
                val data = (mapa["data"] as? Number)?.toLong() ?: return@forEach
                val jaExiste = visitasLocais.any { it.idPraia == idPraia && it.data == data }

                if (!jaExiste) {
                    praiaDao.inserirVisita(
                        Visita(
                            idPraia = idPraia,
                            data = data,
                            nota = (mapa["nota"] as? Number)?.toInt() ?: 3,
                            comentario = mapa["comentario"] as? String
                        )
                    )
                }
            }

            val nomesLocais = perfilDao.lista().map { it.nome }
            (remoto.get("perfis") as? List<*>)?.forEach { valor ->
                val mapa = valor as? Map<*, *> ?: return@forEach
                val nome = mapa["nome"] as? String ?: return@forEach
                if (nome in nomesLocais) return@forEach

                val base = PerfilUtilizador(nome = nome)
                perfilDao.guardar(
                    PerfilUtilizador(
                        nome = nome,
                        temperaturaMinima = (mapa["temperaturaMinima"] as? Number)?.toDouble()
                            ?: base.temperaturaMinima,
                        chuvaMaxima = (mapa["chuvaMaxima"] as? Number)?.toDouble()
                            ?: base.chuvaMaxima,
                        ventoMaximo = (mapa["ventoMaximo"] as? Number)?.toInt()
                            ?: base.ventoMaximo,
                        marMinimo = (mapa["marMinimo"] as? Number)?.toDouble(),
                        marMaximo = (mapa["marMaximo"] as? Number)?.toDouble()
                            ?: base.marMaximo,
                        aguaMinima = (mapa["aguaMinima"] as? Number)?.toDouble()
                            ?: base.aguaMinima
                    )
                )
            }
        }

        enviar(esperar = true)
    }

    suspend fun enviar(esperar: Boolean) {
        val documento = documento() ?: return

        val dados = mapOf(
            "favoritas" to praiaDao.idsFavoritas(),
            "visitas" to praiaDao.todasVisitas().map {
                mapOf(
                    "idPraia" to it.idPraia,
                    "data" to it.data,
                    "nota" to it.nota,
                    "comentario" to it.comentario
                )
            },
            "perfis" to perfilDao.lista().map {
                mapOf(
                    "nome" to it.nome,
                    "temperaturaMinima" to it.temperaturaMinima,
                    "chuvaMaxima" to it.chuvaMaxima,
                    "ventoMaximo" to it.ventoMaximo,
                    "marMinimo" to it.marMinimo,
                    "marMaximo" to it.marMaximo,
                    "aguaMinima" to it.aguaMinima
                )
            },
            "atualizadoEm" to System.currentTimeMillis()
        )

        val tarefa = documento.set(dados)
        if (esperar) tarefa.await()
    }
}
