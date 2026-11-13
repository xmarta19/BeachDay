package pt.umaia.beachday.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class Sincronizacao(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val baseDados = AppDatabase.obter(context)
    private val praiaDao = baseDados.praiaDao()
    private val perfilDao = baseDados.perfilDao()

    val emailAtual: String?
        get() = auth.currentUser?.email

    suspend fun entrar(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
        juntarDados()
    }

    suspend fun criarConta(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
        enviar(esperar = true)
    }

    suspend fun recuperarPassword(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun terminarSessao() {
        auth.signOut()
    }

    private fun documento(): DocumentReference? {
        val utilizador = auth.currentUser ?: return null
        return firestore
            .collection("utilizadores")
            .document(utilizador.uid)
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
