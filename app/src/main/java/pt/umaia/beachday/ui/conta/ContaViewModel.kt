package pt.umaia.beachday.ui.conta

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import pt.umaia.beachday.data.conta.Sincronizacao
import pt.umaia.beachday.data.conta.guardarPassword
import pt.umaia.beachday.data.conta.limparContaGoogle
import pt.umaia.beachday.data.conta.pedirContaGoogle

data class EstadoConta(
    val email: String? = null,
    val nome: String? = null,
    val foto: String? = null,
    val emailVerificado: Boolean = false,
    val temGoogle: Boolean = false,
    val temPassword: Boolean = false
)

/**
 * Conta que já iniciou sessão neste telemóvel.
 * [sessao] é o nome da sessão guardada: enquanto existir, muda-se para a conta sem password.
 * [sessaoTerminada] fica true quando o utilizador carrega em "Terminar sessão" nessa conta.
 */
data class ContaGuardada(
    val email: String,
    val nome: String?,
    val foto: String?,
    val google: Boolean,
    val sessao: String? = null,
    val sessaoTerminada: Boolean = false
)

/**
 * Folha que sobe do fundo:
 * ENTRAR e CRIAR têm Google e email; CONTA é para voltar a uma conta conhecida (só a password).
 */
enum class Folha { ENTRAR, CRIAR, CONTA }

/** O utilizador escolheu na janela do Google uma conta diferente da que queria. */
private class ContaGoogleDiferente(val esperada: String) : Exception()

// Em que situação aconteceu o erro, para a mensagem fazer sentido
private enum class Acao { EMAIL, GOOGLE, APAGAR, OUTRA }

class ContaViewModel(application: Application) : AndroidViewModel(application) {
    private val sincronizacao = Sincronizacao(application)
    private val preferencias = application.getSharedPreferences("contas", Context.MODE_PRIVATE)

    val estado = MutableStateFlow(lerEstado())
    val aCarregar = MutableStateFlow(false)
    val mensagem = MutableStateFlow<String?>(null)

    val painelAberto = MutableStateFlow(false)
    val folha = MutableStateFlow<Folha?>(null)

    /** Conta para onde se quer voltar (Folha.CONTA). */
    val contaAlvo = MutableStateFlow<ContaGuardada?>(null)
    val contasGuardadas = MutableStateFlow(lerContas())

    private fun lerEstado() = EstadoConta(
        email = sincronizacao.emailAtual,
        nome = sincronizacao.nomeAtual,
        foto = sincronizacao.fotoAtual,
        emailVerificado = sincronizacao.emailVerificado,
        temGoogle = sincronizacao.temGoogle,
        temPassword = sincronizacao.temPassword
    )

    private fun atualizarEstado() {
        estado.value = lerEstado()
    }

    // ---------- Painel e folha ----------

    fun abrirPainel() {
        mensagem.value = null
        painelAberto.value = true
    }

    fun fecharPainel() {
        painelAberto.value = false
        mensagem.value = null
    }

    fun abrirFolha(qual: Folha) {
        mensagem.value = null
        painelAberto.value = false
        folha.value = qual
    }

    fun fecharFolha() {
        folha.value = null
        contaAlvo.value = null
        mensagem.value = null
    }

    // ---------- Contas guardadas neste telemóvel ----------

    private fun lerContas(): List<ContaGuardada> = try {
        val lista = JSONArray(preferencias.getString("lista", "[]"))
        (0 until lista.length()).map { i ->
            val conta = lista.getJSONObject(i)
            ContaGuardada(
                email = conta.getString("email"),
                nome = conta.optString("nome").ifBlank { null },
                foto = conta.optString("foto").ifBlank { null },
                google = conta.optBoolean("google"),
                sessao = conta.optString("sessao").ifBlank { null },
                sessaoTerminada = conta.optBoolean("sessaoTerminada")
            )
        }
    } catch (e: Exception) {
        Log.w("BeachDay", "Lista de contas guardadas inválida", e)
        emptyList()
    }

