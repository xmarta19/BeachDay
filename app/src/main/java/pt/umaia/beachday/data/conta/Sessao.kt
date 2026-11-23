package pt.umaia.beachday.data.conta

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Utilizador da conta ativa (ou null se for convidado).
 * Avisa quando se inicia sessão, se termina sessão ou se muda de conta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun utilizadorAtual(): Flow<FirebaseUser?> = GestorContas.ativa.flatMapLatest { app ->
    if (app == null) {
        flowOf(null)
    } else {
        callbackFlow {
            val auth = FirebaseAuth.getInstance(app)
            val ouvinte = FirebaseAuth.IdTokenListener { trySend(it.currentUser) }
            auth.addIdTokenListener(ouvinte)
            awaitClose { auth.removeIdTokenListener(ouvinte) }
        }
    }
}
