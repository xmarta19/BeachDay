package pt.umaia.beachday.ui

import android.app.Application
import android.content.Context
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
import pt.umaia.beachday.data.Perfil
import pt.umaia.beachday.data.Praia
import pt.umaia.beachday.data.PraiaRepository
import pt.umaia.beachday.data.Regras
import pt.umaia.beachday.data.estadoRede
import pt.umaia.beachday.data.marcarAvisos
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class Pedido(
    val pesquisa: String,
    val regiao: String?,
    val tipo: String,
    val dia: String,
    val perfil: Perfil,
    val regras: Regras
)

class PraiasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PraiaRepository(
        application,
        AppDatabase.obter(application).praiaDao(),
        IpmaApi.criar()
    )

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
    val perfil = MutableStateFlow(lerPerfil())
    val regras = MutableStateFlow(lerRegras())
    val avisos = MutableStateFlow<List<AvisoIpma>>(emptyList())
    val aAtualizar = MutableStateFlow(false)

    val temRede = estadoRede(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val ultimaAtualizacao = repository.ultimaAtualizacao()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val praias = combine(
        combine(pesquisa, regiao, tipo) { texto, zona, filtro -> Triple(texto, zona, filtro) },
        dia,
        perfil,
        regras
    ) { (texto, zona, filtro), diaEscolhido, perfilAtual, regrasAtuais ->
        Pedido(texto, zona, filtro, diaEscolhido, perfilAtual, regrasAtuais)
    }.flatMapLatest { pedido ->
        combine(
            repository.ranking(
                pedido.pesquisa, pedido.regiao, pedido.tipo,
                pedido.dia, pedido.perfil, pedido.regras
            ),
            avisos
        ) { lista, avisosAtuais -> marcarAvisos(lista, avisosAtuais, pedido.dia) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val favoritas = combine(dia, perfil, regras) { diaEscolhido, perfilAtual, regrasAtuais ->
        Triple(diaEscolhido, perfilAtual, regrasAtuais)
    }.flatMapLatest { (diaEscolhido, perfilAtual, regrasAtuais) ->
        combine(
            repository.rankingFavoritas(diaEscolhido, perfilAtual, regrasAtuais),
            avisos
        ) { lista, avisosAtuais -> marcarAvisos(lista, avisosAtuais, diaEscolhido) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun escolherPerfil(novo: Perfil) {
        perfil.value = novo
        preferencias.edit().putString("perfil", novo.name).apply()
    }

    fun alterarRegras(novas: Regras) {
        regras.value = novas
        preferencias.edit()
            .putFloat("temperaturaMinima", novas.temperaturaMinima.toFloat())
            .putFloat("chuvaMaxima", novas.chuvaMaxima.toFloat())
            .putInt("ventoMaximo", novas.ventoMaximo)
            .putFloat("marMaximo", novas.marMaximo.toFloat())
            .putFloat("aguaMinima", novas.aguaMinima.toFloat())
            .apply()
    }

    private fun lerPerfil(): Perfil {
        val guardado = preferencias.getString("perfil", null)
        return Perfil.entries.firstOrNull { it.name == guardado } ?: Perfil.APANHAR_SOL
    }

    private fun lerRegras(): Regras {
        val base = Regras()
        return Regras(
            temperaturaMinima = preferencias.getFloat(
                "temperaturaMinima", base.temperaturaMinima.toFloat()
            ).toDouble(),
            chuvaMaxima = preferencias.getFloat(
                "chuvaMaxima", base.chuvaMaxima.toFloat()
            ).toDouble(),
            ventoMaximo = preferencias.getInt("ventoMaximo", base.ventoMaximo),
            marMaximo = preferencias.getFloat(
                "marMaximo", base.marMaximo.toFloat()
            ).toDouble(),
            aguaMinima = preferencias.getFloat(
                "aguaMinima", base.aguaMinima.toFloat()
            ).toDouble()
        )
    }

    fun atualizar() {
        if (!temRede.value || aAtualizar.value) return
        viewModelScope.launch { atualizarDados() }
    }

    private suspend fun atualizarDados() {
        aAtualizar.value = true

        try {
            repository.associarAoIpma()
            repository.atualizarDias(regiao.value)
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
                repository.atualizarDias(novaRegiao)
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

    fun previsao(praia: Praia, perfilAtual: Perfil, regrasAtuais: Regras) =
        repository.previsao(praia, perfilAtual, regrasAtuais)

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