    private fun gravarContas(contas: List<ContaGuardada>) {
        val lista = JSONArray()
        contas.forEach { conta ->
            lista.put(
                JSONObject()
                    .put("email", conta.email)
                    .put("nome", conta.nome.orEmpty())
                    .put("foto", conta.foto.orEmpty())
                    .put("google", conta.google)
                    .put("sessao", conta.sessao.orEmpty())
                    .put("sessaoTerminada", conta.sessaoTerminada)
            )
        }
        preferencias.edit { putString("lista", lista.toString()) }
        contasGuardadas.value = contas
    }

    /** Guarda (ou atualiza) a conta ativa na lista, com a sessão onde está. */
    private fun lembrarContaAtual(google: Boolean) {
        val email = sincronizacao.emailAtual ?: return
        val antiga = contasGuardadas.value.find { it.email.equals(email, ignoreCase = true) }
        val outras = contasGuardadas.value.filter { it !== antiga }
        val atual = ContaGuardada(
            email = email,
            nome = sincronizacao.nomeAtual ?: antiga?.nome,
            foto = sincronizacao.fotoAtual ?: antiga?.foto,
            google = google || antiga?.google == true,
            sessao = sincronizacao.sessaoAtual
        )
        gravarContas(listOf(atual) + outras)
    }

    fun removerConta(conta: ContaGuardada) {
        gravarContas(contasGuardadas.value.filter { it.email != conta.email })
    }

    private fun marcarSessaoTerminada(emails: Set<String>?) {
        gravarContas(
            contasGuardadas.value.map {
                if (emails == null || it.email in emails) it.copy(sessaoTerminada = true, sessao = null) else it
            }
        )
    }

    // ---------- Mudar de conta e iniciar sessão ----------

    /**
     * Tocar numa conta da lista:
     * - se a sessão dessa conta ainda está iniciada, muda logo (sem password);
     * - se não, pede só o que falta para ESSA conta (Google ou password).
     */
    fun mudarParaConta(context: Context, conta: ContaGuardada) {
        val sessao = sincronizacao.sessaoValida(conta.sessao, conta.email)
        when {
            sessao != null -> executar(Acao.OUTRA, fecharNoFim = true) {
                sincronizacao.mudarPara(sessao)
                "Mudaste para ${conta.email}."
            }
            conta.google -> entrarComGoogle(context, esperada = conta.email)
            else -> {
                contaAlvo.value = conta
                abrirFolha(Folha.CONTA)
            }
        }
    }

    /** "Continuar com Google". Com [esperada], só aceita essa conta Google. */
    fun entrarComGoogle(context: Context, esperada: String? = null) {
        executar(Acao.GOOGLE, fecharNoFim = true) {
            val google = pedirContaGoogle(context)
            if (esperada != null && !google.email.equals(esperada, ignoreCase = true)) {
                throw ContaGoogleDiferente(esperada)
            }
            sincronizacao.entrarComGoogle(google.idToken, google.email)
            lembrarContaAtual(google = true)
            "Sessão iniciada como ${google.email}."
        }
    }

    fun entrarComEmail(context: Context, email: String, password: String) {
        if (!dadosValidos(email, password)) return

        executar(Acao.EMAIL, fecharNoFim = true) {
            sincronizacao.entrar(email.trim(), password)
            lembrarContaAtual(google = false)
            guardarPassword(context, email.trim(), password)
            "Sessão iniciada como ${email.trim()}."
        }
    }

    fun criarConta(context: Context, email: String, password: String) {
        if (!dadosValidos(email, password)) return

        executar(Acao.EMAIL, fecharNoFim = true) {
            sincronizacao.criarConta(email.trim(), password)
            lembrarContaAtual(google = false)
            guardarPassword(context, email.trim(), password)
            "Conta criada. Enviámos um email para confirmares o endereço."
        }
    }

    // ---------- Conta ativa ----------

    fun reenviarVerificacao() {
        executar(Acao.OUTRA) {
            sincronizacao.reenviarVerificacao()
            "Enviámos outra vez o email de confirmação."
        }
    }

    fun jaVerifiquei() {
        executar(Acao.OUTRA) {
            sincronizacao.recarregar()
            if (sincronizacao.emailVerificado) "Email confirmado." else "O email ainda não está confirmado."
        }
    }

