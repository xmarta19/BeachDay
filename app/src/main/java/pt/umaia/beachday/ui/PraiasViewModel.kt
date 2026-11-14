package pt.umaia.beachday.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.umaia.beachday.data.AppDatabase
import pt.umaia.beachday.data.AvisoIpma
import pt.umaia.beachday.data.IpmaApi
import pt.umaia.beachday.data.PerfilUtilizador
import pt.umaia.beachday.data.Praia
import pt.umaia.beachday.data.PraiaRepository
import pt.umaia.beachday.data.Sincronizacao
import pt.umaia.beachday.data.estadoRede
import pt.umaia.beachday.data.marcarAvisos
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

private data class Pedido(
    val pesquisa: String,
    val regiao: String?,
    val tipo: String,
    val dia: String,
    val perfil: PerfilUtilizador?
)

class PraiasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PraiaRepository(
        application,
        AppDatabase.obter(application).praiaDao(),
        IpmaApi.criar()
    )

    private val perfilDao = AppDatabase.obter(application).perfilDao()
    private val sincronizacao = Sincronizacao(application)

    private val preferencias =
        application.getSharedPreferences("beachday", Context.MODE_PRIVATE)

    val dias: List<String> = (0..2).map { deslocamento ->
        val calendario = Calendar.getInstance()
        calendario.add(Calendar.DAY_OF_YEAR, deslocamento)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendario.time)
    }

    val pesquisa = MutableStateFlow("")
    val regiao = MutableStateFlow<String?>(null)
    val tipo = MutableStateFlow("Costeiras")
    val dia = MutableStateFlow(dias.first())
    val idPerfil = MutableStateFlow(preferencias.getInt("idPerfil", 0))

    val perfis = perfilDao.todos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val perfil = combine(idPerfil, perfis) { id, lista -> lista.find { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val avisos = MutableStateFlow<List<AvisoIpma>>(emptyList())
    val aAtualizar = MutableStateFlow(false)
    val botaoBloqueado = MutableStateFlow(false)
    val erroAtualizacao = MutableStateFlow<String?>(null)
    private var avisosCarregados = false

    val temRede = estadoRede(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val ultimaAtualizacao = repository.ultimaAtualizacao()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val praias = combine(
        combine(pesquisa, regiao, tipo) { texto, zona, filtro -> Triple(texto, zona, filtro) },
        dia,
        perfil
    ) { (texto, zona, filtro), diaEscolhido, perfilAtual ->
        Pedido(texto, zona, filtro, diaEscolhido, perfilAtual)
    }.flatMapLatest { pedido ->
        combine(
            repository.ranking(
                pedido.pesquisa, pedido.regiao, pedido.tipo,
                pedido.dia, pedido.perfil
            ),
            avisos
        ) { lista, avisosAtuais -> marcarAvisos(lista, avisosAtuais, pedido.dia) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val favoritas = combine(dia, perfil) { diaEscolhido, perfilAtual ->
        diaEscolhido to perfilAtual
    }.flatMapLatest { (diaEscolhido, perfilAtual) ->
        combine(
            repository.rankingFavoritas(diaEscolhido, perfilAtual),
            avisos
        ) { lista, avisosAtuais -> marcarAvisos(lista, avisosAtuais, diaEscolhido) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Melhor praia de cada um dos 3 dias, para o ecrã de escolha do dia
    @OptIn(ExperimentalCoroutinesApi::class)
    val melhoresPorDia = combine(tipo, perfil) { filtro, perfilAtual ->
        filtro to perfilAtual
    }.flatMapLatest { (filtro, perfilAtual) ->
        combine(
            dias.map { diaLista -> repository.ranking("", null, filtro, diaLista, perfilAtual) }
        ) { listas ->
            dias.zip(listas.toList()) { diaLista, lista ->
                diaLista to lista.firstOrNull { it.nota != null }
            }.toMap()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val visitas = repository.visitas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.importarSeNecessario()
            atualizarDados()
        }
    }

    fun nomeDia(valor: String): String = when (dias.indexOf(valor)) {
        0 -> "hoje"
        1 -> "amanhã"
        2 -> "depois de amanhã"
        else -> valor
    }

    fun escolherDia(valor: String) {
        dia.value = valor
    }

    fun escolherPerfil(id: Int) {
        idPerfil.value = id
        preferencias.edit { putInt("idPerfil", id) }
    }

    fun guardarPerfil(novo: PerfilUtilizador) {
        viewModelScope.launch {
            val id = perfilDao.guardar(novo).toInt()
            escolherPerfil(id)
            enviarParaNuvem()
        }
    }

    fun apagarPerfil(antigo: PerfilUtilizador) {
        viewModelScope.launch {
            perfilDao.apagar(antigo)
            if (idPerfil.value == antigo.id) escolherPerfil(0)
            enviarParaNuvem()
        }
    }

    fun atualizar() {
        if (!temRede.value || aAtualizar.value || botaoBloqueado.value) return
        viewModelScope.launch { atualizarDados(forcar = true) }
    }

    private suspend fun atualizarDados(forcar: Boolean = false) {
        if (!forcar && repository.dadosRecentes()) {
            if (!avisosCarregados) atualizarAvisos()
            return
        }

        aAtualizar.value = true
        var correuBem = false

        try {
            repository.associarAoIpma()
            repository.atualizarDias(null)
            erroAtualizacao.value = null
            correuBem = true
        } catch (e: IOException) {
            Log.e("BeachDay", "Sem ligação ao IPMA", e)
            erroAtualizacao.value = "Não foi possível ligar ao IPMA."
        } catch (e: HttpException) {
            Log.e("BeachDay", "Erro do IPMA: ${e.code()}", e)
            erroAtualizacao.value = "O IPMA respondeu com um erro (${e.code()})."
        } catch (e: JsonParseException) {
            Log.e("BeachDay", "Formato inesperado do IPMA", e)
            erroAtualizacao.value = "O IPMA enviou dados num formato inesperado."
        } finally {
            aAtualizar.value = false
        }

        atualizarAvisos()

        if (forcar && correuBem) {
            botaoBloqueado.value = true
            delay(30.seconds)
            botaoBloqueado.value = false
        }
    }

    private suspend fun atualizarAvisos() {
        try {
            avisos.value = repository.obterAvisos()
            avisosCarregados = true
        } catch (e: IOException) {
            Log.e("BeachDay", "Sem ligação aos avisos do IPMA", e)
        } catch (e: HttpException) {
            Log.e("BeachDay", "Erro nos avisos do IPMA: ${e.code()}", e)
        } catch (e: JsonParseException) {
            Log.e("BeachDay", "Formato inesperado dos avisos do IPMA", e)
        }
    }

    fun escolherRegiao(novaRegiao: String?) {
        regiao.value = novaRegiao
    }

    fun definirFavorita(id: Int, favorita: Boolean) {
        viewModelScope.launch {
            repository.definirFavorita(id, favorita)
            enviarParaNuvem()
        }
    }

    fun registarVisita(idPraia: Int, nota: Int, comentario: String) {
        viewModelScope.launch {
            repository.registarVisita(idPraia, nota, comentario.trim().ifBlank { null })
            enviarParaNuvem()
        }
    }

    fun apagarVisita(id: Int) {
        viewModelScope.launch {
            repository.apagarVisita(id)
            enviarParaNuvem()
        }
    }

    private suspend fun enviarParaNuvem() {
        try {
            sincronizacao.enviar(esperar = false)
        } catch (e: IllegalStateException) {
            Log.e("BeachDay", "Não foi possível enviar para a nuvem", e)
        }
    }

    fun praia(id: Int) = repository.obter(id)

    fun previsao(praia: Praia, perfilAtual: PerfilUtilizador?) =
        repository.previsao(praia, perfilAtual)

    suspend fun carregarPrevisao(praia: Praia): String? {
        if (!temRede.value) return null

        return try {
            repository.atualizarPrevisao(praia)
            null
        } catch (e: IOException) {
            Log.e("BeachDay", "Sem ligação ao IPMA", e)
            "Não foi possível ligar ao IPMA."
        } catch (e: HttpException) {
            Log.e("BeachDay", "Erro do IPMA: ${e.code()}", e)
            "O IPMA respondeu com um erro (${e.code()})."
        } catch (e: JsonParseException) {
            Log.e("BeachDay", "Formato inesperado do IPMA", e)
            "O IPMA enviou dados num formato inesperado."
        }
    }
}
