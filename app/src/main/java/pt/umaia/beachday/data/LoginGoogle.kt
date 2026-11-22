package pt.umaia.beachday.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import pt.umaia.beachday.R

/** Conta escolhida na janela do Android: uma conta Google ou um email com password guardada. */
sealed interface ContaEscolhida {
    data class Google(val idToken: String, val email: String) : ContaEscolhida
    data class Password(val email: String, val password: String) : ContaEscolhida
}

/**
 * Abre a janela de contas do próprio Android (a mesma que as apps da Google usam).
 * Mostra as contas Google do telemóvel e, se [incluirPasswords], os emails
 * com password guardados no Gestor de Passwords da Google.
 * O context tem de ser a Activity (no Compose: LocalContext.current).
 */
suspend fun escolherConta(context: Context, incluirPasswords: Boolean = true): ContaEscolhida {
    val opcaoGoogle = GetGoogleIdOption.Builder()
        .setServerClientId(context.getString(R.string.default_web_client_id))
        .setFilterByAuthorizedAccounts(false)
        .build()

    val pedido = GetCredentialRequest.Builder()
        .addCredentialOption(opcaoGoogle)
        .apply { if (incluirPasswords) addCredentialOption(GetPasswordOption()) }
        .build()

    val credencial = CredentialManager.create(context).getCredential(context, pedido).credential

    return when {
        credencial is PasswordCredential ->
            ContaEscolhida.Password(credencial.id, credencial.password)

        credencial is CustomCredential &&
            credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
            GoogleIdTokenCredential.createFrom(credencial.data).let {
                // O "id" da credencial Google é o email da conta
                ContaEscolhida.Google(it.idToken, it.id)
            }

        else -> throw IllegalStateException("Tipo de conta desconhecido.")
    }
}

/** Só contas Google (para o botão "Continuar com Google"). */
suspend fun pedirContaGoogle(context: Context): ContaEscolhida.Google =
    escolherConta(context, incluirPasswords = false) as ContaEscolhida.Google

/**
 * Pergunta ao utilizador se quer guardar o email e a password no Gestor de Passwords,
 * para da próxima vez aparecerem na janela de contas. Se recusar, não faz mal.
 */
suspend fun guardarPassword(context: Context, email: String, password: String) {
    try {
        CredentialManager.create(context).createCredential(context, CreatePasswordRequest(email, password))
    } catch (e: CreateCredentialException) {
        Log.i("BeachDay", "Password não guardada: ${e.type}")
    }
}

/** Esquece a conta escolhida, para na próxima vez voltar a perguntar. */
suspend fun limparContaGoogle(context: Context) {
    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
}
