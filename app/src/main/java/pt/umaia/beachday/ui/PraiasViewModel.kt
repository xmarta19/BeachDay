package pt.umaia.beachday.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.umaia.beachday.data.AppDatabase
import pt.umaia.beachday.data.AvisoIpma
import pt.umaia.beachday.data.IpmaApi
import pt.umaia.beachday.data.Praia
import pt.umaia.beachday.data.PraiaRepository
import pt.umaia.beachday.data.estadoRede
import pt.umaia.beachday.data.marcarAvisos
import retrofit2.HttpException
import java.io.IOException

class PraiasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PraiaRepository(
        application,
        AppDatabase.obter(application).praiaDao(),
        IpmaApi.criar()
    )

    val pesquisa = MutableStateFlow("")
    val regiao = MutableStateFlow<String?>(null)
    val tipo = MutableStateFlow("Costeiras")
    val avisos = MutableStateFlow<List<AvisoIpma>>(emptyList())
    val aAtualizar = MutableStateFlow(false)

    val temRede = estadoRede(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val ultimaAtualizacao = repository.ultimaAtualizacao()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val praias = combine(
        combine(pesquisa, regiao, tipo) { texto, zona, filtro ->
            Triple(texto, zona, filtro)
        }.flatMapLatest { (texto, zona, filtro) ->
            repository.ranking(texto, zona, filtro)
        },
        avisos
    ) { lista, avisosAtuais -> marcarAvisos(lista, avisosAtuais) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoritas = combine(repository.rankingFavoritas(), avisos) { lista, avisosAtuais ->
        marcarAvisos(lista, avisosAtuais)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visitas = repository.visitas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.importarSeNecessario()
            atualizarDados()
        }
    }

    fun atualizar() {
        if (!temRede.value || aAtualizar.value) return
        viewModelScope.launch { atualizarDados() }
    }

    private suspend fun atualizarDados() {
        aAtualizar.value = true

        try {
            repository.associarAoIpma()
            repository.atualizarHoje(regiao.value)
        } catch (e: IOException) {
            Log.e("BeachDay", "Sem ligação ao IPMA", e)
        } catch (e: HttpException) {
            Log.e("BeachDay", "Erro do IPMA: ${e.code()}", e)
        } finally {
            aAtualizar.value = false
        }

        atualizarAvisos()
    }

    private suspend fun atualizarAvisos() {
        try {
            avisos.value = repository.obterAvisos()
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
        if (!temRede.value) return

        viewModelScope.launch {
            try {
                repository.atualizarHoje(novaRegiao)
            } catch (e: IOException) {
                Log.e("BeachDay", "Sem ligação ao IPMA", e)
            } catch (e: HttpException) {
                Log.e("BeachDay", "Erro do IPMA: ${e.code()}", e)
            }
        }
    }

    fun definirFavorita(id: Int, favorita: Boolean) {
        viewModelScope.launch { repository.definirFavorita(id, favorita) }
    }

    fun registarVisita(idPraia: Int, nota: Int, comentario: String) {
        viewModelScope.launch {
            repository.registarVisita(idPraia, nota, comentario.trim().ifBlank { null })
        }
    }

    fun apagarVisita(id: Int) {
        viewModelScope.launch { repository.apagarVisita(id) }
    }

    fun praia(id: Int) = repository.obter(id)

    fun previsao(praia: Praia) = repository.previsao(praia)

    fun atualizarPrevisao(praia: Praia) {
        if (!temRede.value) return

        viewModelScope.launch {
            try {
                repository.atualizarPrevisao(praia)
            } catch (e: IOException) {
                Log.e("BeachDay", "Sem ligação ao IPMA", e)
            } catch (e: HttpException) {
                Log.e("BeachDay", "Erro do IPMA: ${e.code()}", e)
            }
        }
    }
}