    fun recuperarPassword(email: String) {
        if (email.isBlank()) {
            mensagem.value = "Escreve o teu email primeiro."
            return
        }

        executar(Acao.EMAIL) {
            sincronizacao.recuperarPassword(email.trim())
            "Enviámos um email para recuperares a password."
        }
    }

    fun sincronizar() {
        executar(Acao.OUTRA) {
            sincronizacao.juntarDados()
            "Dados sincronizados."
        }
    }

    fun terminarSessao() {
        executar(Acao.OUTRA, fecharNoFim = true) {
            sincronizacao.emailAtual?.let { marcarSessaoTerminada(setOf(it)) }
            sincronizacao.terminarSessao()
            limparGoogle()
            "Sessão terminada. Os dados da conta ficaram guardados na nuvem."
        }
    }

    fun terminarTodas() {
        executar(Acao.OUTRA, fecharNoFim = true) {
            sincronizacao.terminarTodas()
            limparGoogle()
            marcarSessaoTerminada(null)
            "Terminaste sessão em todas as contas."
        }
    }

    fun apagarConta() {
        executar(Acao.APAGAR, fecharNoFim = true) {
            val email = sincronizacao.emailAtual
            sincronizacao.apagarConta()
            limparGoogle()
            gravarContas(contasGuardadas.value.filter { it.email != email })
            "Conta apagada."
        }
    }

    // ---------- Auxiliares ----------

    private suspend fun limparGoogle() {
        try {
            limparContaGoogle(getApplication())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("BeachDay", "Não foi possível limpar a conta escolhida", e)
        }
    }

    private fun dadosValidos(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            mensagem.value = "Preenche o email e a password."
            return false
        }
        return true
    }

    /**
     * Corre a [tarefa] e mostra a mensagem que ela devolve.
     * Com [fecharNoFim], fecha o painel e a folha quando corre bem
     * (a mensagem aparece então numa barra no fundo do ecrã).
     */
    private fun executar(
        acao: Acao,
        fecharNoFim: Boolean = false,
        tarefa: suspend () -> String
    ) {
        if (aCarregar.value) return

        viewModelScope.launch {
            aCarregar.value = true
            mensagem.value = try {
                val resultado = tarefa()
                if (fecharNoFim) {
                    folha.value = null
                    contaAlvo.value = null
                    painelAberto.value = false
                }
                resultado
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("BeachDay", "Erro na conta", e)
                traduzirErro(e, acao)
            } finally {
                aCarregar.value = false
                atualizarEstado()
            }
        }
    }

    // Devolve null quando não é preciso mostrar nada (por exemplo, o utilizador fechou a janela)
    private fun traduzirErro(erro: Exception, acao: Acao): String? = when (erro) {
        is GetCredentialCancellationException ->
            null
        is ContaGoogleDiferente ->
            "Escolheste outra conta Google. Escolhe ${erro.esperada}."
        is NoCredentialException ->
            "Não há nenhuma conta Google neste telemóvel. Adiciona uma nas definições do telemóvel."
        is FirebaseAuthRecentLoginRequiredException ->
            "Por segurança, termina sessão, inicia sessão outra vez e depois apaga a conta."
        is FirebaseAuthWeakPasswordException ->
            "A password tem de ter pelo menos 6 caracteres."
        is FirebaseAuthUserCollisionException ->
            if (acao == Acao.GOOGLE) "Este email já tem conta com password. Inicia sessão com email e password."
            else "Já existe uma conta com este email. Inicia sessão."
        is FirebaseAuthInvalidUserException ->
            "Não existe nenhuma conta com este email. Cria uma conta."
        is FirebaseAuthInvalidCredentialsException ->
            if (acao == Acao.EMAIL) "Email ou password incorretos."
            else "Não foi possível confirmar a conta Google. Tenta outra vez."
        is FirebaseNetworkException ->
            "Sem ligação à internet."
        is FirebaseFirestoreException ->
            "Não foi possível sincronizar os dados (${erro.code})."
        else ->
            "Ocorreu um erro. Tenta outra vez."
    }
}
