package pt.umaia.beachday.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import pt.umaia.beachday.data.Sincronizacao

class ContaViewModel(application: Application) : AndroidViewModel(application) {
    private val sincronizacao = Sincronizacao(application)

    val email = MutableStateFlow(sincronizacao.emailAtual)
    val aCarregar = MutableStateFlow(false)
    val mensagem = MutableStateFlow<String?>(null)

    fun entrar(emailIntroduzido: String, password: String) {
        if (!dadosValidos(emailIntroduzido, password)) return

        executar {
            sincronizacao.entrar(emailIntroduzido.trim(), password)
            email.value = sincronizacao.emailAtual
            "Sessão iniciada. Os teus dados foram sincronizados."
        }
    }

    fun criarConta(emailIntroduzido: String, password: String) {
        if (!dadosValidos(emailIntroduzido, password)) return

        executar {
            sincronizacao.criarConta(emailIntroduzido.trim(), password)
            email.value = sincronizacao.emailAtual
            "Conta criada. Os teus dados já estão guardados na nuvem."
        }
    }

    fun recuperarPassword(emailIntroduzido: String) {
        if (emailIntroduzido.isBlank()) {
            mensagem.value = "Escreve o teu email primeiro."
            return
        }

        executar {
            sincronizacao.recuperarPassword(emailIntroduzido.trim())
            "Enviámos um email para recuperares a password."
        }
    }

    fun sincronizar() {
        executar {
            sincronizacao.juntarDados()
            "Dados sincronizados."
        }
    }

    fun terminarSessao() {
        sincronizacao.terminarSessao()
        email.value = null
        mensagem.value = "Sessão terminada. Os teus dados continuam neste telemóvel."
    }

    private fun dadosValidos(emailIntroduzido: String, password: String): Boolean {
        if (emailIntroduzido.isBlank() || password.isBlank()) {
            mensagem.value = "Preenche o email e a password."
            return false
        }
        return true
    }

    private fun executar(acao: suspend () -> String) {
        if (aCarregar.value) return

        viewModelScope.launch {
            aCarregar.value = true
            mensagem.value = try {
                acao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("BeachDay", "Erro na conta", e)
                traduzirErro(e)
            } finally {
                aCarregar.value = false
            }
        }
    }

    private fun traduzirErro(erro: Exception): String = when (erro) {
        is FirebaseAuthWeakPasswordException ->
            "A password tem de ter pelo menos 6 caracteres."
        is FirebaseAuthUserCollisionException ->
            "Já existe uma conta com este email."
        is FirebaseAuthInvalidUserException ->
            "Não existe nenhuma conta com este email."
        is FirebaseAuthInvalidCredentialsException ->
            "Email ou password incorretos."
        is FirebaseNetworkException ->
            "Sem ligação à internet."
        is FirebaseFirestoreException ->
            "Não foi possível sincronizar os dados (${erro.code})."
        else ->
            "Ocorreu um erro. Tenta outra vez."
    }
}
