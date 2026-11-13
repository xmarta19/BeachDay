package pt.umaia.beachday.data

import android.content.Context
import android.location.Location
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val VALIDADE_DADOS = 30 * 60 * 1000L

class PraiaRepository(
    private val context: Context,
    private val dao: PraiaDao,
    private val api: IpmaApi
) {
    private fun recente(instante: Long?): Boolean =
        instante != null && System.currentTimeMillis() - instante < VALIDADE_DADOS

    suspend fun dadosRecentes(): Boolean = recente(dao.ultimaAtualizacaoAgora())

    fun obter(id: Int) = dao.obter(id)

    fun ultimaAtualizacao() = dao.ultimaAtualizacao()

    suspend fun importarSeNecessario() {
        if (dao.contar() > 0) return

        val praias = withContext(Dispatchers.IO) {
            context.assets.open("praias.json").bufferedReader().use {
                Gson().fromJson(it, Array<Praia>::class.java).toList()
            }
        }
        dao.inserirTodas(praias)
    }

    suspend fun associarAoIpma() {
        if (dao.contarSemAssociacao() == 0) return

        val locaisTempo = api.obterLocaisTempo().data
        val locaisMar = api.obterLocaisMar()

        val praias = dao.listarTodas().map { praia ->
            val tempo = localMaisProximo(praia, locaisTempo)
            val mar = if (praia.tipo == "fluvial") {
                null
            } else {
                localMaisProximo(praia, locaisMar)
            }

            praia.copy(
                idLocalTempo = tempo?.globalIdLocal,
                nomeLocalTempo = tempo?.local,
                idAreaAviso = tempo?.idAreaAviso,
                idLocalMar = mar?.globalIdLocal,
                nomeLocalMar = mar?.local
            )
        }

        dao.atualizarTodas(praias)
    }

    suspend fun atualizarDias(regiao: String?) {
        dao.locaisTempo(regiao).forEach { id -> guardarTempo(id) }

        val agora = System.currentTimeMillis()
        (0..2).forEach { dia ->
            val resposta = api.obterPrevisaoMar(dia)
            val mar = resposta.data.mapNotNull {
                val altura = it.totalSeaMax.toDoubleOrNull()
                val agua = it.sstMax.toDoubleOrNull()
                if (altura == null || agua == null) {
                    null
                } else {
                    CacheMar(it.globalIdLocal, resposta.forecastDate, altura, agua, agora)
                }
            }
            dao.guardarMar(mar)
        }
    }

    private suspend fun guardarTempo(id: Int) {
        val agora = System.currentTimeMillis()
        val dados = api.obterPrevisaoTempo(id).data.mapNotNull {
            val temperatura = it.tMax.toDoubleOrNull()
            val chuva = it.precipitaProb.toDoubleOrNull()
            if (temperatura == null || chuva == null) {
                null
            } else {
                CacheTempo(
                    idLocal = id,
                    dia = it.forecastDate,
                    temperaturaMaxima = temperatura,
                    probabilidadeChuva = chuva,
                    classeVento = it.classWindSpeed,
                    atualizadoEm = agora,
                    direcaoVento = it.predWindDir
                )
            }
        }
        dao.guardarTempo(dados)
    }

    fun ranking(
        pesquisa: String,
        regiao: String?,
        tipo: String,
        dia: String,
        perfil: PerfilUtilizador?
    ) = combine(
        dao.pesquisar(pesquisa, regiao),
        dao.tempoDoDia(dia),
        dao.marDoDia(dia)
    ) { praias, tempo, mar ->
        val filtradas = praias.filter { praia ->
            val tipoCerto = when (tipo) {
                "Costeiras" -> praia.tipo != "fluvial"
                "Fluviais" -> praia.tipo == "fluvial"
                else -> true
            }
            val perfilCerto = !perfilPrecisaDeMar(perfil) || praia.tipo != "fluvial"
            tipoCerto && perfilCerto
        }
        ordenarPorNota(filtradas, tempo, mar, perfil)
    }

    fun rankingFavoritas(dia: String, perfil: PerfilUtilizador?) = combine(
        dao.favoritas(),
        dao.tempoDoDia(dia),
        dao.marDoDia(dia)
    ) { praias, tempo, mar ->
        ordenarPorNota(praias, tempo, mar, perfil)
    }

    private fun ordenarPorNota(
        praias: List<Praia>,
        tempo: List<CacheTempo>,
        mar: List<CacheMar>,
        perfil: PerfilUtilizador?
    ): List<PraiaRanking> {
        val tempoPorLocal = tempo.associateBy { it.idLocal }
        val marPorLocal = mar.associateBy { it.idLocal }

        return praias.map { praia ->
            val diaTempo = tempoPorLocal[praia.idLocalTempo]
            val diaMar = marPorLocal[praia.idLocalMar]
            val nota = diaTempo?.let {
                calcularNota(
                    perfil = perfil,
                    praia = praia,
                    temperaturaMaxima = it.temperaturaMaxima,
                    probabilidadeChuva = it.probabilidadeChuva,
                    classeVento = it.classeVento,
                    alturaMaximaMar = diaMar?.alturaMaximaMar,
                    temperaturaMaximaAgua = diaMar?.temperaturaMaximaAgua
                )
            }
            PraiaRanking(praia, nota)
        }.sortedWith(
            compareByDescending<PraiaRanking> { it.nota ?: -1 }
                .thenByDescending { it.praia.favorita }
                .thenBy { it.praia.nome }
        )
    }

    suspend fun definirFavorita(id: Int, favorita: Boolean) =
        dao.definirFavorita(id, favorita)

    suspend fun registarVisita(idPraia: Int, nota: Int, comentario: String?) =
        dao.inserirVisita(
            Visita(
                idPraia = idPraia,
                data = System.currentTimeMillis(),
                nota = nota,
                comentario = comentario
            )
        )

    suspend fun apagarVisita(id: Int) = dao.apagarVisita(id)

    fun visitas() = dao.visitas()

    suspend fun obterAvisos(): List<AvisoIpma> =
        api.obterAvisos().filter { aviso ->
            aviso.awarenessLevelID == "yellow" ||
                aviso.awarenessLevelID == "orange" ||
                aviso.awarenessLevelID == "red"
        }

    suspend fun atualizarPrevisao(praia: Praia) {
        praia.idLocalTempo?.let { id ->
            if (!recente(dao.atualizacaoTempo(id))) guardarTempo(id)
        }

        praia.idLocalMar?.let { id ->
            if (recente(dao.atualizacaoMar(id))) return
            val agora = System.currentTimeMillis()
            val dados = (0..2).mapNotNull { dia ->
                val resposta = api.obterPrevisaoMar(dia)
                val mar = resposta.data.find { it.globalIdLocal == id }
                val altura = mar?.totalSeaMax?.toDoubleOrNull()
                val agua = mar?.sstMax?.toDoubleOrNull()
                if (altura == null || agua == null) {
                    null
                } else {
                    CacheMar(id, resposta.forecastDate, altura, agua, agora)
                }
            }
            dao.guardarMar(dados)
        }
    }

    fun previsao(praia: Praia, perfil: PerfilUtilizador?) = run {
        val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val orientacao = orientacaoPraia(praia)

        combine(
            dao.obterTempo(praia.idLocalTempo ?: -1, hoje),
            dao.obterMar(praia.idLocalMar ?: -1, hoje)
        ) { tempo, mar ->
            tempo.map { dia ->
                val diaMar = mar.find { it.dia == dia.dia }
                val avaliacao = avaliar(
                    perfil = perfil,
                    praia = praia,
                    temperaturaMaxima = dia.temperaturaMaxima,
                    probabilidadeChuva = dia.probabilidadeChuva,
                    classeVento = dia.classeVento,
                    alturaMaximaMar = diaMar?.alturaMaximaMar,
                    temperaturaMaximaAgua = diaMar?.temperaturaMaximaAgua
                )
                DiaPrevisao(
                    dia = dia.dia,
                    temperaturaMaxima = dia.temperaturaMaxima,
                    probabilidadeChuva = dia.probabilidadeChuva,
                    classeVento = dia.classeVento,
                    direcaoVento = dia.direcaoVento,
                    relacaoVento = relacaoVento(dia.direcaoVento, orientacao),
                    alturaMaximaMar = diaMar?.alturaMaximaMar,
                    temperaturaMaximaAgua = diaMar?.temperaturaMaximaAgua,
                    nota = avaliacao?.nota,
                    penalizacoes = avaliacao?.penalizacoes ?: emptyList()
                )
            }
        }
    }

    private fun localMaisProximo(
        praia: Praia,
        locais: List<LocalIpma>
    ): LocalIpma? =
        locais.minByOrNull { local ->
            val distancia = FloatArray(1)
            Location.distanceBetween(
                praia.latitude,
                praia.longitude,
                local.latitude.toDouble(),
                local.longitude.toDouble(),
                distancia
            )
            distancia[0]
        }
}
