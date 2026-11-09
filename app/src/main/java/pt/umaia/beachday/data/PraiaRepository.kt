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

class PraiaRepository(
    private val context: Context,
    private val dao: PraiaDao,
    private val api: IpmaApi
) {
    fun pesquisar(pesquisa: String, regiao: String?) =
        dao.pesquisar(pesquisa, regiao)

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

    suspend fun atualizarHoje(regiao: String?) {
        val agora = System.currentTimeMillis()

        dao.locaisTempo(regiao).forEach { id ->
            val dados = api.obterPrevisaoTempo(id).data.mapNotNull {
                val temperatura = it.tMax.toDoubleOrNull()
                val chuva = it.precipitaProb.toDoubleOrNull()
                if (temperatura == null || chuva == null) {
                    null
                } else {
                    CacheTempo(id, it.forecastDate, temperatura, chuva, it.classWindSpeed, agora)
                }
            }
            dao.guardarTempo(dados)
        }

        val resposta = api.obterPrevisaoMar(0)
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

    fun ranking(pesquisa: String, regiao: String?, tipo: String) = run {
        val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        combine(
            dao.pesquisar(pesquisa, regiao),
            dao.tempoDoDia(hoje),
            dao.marDoDia(hoje)
        ) { praias, tempo, mar ->
            val filtradas = praias.filter { praia ->
                when (tipo) {
                    "Costeiras" -> praia.tipo != "fluvial"
                    "Fluviais" -> praia.tipo == "fluvial"
                    else -> true
                }
            }
            ordenarPorNota(filtradas, tempo, mar)
        }
    }

    fun rankingFavoritas() = run {
        val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        combine(
            dao.favoritas(),
            dao.tempoDoDia(hoje),
            dao.marDoDia(hoje)
        ) { praias, tempo, mar ->
            ordenarPorNota(praias, tempo, mar)
        }
    }

    private fun ordenarPorNota(
        praias: List<Praia>,
        tempo: List<CacheTempo>,
        mar: List<CacheMar>
    ): List<PraiaRanking> {
        val tempoPorLocal = tempo.associateBy { it.idLocal }
        val marPorLocal = mar.associateBy { it.idLocal }

        return praias.map { praia ->
            val diaTempo = tempoPorLocal[praia.idLocalTempo]
            val diaMar = marPorLocal[praia.idLocalMar]
            val nota = diaTempo?.let {
                calcularNota(
                    it.temperaturaMaxima,
                    it.probabilidadeChuva,
                    it.classeVento,
                    diaMar?.alturaMaximaMar,
                    diaMar?.temperaturaMaximaAgua
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

    suspend fun obterAvisos(): List<AvisoIpma> {
        val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        return api.obterAvisos().filter { aviso ->
            val nivelAtivo = aviso.awarenessLevelID == "yellow" ||
                    aviso.awarenessLevelID == "orange" ||
                    aviso.awarenessLevelID == "red"
            val inicio = aviso.startTime?.take(10) ?: hoje
            val fim = aviso.endTime?.take(10) ?: hoje

            nivelAtivo && inicio <= hoje && fim >= hoje
        }
    }

    suspend fun atualizarPrevisao(praia: Praia) {
        val agora = System.currentTimeMillis()

        praia.idLocalTempo?.let { id ->
            val dados = api.obterPrevisaoTempo(id).data.mapNotNull {
                val temperatura = it.tMax.toDoubleOrNull()
                val chuva = it.precipitaProb.toDoubleOrNull()
                if (temperatura == null || chuva == null) {
                    null
                } else {
                    CacheTempo(id, it.forecastDate, temperatura, chuva, it.classWindSpeed, agora)
                }
            }
            dao.guardarTempo(dados)
        }

        praia.idLocalMar?.let { id ->
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

    fun previsao(praia: Praia) = run {
        val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        combine(
            dao.obterTempo(praia.idLocalTempo ?: -1, hoje),
            dao.obterMar(praia.idLocalMar ?: -1, hoje)
        ) { tempo, mar ->
            tempo.map { dia ->
                val diaMar = mar.find { it.dia == dia.dia }
                DiaPrevisao(
                    dia = dia.dia,
                    temperaturaMaxima = dia.temperaturaMaxima,
                    probabilidadeChuva = dia.probabilidadeChuva,
                    classeVento = dia.classeVento,
                    alturaMaximaMar = diaMar?.alturaMaximaMar,
                    temperaturaMaximaAgua = diaMar?.temperaturaMaximaAgua,
                    nota = calcularNota(
                        dia.temperaturaMaxima,
                        dia.probabilidadeChuva,
                        dia.classeVento,
                        diaMar?.alturaMaximaMar,
                        diaMar?.temperaturaMaximaAgua
                    )
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